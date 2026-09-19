import SwiftUI

struct LeaderboardView: View {
    @ObservedObject var authManager = AuthManager.shared
    @Environment(\.dismiss) var dismiss
    private var s: I18nStrings { currentStrings() }

    var body: some View {
        NavigationView {
            ZStack {
                Color.black.opacity(0.9).edgesIgnoringSafeArea(.all)
                
                if authManager.leaderboard.isEmpty {
                    VStack {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.5)
                        Text(s.loadingLeaderboard)
                            .foregroundColor(.gray)
                            .padding(.top)
                    }
                } else {
                    List {
                        ForEach(Array(authManager.leaderboard.enumerated()), id: \.element.id) { index, entry in
                            HStack(spacing: 12) {
                                Text("\(index + 1)")
                                    .font(.headline)
                                    .foregroundColor(.gray)
                                    .frame(width: 30, alignment: .leading)
                                
                                if let photoUrlString = entry.photoUrl, let photoUrl = URL(string: photoUrlString), entry.isGuest != true {
                                    AsyncImage(url: photoUrl) { phase in
                                        if let image = phase.image {
                                            image.resizable().scaledToFill().frame(width: 40, height: 40).clipShape(Circle())
                                        } else {
                                            Image(systemName: "person.circle.fill").resizable().frame(width: 40, height: 40).foregroundColor(.gray)
                                        }
                                    }
                                } else {
                                    Image(systemName: "person.circle.fill").resizable().frame(width: 40, height: 40).foregroundColor(.gray)
                                }
                                
                                VStack(alignment: .leading) {
                                    Text(displayName(for: entry))
                                        .font(.headline)
                                        .foregroundColor(.white)
                                    Text("\(entry.gamesPlayed) \(s.gamesPlayed)")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                                
                                VStack(alignment: .trailing) {
                                    Text("\(entry.wins)")
                                        .font(.title3.bold())
                                        .foregroundColor(.green)
                                    Text(s.winsLabel)
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                            }
                            .listRowBackground(Color.white.opacity(0.05))
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("🏆 \(s.leaderboard)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(s.close) {
                        dismiss()
                    }
                }
            }
            .onAppear {
                authManager.fetchLeaderboard()
            }
        }
        .preferredColorScheme(.dark)
    }
    
    private func displayName(for entry: LeaderboardEntry) -> String {
        let isGuest = entry.isGuest ?? (entry.photoUrl == nil)
        if isGuest && !entry.name.contains("👤") {
            return "\(entry.name) 👤"
        }
        return entry.name
    }
}
