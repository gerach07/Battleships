import SwiftUI

struct ProfileView: View {
    @ObservedObject var authManager = AuthManager.shared
    @Environment(\.presentationMode) var presentationMode
    
    @State private var editName: String = ""
    @State private var editPlayerId: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Profile Info")) {
                    if let url = authManager.profilePicUrl {
                        AsyncImage(url: url) { phase in
                            if let image = phase.image {
                                image.resizable().scaledToFit().frame(width: 80, height: 80).clipShape(Circle())
                            } else if phase.error != nil {
                                Image(systemName: "person.circle.fill").resizable().frame(width: 80, height: 80).foregroundColor(.gray)
                            } else {
                                ProgressView()
                            }
                        }
                    }
                    Text("Wins: \(authManager.wins)")
                }
                
                Section(header: Text("Edit Details")) {
                    TextField("Name", text: $editName)
                    TextField("Player ID", text: $editPlayerId)
                    
                    Button("Save") {
                        authManager.updateProfile(name: editName, playerId: editPlayerId)
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(editName.isEmpty || editPlayerId.isEmpty)
                }
                
                Section {
                    Button("Sign Out") {
                        authManager.signOut()
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationTitle("Profile")
            .navigationBarItems(trailing: Button("Close") {
                presentationMode.wrappedValue.dismiss()
            })
            .onAppear {
                editName = authManager.userName
                editPlayerId = authManager.playerId ?? ""
            }
        }
    }
}
