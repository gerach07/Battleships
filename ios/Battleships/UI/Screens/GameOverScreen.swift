import SwiftUI

struct GameOverScreen: View {
    @ObservedObject var vm: GameViewModel

    private var s: I18nStrings { vm.s }
    private var isWinner: Bool { vm.winner == vm.playerIdRef }
    private var spectatorWinnerName: String {
        vm.spectatorBoards.first(where: { $0.playerId == vm.winner })?.playerName ?? "?"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Spacer().frame(height: 20)

                // Main result card
                VStack(spacing: 20) {
                    // Trophy / skull emoji
                    Text(vm.isSpectator ? "🏁" : (isWinner ? "🏆" : "💀"))
                        .font(.system(size: 80))
                        .shadow(color: isWinner && !vm.isSpectator ? .yellow.opacity(0.5) : .clear, radius: 20)

                    // Title
                    Text(vm.isSpectator ? s.gameOver : (isWinner ? s.victory : s.defeat))
                        .font(.system(size: 40, weight: .black))
                        .foregroundStyle(
                            vm.isSpectator
                                ? AnyShapeStyle(.white)
                                : isWinner
                                    ? AnyShapeStyle(LinearGradient(colors: [.yellow, .orange, .yellow], startPoint: .leading, endPoint: .trailing))
                                    : AnyShapeStyle(.red)
                        )

                    // Subtitle
                    Text(vm.isSpectator
                        ? s.winsMessage.fmt(spectatorWinnerName)
                        : isWinner
                            ? s.youSunkEnemy
                            : s.destroyedYourFleet.fmt(vm.opponentName))
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)

                    // Stars for winner
                    if isWinner && !vm.isSpectator {
                        HStack(spacing: 8) {
                            ForEach(0..<3, id: \.self) { _ in
                                Text("⭐").font(.title)
                                    .shadow(color: .yellow.opacity(0.6), radius: 8)
                            }
                        }
                    }

                    // Play again section (non-spectator)
                    if !vm.isSpectator {
                        playAgainSection
                    }

                    // Spectator — just back to menu
                    if vm.isSpectator {
                        Button {
                            vm.handleBackToMenu()
                        } label: {
                            Text(s.mainMenu)
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color.white.opacity(0.1))
                                .foregroundColor(.white)
                                .cornerRadius(16)
                        }
                    }
                }
                .padding(28)
                .background(
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.white.opacity(0.06))
                        .overlay(RoundedRectangle(cornerRadius: 20).stroke(
                            vm.isSpectator
                                ? Color.white.opacity(0.1)
                                : isWinner
                                    ? Color.yellow.opacity(0.3)
                                    : Color.red.opacity(0.2),
                            lineWidth: 1))
                )
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 40)
        }
    }

    // MARK: - Play Again
    @ViewBuilder
    private var playAgainSection: some View {
        if vm.opponentWantsPlayAgain {
            // Opponent wants rematch
            VStack(spacing: 12) {
                Text(s.opWantsRematch)
                    .font(.subheadline.bold())
                    .foregroundColor(.blue)
                    .multilineTextAlignment(.center)

                HStack(spacing: 10) {
                    Button {
                        vm.handlePlayAgain()
                    } label: {
                        Text(s.accept)
                            .font(.subheadline.bold())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(LinearGradient(colors: [.green, .green.opacity(0.8)], startPoint: .leading, endPoint: .trailing))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }

                    Button {
                        vm.handleDeclinePlayAgain()
                    } label: {
                        Text(s.decline)
                            .font(.subheadline.bold())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.red.opacity(0.7))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }

                Button {
                    vm.handleBackToMenu()
                } label: {
                    Text(s.mainMenu)
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.white.opacity(0.08))
                        .foregroundColor(.gray)
                        .cornerRadius(12)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.blue.opacity(0.15))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.blue.opacity(0.3)))
            )
        } else if vm.playAgainPending {
            // Waiting for opponent response
            VStack(spacing: 10) {
                HStack(spacing: 6) {
                    ForEach(0..<3, id: \.self) { _ in
                        Circle().fill(Color.blue).frame(width: 6, height: 6)
                    }
                }
                Text(s.waitingForOpponentRematch)
                    .font(.subheadline)
                    .foregroundColor(.gray)

                Button {
                    vm.handleDeclinePlayAgain()
                } label: {
                    Text(s.cancelRematch)
                        .font(.caption)
                        .foregroundColor(.gray)
                        .underline()
                }

                Button {
                    vm.handleBackToMenu()
                } label: {
                    Text(s.mainMenu)
                        .font(.caption.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.white.opacity(0.08))
                        .foregroundColor(.gray)
                        .cornerRadius(12)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.white.opacity(0.05))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.1)))
            )
        } else {
            // Default — no play again flow yet
            VStack(spacing: 10) {
                Button {
                    vm.handlePlayAgain()
                } label: {
                    Text(s.playAgain)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(LinearGradient(colors: [.blue, .purple], startPoint: .leading, endPoint: .trailing))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }

                Button {
                    vm.handleBackToMenu()
                } label: {
                    Text(s.mainMenu)
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.white.opacity(0.08))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
            }
        }
    }
}
