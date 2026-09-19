import SwiftUI

struct ProfileView: View {
    @ObservedObject var authManager = AuthManager.shared
    @Environment(\.presentationMode) var presentationMode
    private var s: I18nStrings { currentStrings() }
    
    @State private var editName: String = ""
    @State private var isSaving = false

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(s.profileInfo)) {
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
                    Text("\(s.winsLabel): \(authManager.wins)")
                }

                Section(header: Text(s.editDetails)) {
                    TextField(s.displayName, text: $editName)

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
                            Text(s.save)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(isSaving || editName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                
                Section {
                    Button(s.signOut) {
                        authManager.signOut()
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationTitle(s.profile)
            .navigationBarItems(trailing: Button(s.close) {
                presentationMode.wrappedValue.dismiss()
            })
            .onAppear {
                editName = authManager.userName
            }
        }
    }
}
