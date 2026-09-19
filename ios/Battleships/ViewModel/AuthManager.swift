import Foundation
import FirebaseAuth
import GoogleSignIn
import FirebaseCore
import SwiftUI

class AuthManager: ObservableObject {
    static let shared = AuthManager()

    @Published var isSignedIn: Bool = false
    @Published var userName: String = ""
    @Published var userEmail: String = ""
    @Published var profilePicUrl: URL? = nil
    @Published var idToken: String? = nil
    @Published var playerId: String? = nil
    @Published var wins: Int = 0

    init() {
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            if FirebaseApp.app() == nil {
                FirebaseApp.configure()
            }
            if let clientID = FirebaseApp.app()?.options.clientID {
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            }
        }
        checkLoginState()
    }

    func checkLoginState() {
        if let user = GIDSignIn.sharedInstance.currentUser {
            updateState(with: user)
        } else {
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                if let user = user {
                    self.updateState(with: user)
                }
            }
        }
    }

    private func updateState(with user: GIDGoogleUser) {
        DispatchQueue.main.async {
            self.isSignedIn = true
            self.userName = user.profile?.name ?? ""
            self.userEmail = user.profile?.email ?? ""
            self.profilePicUrl = user.profile?.imageURL(withDimension: 100)
            self.idToken = user.idToken?.tokenString
            self.fetchProfile()
        }
    }

    func signIn(presenting: UIViewController) {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            print("GoogleService-Info.plist not found, cannot sign in.")
            return
        }

        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        } else {
            print("Google Sign-In client ID missing; Firebase config may not be loaded.")
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presenting) { result, error in
            if let error = error {
                print("Google sign in failed: \(error.localizedDescription)")
                return
            }
            if let user = result?.user {
                self.updateState(with: user)
            } else {
                print("Google sign in returned no user result")
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        DispatchQueue.main.async {
            self.isSignedIn = false
            self.userName = ""
            self.userEmail = ""
            self.profilePicUrl = nil
            self.idToken = nil
            self.playerId = nil
            self.wins = 0
        }
    }

    private func ensureUserProfileExists() {
        guard let token = idToken else { return }

        var request = URLRequest(url: URL(string: "\(SERVER_URL)/api/auth/login")!)
        request.httpMethod = "POST"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        URLSession.shared.dataTask(with: request) { data, response, error in
            guard error == nil else {
                print("Login bootstrap failed: \(error!.localizedDescription)")
                return
            }
            guard let http = response as? HTTPURLResponse else { return }
            if http.statusCode == 200 || http.statusCode == 201 {
                self.fetchProfile()
            }
        }.resume()
    }

    func fetchProfile() {
        guard let token = idToken else { return }
        var request = URLRequest(url: URL(string: "\(SERVER_URL)/api/profile")!)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else { return }

            if let http = response as? HTTPURLResponse, (http.statusCode == 401 || http.statusCode == 403 || http.statusCode == 404) {
                self.ensureUserProfileExists()
                return
            }

            do {
                if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    DispatchQueue.main.async {
                        self.playerId = json["playerId"] as? String
                        self.wins = json["wins"] as? Int ?? 0
                        if let name = json["name"] as? String, !name.isEmpty {
                            self.userName = name
                        }
                    }
                }
            } catch {
                print("Failed to parse profile JSON")
            }
        }.resume()
    }

    func updateProfile(name: String, completion: ((Bool) -> Void)? = nil) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let token = idToken, !trimmed.isEmpty else {
            completion?(false)
            return
        }

        var request = URLRequest(url: URL(string: "\(SERVER_URL)/api/profile")!)
        request.httpMethod = "PUT"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["name": trimmed])

        URLSession.shared.dataTask(with: request) { data, response, error in
            guard error == nil, let httpResponse = response as? HTTPURLResponse else {
                DispatchQueue.main.async { completion?(false) }
                return
            }

            if httpResponse.statusCode >= 200 && httpResponse.statusCode < 300 {
                DispatchQueue.main.async {
                    self.userName = trimmed
                    completion?(true)
                    self.fetchProfile()
                }
            } else {
                DispatchQueue.main.async { completion?(false) }
            }
        }.resume()
    }
}
