import SwiftUI

struct WaitingRoomScreen: View {
    @ObservedObject var vm: GameViewModel
    @State private var copied: String?

    private var s: I18nStrings { vm.s }
    private var hasOpponent: Bool { !vm.opponentName.isEmpty }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Room code card
                roomCodeCard

                // Player slots
                playerSlots

                // Host controls
                if vm.isHost && hasOpponent {
                    hostControls
                }

                // Waiting for host (non-host)
                if !vm.isHost && hasOpponent {
                    waitingForHostCard
                }

                // Share section (when alone)
                if !hasOpponent {
                    shareSection
                }

                // Leave button
                Button {
                    vm.handleBackToMenu()
                } label: {
                    Text("← \(s.leaveRoom)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.red.opacity(0.8))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 40)
        }
        .alert(s.confirmKick, isPresented: $vm.showKickDialog) {
            Button(s.yes, role: .destructive) { vm.confirmKick() }
            Button(s.no, role: .cancel) { vm.cancelKick() }
        }
    }

    // MARK: - Room Code Card
    private var roomCodeCard: some View {
        VStack(spacing: 12) {
            Text(s.roomCodeLabel.uppercased())
                .font(.caption2.bold())
                .foregroundColor(.yellow.opacity(0.8))
                .tracking(3)

            Text(vm.gameId)
                .font(.system(size: 48, weight: .black, design: .monospaced))
                .foregroundColor(.yellow)
                .shadow(color: .yellow.opacity(0.3), radius: 16)

            HStack(spacing: 10) {
                if !vm.roomPassword.isEmpty {
                    HStack(spacing: 4) {
                        Text("🔒")
                        Text("PIN: \(vm.roomPassword)")
                            .font(.system(.caption, design: .monospaced).bold())
                    }
                    .foregroundColor(.orange)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.orange.opacity(0.15))
                    .cornerRadius(20)
                }

                HStack(spacing: 4) {
                    Text("⏱️")
                    Text("\(vm.gameTimeLimit / 60) \(s.min)")
                        .font(.caption.bold())
                }
                .foregroundColor(.blue)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.blue.opacity(0.15))
                .cornerRadius(20)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.yellow.opacity(0.3), lineWidth: 1))
        )
    }

    // MARK: - Player Slots
    private var playerSlots: some View {
        HStack(spacing: 16) {
            // You
            playerSlot(
                name: s.youSlot,
                isReady: true,
                isHost: vm.isHost,
                isJoined: true
            )

            // Opponent
            playerSlot(
                name: hasOpponent ? vm.opponentName : s.waitingFor,
                isReady: hasOpponent,
                isHost: false,
                isJoined: hasOpponent
            )
        }
    }

    private func playerSlot(name: String, isReady: Bool, isHost: Bool, isJoined: Bool) -> some View {
        VStack(spacing: 10) {
            ZStack(alignment: .topTrailing) {
                Circle()
                    .fill(isJoined
                        ? LinearGradient(colors: [.green.opacity(0.2), .green.opacity(0.3)], startPoint: .topLeading, endPoint: .bottomTrailing)
                        : LinearGradient(colors: [.blue.opacity(0.15), .blue.opacity(0.2)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 48, height: 48)
                    .overlay(
                        Circle().stroke(isJoined ? Color.green.opacity(0.4) : Color.blue.opacity(0.3), lineWidth: 1)
                    )
                    .overlay(
                        Group {
                            if isJoined {
                                Text("✓").font(.title3)
                            } else {
                                HStack(spacing: 3) {
                                    ForEach(0..<3) { i in
                                        Circle().fill(Color.blue).frame(width: 4, height: 4)
                                    }
                                }
                            }
                        }
                    )

                if isHost {
                    Text("👑")
                        .font(.caption2)
                        .padding(4)
                        .background(LinearGradient(colors: [.yellow, .orange], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .cornerRadius(8)
                        .offset(x: 6, y: -6)
                }
            }

            Text(name)
                .font(.subheadline.bold())
                .foregroundColor(isJoined ? .green : .gray)
                .lineLimit(1)

            Text(isJoined ? s.readyStatus : s.notJoined)
                .font(.caption2.bold())
                .foregroundColor(isJoined ? .green.opacity(0.7) : .gray.opacity(0.5))
                .textCase(.uppercase)
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(
                    isJoined ? Color.green.opacity(0.2) : Color.white.opacity(0.08), lineWidth: 1))
        )
    }

    // MARK: - Host Controls
    private var hostControls: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                Text("👑").font(.body)
                Text(s.hostControls).font(.subheadline.bold()).foregroundColor(.yellow)
            }

            HStack(spacing: 12) {
                Button {
                    vm.handleStartGame()
                } label: {
                    Text("🎮 \(s.startGame)")
                        .font(.subheadline.bold())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(LinearGradient(colors: [.green, .green.opacity(0.8)], startPoint: .leading, endPoint: .trailing))
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }

                Button {
                    vm.requestKickPlayer()
                } label: {
                    Text("⛔")
                        .font(.body)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(Color.red)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
            }

            Text(s.hostHint)
                .font(.caption2)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.yellow.opacity(0.2), lineWidth: 1))
        )
    }

    // MARK: - Waiting for Host
    private var waitingForHostCard: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                Text("⏳").font(.body)
                Text(s.waitingForHost).font(.subheadline.bold()).foregroundColor(.blue)
            }
            Text(s.waitingForHostHint)
                .font(.caption2)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.blue.opacity(0.2), lineWidth: 1))
        )
    }

    // MARK: - Share Section
    private var shareSection: some View {
        VStack(spacing: 16) {
            Text(s.shareRoom).font(.headline).foregroundColor(.white)

            HStack(spacing: 12) {
                Button {
                    UIPasteboard.general.string = vm.gameId
                    showCopied("code")
                } label: {
                    HStack(spacing: 6) {
                        Text(copied == "code" ? "✓" : "📋")
                        Text(copied == "code" ? s.roomCodeCopied : s.copyCode)
                            .font(.subheadline.weight(.semibold))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(copied == "code" ? Color.green.opacity(0.3) : Color.white.opacity(0.06))
                    )
                    .foregroundColor(copied == "code" ? .green : .white)
                }

                Button {
                    let url = vm.roomPassword.isEmpty ? vm.gameId : "\(vm.gameId)/\(vm.roomPassword)"
                    UIPasteboard.general.string = url
                    showCopied("link")
                } label: {
                    HStack(spacing: 6) {
                        Text(copied == "link" ? "✓" : "🔗")
                        Text(copied == "link" ? s.roomCodeCopied : s.shareRoom)
                            .font(.subheadline.weight(.semibold))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(LinearGradient(colors: copied == "link" ? [.green.opacity(0.3)] : [.blue, .indigo],
                                                 startPoint: .leading, endPoint: .trailing))
                    )
                    .foregroundColor(.white)
                }
            }
        }
    }

    private func showCopied(_ label: String) {
        copied = label
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            if copied == label { copied = nil }
        }
    }
}
