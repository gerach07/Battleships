import SwiftUI

struct BattleScreen: View {
    @ObservedObject var vm: GameViewModel

    private var s: I18nStrings { vm.s }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Timer
                if !vm.playerTimeLeft.isEmpty {
                    GameTimerView(
                        playerTimeLeft: vm.playerTimeLeft,
                        turnStartedAt: vm.turnStartedAt,
                        currentTurn: vm.currentTurn,
                        myId: vm.playerIdRef,
                        opponentName: vm.opponentName,
                        strings: s,
                        spectatorPlayerNames: vm.isSpectator
                            ? Dictionary(uniqueKeysWithValues: vm.spectatorBoards.map { ($0.playerId, $0.playerName) })
                            : [:]
                    )
                }

                // Turn indicator
                turnIndicator

                // Scoreboard (non-spectator)
                if !vm.isSpectator {
                    scoreBoard
                }

                // Spectator boards or normal boards
                if vm.isSpectator {
                    spectatorBoards
                } else {
                    normalBoards
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .alert(s.confirmSurrender, isPresented: $vm.showSurrenderDialog) {
            Button(s.yes, role: .destructive) { vm.confirmForfeit() }
            Button(s.no, role: .cancel) { vm.cancelForfeit() }
        }
    }

    // MARK: - Turn Indicator
    private var turnIndicator: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    if vm.isSpectator {
                        Text("👁️").foregroundColor(.purple)
                        Text(s.spectating).font(.headline).foregroundColor(.purple)
                        if let turnName = vm.spectatorBoards.first(where: { $0.playerId == vm.currentTurn })?.playerName {
                            Text("— \(s.namesTurn.fmt(turnName))")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    } else if vm.isMyTurn {
                        Text("⚡").foregroundColor(.green)
                        Text(s.yourTurnFire).font(.headline).foregroundColor(.green)
                    } else {
                        Text("⏳").foregroundColor(.gray)
                        Text(s.namesTurn.fmt(vm.opponentName)).font(.headline).foregroundColor(.white)
                    }
                }

                Text(!vm.isSpectator && vm.isMyTurn ? s.extraShotHint : " ")
                    .font(.caption2)
                    .foregroundColor(.green.opacity(vm.isMyTurn ? 0.6 : 0))
            }

            Spacer()

            HStack(spacing: 8) {
                if vm.spectatorCount > 0 {
                    Text("👁️ \(vm.spectatorCount)")
                        .font(.caption2.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Color.purple.opacity(0.2))
                        .cornerRadius(8)
                        .foregroundColor(.purple)
                }

                Button {
                    if vm.isSpectator {
                        vm.handleBackToMenu()
                    } else {
                        vm.requestForfeit()
                    }
                } label: {
                    Text(vm.isSpectator ? s.leave : "🏳️")
                        .font(.caption.bold())
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.red.opacity(0.3)))
                        .foregroundColor(.red.opacity(0.8))
                        .cornerRadius(8)
                }
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(
                    vm.isSpectator
                        ? Color.purple.opacity(0.3)
                        : vm.isMyTurn ? Color.green.opacity(0.3) : Color.white.opacity(0.08),
                    lineWidth: 1))
        )
    }

    // MARK: - Score Board
    private var scoreBoard: some View {
        HStack(spacing: 16) {
            HStack(spacing: 6) {
                Text(s.yourHits).font(.caption.bold()).foregroundColor(.green)
                HStack(spacing: 3) {
                    ForEach(0..<5, id: \.self) { i in
                        Circle()
                            .fill(i < vm.theirSunkCount ? Color.orange : Color.white.opacity(0.1))
                            .frame(width: 8, height: 8)
                    }
                }
            }
            Text("│").foregroundColor(.gray.opacity(0.3))
            HStack(spacing: 6) {
                HStack(spacing: 3) {
                    ForEach(0..<5, id: \.self) { i in
                        Circle()
                            .fill(i < vm.mySunkCount ? Color.red : Color.white.opacity(0.1))
                            .frame(width: 8, height: 8)
                    }
                }
                Text(s.theirHits).font(.caption.bold()).foregroundColor(.red)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.white.opacity(0.04))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.06)))
        )
    }

    // MARK: - Normal Boards
    private var normalBoards: some View {
        VStack(spacing: 14) {
            // Your fleet
            boardCard(label: s.yourFleet, isHighlighted: false) {
                GameBoardView(
                    board: vm.playerBoard,
                    isOpponentBoard: false,
                    isInteractive: false,
                    shotKeys: vm.playerShotKeys,
                    explosionKeys: vm.playerExplosionKeys
                )
            }

            // Enemy waters
            boardCard(label: s.enemyWaters.fmt(vm.opponentName), isHighlighted: vm.isMyTurn) {
                GameBoardView(
                    board: vm.opponentBoard,
                    isOpponentBoard: true,
                    isInteractive: vm.isMyTurn,
                    shotKeys: vm.shotKeys,
                    explosionKeys: vm.explosionKeys
                ) { row, col in
                    vm.shoot(row: row, col: col)
                }
            }
        }
    }

    // MARK: - Spectator Boards
    private var spectatorBoards: some View {
        VStack(spacing: 14) {
            ForEach(vm.spectatorBoards, id: \.playerId) { sb in
                boardCard(label: "🛡️ \(sb.playerName)", isHighlighted: false) {
                    GameBoardView(
                        board: sb.board,
                        isOpponentBoard: false,
                        isInteractive: false,
                        shotKeys: [],
                        explosionKeys: []
                    )
                }
            }
        }
    }

    @ViewBuilder
    private func boardCard<Content: View>(label: String, isHighlighted: Bool, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.bold())
                .foregroundColor(.white)

            content()

            // Legend
            HStack(spacing: 10) {
                legendItem(.blue.opacity(0.7), "Water")
                legendItem(.green.opacity(0.6), "Ship")
                legendItem(.red.opacity(0.8), "Hit")
                legendItem(.gray.opacity(0.5), "Miss")
                legendItem(Color(red: 0.6, green: 0.1, blue: 0.1), "Sunk")
            }
            .font(.system(size: 9))
            .foregroundColor(.gray)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(Color.white.opacity(0.04))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(
                    isHighlighted ? Color.green.opacity(0.2) : Color.white.opacity(0.06), lineWidth: 1))
        )
    }

    private func legendItem(_ color: Color, _ label: String) -> some View {
        HStack(spacing: 3) {
            RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 8, height: 8)
            Text(label)
        }
    }
}
