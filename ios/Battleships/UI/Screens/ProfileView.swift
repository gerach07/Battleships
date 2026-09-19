import SwiftUI

struct ProfileView: View {
    @ObservedObject var authManager = AuthManager.shared
    @Environment(\.presentationMode) var presentationMode
    
    @State private var editName: String = ""
    @State private var isSaving = false

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

                    Button(action: {
                        let trimmed = editName.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty else { return }
                        isSaving = true
                        authManager.updateProfile(name: trimmed) { success in
                            DispatchQueue.main.async {
                                isSaving = false
                                if success {
                                    authManager.userName = trimmed
                                    presentationMode.wrappedValue.dismiss()
                                }
                            }
                        }
                    }) {
                        if isSaving {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                                .frame(maxWidth: .infinity)
                        } else {
                            Text("Save")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(isSaving || editName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
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
            }
        }
    }
}
