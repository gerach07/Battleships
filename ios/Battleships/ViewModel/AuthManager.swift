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

    @Published var wins: Int = 0
    @Published var leaderboard: [LeaderboardEntry] = []

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
            signIntoFirebase(googleUser: user)
        } else {
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                if let user = user {
                    self.signIntoFirebase(googleUser: user)
                }
            }
        }
    }

    /// After Google Sign-In succeeds, authenticate with Firebase Auth to get a
    /// proper Firebase ID token (not a raw Google token). The server requires this.
    private func signIntoFirebase(googleUser: GIDGoogleUser) {
        guard let idTokenString = googleUser.idToken?.tokenString else {
            print("Google idToken missing")
            return
        }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idTokenString,
            accessToken: googleUser.accessToken.tokenString
        )
        Auth.auth().signIn(with: credential) { result, error in
            if let error = error {
                print("Firebase sign-in failed: \(error.localizedDescription)")
                return
            }
            // Refresh the Firebase ID token and update state
            result?.user.getIDToken { token, error in
                guard let token = token, error == nil else {
                    print("Failed to get Firebase ID token: \(String(describing: error))")
                    return
                }
                DispatchQueue.main.async {
                    self.isSignedIn = true
                    self.userName = googleUser.profile?.name ?? ""
                    self.userEmail = googleUser.profile?.email ?? ""
                    self.profilePicUrl = googleUser.profile?.imageURL(withDimension: 100)
                    self.idToken = token
                    self.fetchProfile()
                }
            }
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
                self.signIntoFirebase(googleUser: user)
            } else {
                print("Google sign in returned no user result")
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        try? Auth.auth().signOut()
        DispatchQueue.main.async {
            self.isSignedIn = false
            self.userName = ""
            self.userEmail = ""
            self.profilePicUrl = nil
            self.idToken = nil

            self.wins = 0
        }
    }

    /// Refresh the Firebase ID token before making API calls to avoid expired token errors.
    func refreshTokenThenCall(_ block: @escaping (String) -> Void) {
        guard let firebaseUser = Auth.auth().currentUser else {
            print("No Firebase user signed in")
            return
        }
        firebaseUser.getIDToken { token, error in
            guard let token = token, error == nil else {
                print("Failed to refresh Firebase token: \(String(describing: error))")
                return
            }
            DispatchQueue.main.async {
                self.idToken = token
            }
            block(token)
        }
    }

    private func ensureUserProfileExists() {
        refreshTokenThenCall { token in
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
    }

    func fetchProfile() {
        refreshTokenThenCall { token in
            var request = URLRequest(url: URL(string: "\(SERVER_URL)/api/profile")!)
            request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

            URLSession.shared.dataTask(with: request) { data, response, error in
                guard let data = data, error == nil else { return }

                if let http = response as? HTTPURLResponse,
                   (http.statusCode == 401 || http.statusCode == 403 || http.statusCode == 404) {
                    self.ensureUserProfileExists()
                    return
                }

                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        DispatchQueue.main.async {

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
    }

    func updateProfile(name: String, completion: ((Bool) -> Void)? = nil) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            completion?(false)
            return
        }

        refreshTokenThenCall { token in
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
                    }
                    // Refresh profile in background to sync all fields
                    self.fetchProfile()
                } else {
                    DispatchQueue.main.async { completion?(false) }
                }
            }.resume()
        }
    }

    func fetchLeaderboard() {
        let url = URL(string: "\(SERVER_URL)/api/leaderboard")!
        var request = URLRequest(url: url)

        let performRequest = { (req: URLRequest) in
            URLSession.shared.dataTask(with: req) { data, response, error in
                guard let data = data, error == nil else {
                    print("Failed to fetch leaderboard: \(String(describing: error))")
                    return
                }
                do {
                    let payload = try JSONDecoder().decode(LeaderboardResponse.self, from: data)
                    DispatchQueue.main.async {
                        self.leaderboard = payload.leaderboard
                    }
                } catch {
                    print("Failed to decode leaderboard: \(error)")
                }
            }.resume()
        }

        if Auth.auth().currentUser != nil {
            refreshTokenThenCall { token in
                request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                performRequest(request)
            }
        } else {
            performRequest(request)
        }
    }
}

struct LeaderboardResponse: Codable {
    let leaderboard: [LeaderboardEntry]
}

struct LeaderboardEntry: Codable, Identifiable {
    let rank: Int?
    let name: String
    let wins: Int
    let gamesPlayed: Int
    let photoUrl: String?
    let isGuest: Bool?

    var id: String {
        "\(rank ?? 0)-\(name)-\(wins)-\(gamesPlayed)-\(photoUrl ?? "")"
    }
}
