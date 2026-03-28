import SwiftUI

struct ContentView: View {
    @StateObject private var vm = GameViewModel()
    @ObservedObject private var mm = MusicManager.shared
    @State private var showCredits = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: [
                    Color(red: 0.03, green: 0.05, blue: 0.12),
                    Color(red: 0.06, green: 0.10, blue: 0.22),
                    Color(red: 0.03, green: 0.07, blue: 0.15)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            // Background ships (only on non-battle screens)
            if vm.phase != "battle" && vm.phase != "placement" {
                BackgroundShipsView()
            }

            // Main content
            VStack(spacing: 0) {
                // Header bar
                headerBar

                ZStack(alignment: .top) {
                    // Phase content (permanently pushed down so the music bar doesn't cause shifting)
                    phaseContent
                        .padding(.top, 32)

                    // Now-playing strip (absolutely positioned at the top of the ZStack)
                    if mm.enabled, let trackName = mm.currentTrackName {
                        HStack(spacing: 8) {
                            ZStack {
                                Circle()
                                    .fill(Color.blue.opacity(0.18))
                                    .frame(width: 20, height: 20)
                                Image(systemName: "music.note")
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundColor(.blue.opacity(0.9))
                            }
                            MarqueeText(trackName, font: .system(size: 11, weight: .medium), color: .white.opacity(0.75))
                            Spacer()
                            Button { showCredits = true } label: {
                                Image(systemName: "info.circle")
                                    .font(.system(size: 14))
                                    .foregroundColor(.blue.opacity(0.65))
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 5)
                        .background(
                            LinearGradient(
                                colors: [Color.blue.opacity(0.10), Color.indigo.opacity(0.06)],
                                startPoint: .leading, endPoint: .trailing
                            )
                        )
                        .overlay(alignment: .bottom) {
                            Rectangle().fill(Color.blue.opacity(0.12)).frame(height: 0.5)
                        }
                        .transition(.opacity)
                    }
                }
            }

            // Message toast
            if !vm.message.isEmpty {
                messageToast
            }

            // Chat overlay
            if vm.chatOpen && (vm.phase == "battle" || vm.phase == "placement" || vm.phase == "gameOver" || vm.phase == "waiting") {
                ChatOverlay(vm: vm)
                    .frame(maxHeight: 400)
                    .padding(16)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            // Connection overlay
            if !vm.socketManager.isConnected && vm.phase != "login" {
                ConnectionOverlay(strings: vm.s) {
                    vm.forceReconnect()
                }
                .transition(.opacity)
            }
        }
        .preferredColorScheme(.dark)
        .animation(.easeInOut(duration: 0.3), value: vm.phase)
        .animation(.easeInOut(duration: 0.2), value: vm.chatOpen)
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .active:
                vm.handleSceneActive()
            case .background, .inactive:
                vm.handleSceneBackground()
            @unknown default:
                break
            }
        }
        .sheet(isPresented: $showCredits) {
            CreditsSheet()
        }
    }

    // MARK: - Header
    private var headerBar: some View {
        HStack {
            // Language picker
            Menu {
                ForEach(Language.allCases, id: \.self) { lang in
                    Button {
                        vm.language = lang
                    } label: {
                        Text("\(lang.flag) \(lang.displayName)")
                    }
                }
            } label: {
                Text(vm.language.flag)
                    .font(.title3)
                    .padding(6)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(8)
            }

            Spacer()

            // Title
            if vm.phase != "login" {
                Text("⚓")
                    .font(.title2)
                Text(vm.s.battleships)
                    .font(.subheadline.bold())
                    .foregroundColor(.white.opacity(0.8))
            }

            Spacer()

            // Sound toggle
            Button {
                vm.soundEnabled.toggle()
            } label: {
                Image(systemName: vm.soundEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill")
                    .frame(width: 20, height: 20)
                    .foregroundColor(vm.soundEnabled ? .blue : .gray)
                    .padding(6)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(8)
            }

            // Music toggle
            Button {
                vm.musicEnabled.toggle()
            } label: {
                Image(systemName: vm.musicEnabled ? "music.note" : "music.note.list")
                    .frame(width: 20, height: 20)
                    .foregroundColor(vm.musicEnabled ? .blue : .gray)
                    .padding(6)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(8)
            }

            // Chat button (when in game)
            if vm.phase == "battle" || vm.phase == "placement" || vm.phase == "gameOver" || vm.phase == "waiting" {
                Button {
                    vm.toggleChat()
                } label: {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: vm.chatOpen ? "bubble.fill" : "bubble")
                            .foregroundColor(.blue)
                            .padding(6)
                            .background(Color.white.opacity(0.08))
                            .cornerRadius(8)

                        if vm.chatUnread > 0 {
                            Text("\(vm.chatUnread)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.white)
                                .padding(3)
                                .background(Circle().fill(Color.red))
                                .offset(x: 4, y: -4)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    // MARK: - Phase Content
    @ViewBuilder
    private var phaseContent: some View {
        switch vm.phase {
        case "login":
            LoginScreen(vm: vm)
                .transition(.opacity)
        case "waiting":
            WaitingRoomScreen(vm: vm)
                .transition(.opacity)
        case "placement":
            PlacementScreen(vm: vm)
                .transition(.opacity)
        case "battle":
            BattleScreen(vm: vm)
                .transition(.opacity)
        case "gameOver":
            GameOverScreen(vm: vm)
                .transition(.opacity)
        default:
            LoginScreen(vm: vm)
        }
    }

    // MARK: - Message Toast
    private var messageToast: some View {
        VStack {
            Spacer()
            Text(vm.message)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(messageColor)
                )
                .shadow(color: .black.opacity(0.3), radius: 8)
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: vm.message)
        .allowsHitTesting(false)
    }

    private var messageColor: Color {
        switch vm.messageType {
        case "error": return Color.red.opacity(0.85)
        case "success": return Color.green.opacity(0.75)
        default: return Color.blue.opacity(0.75)
        }
    }
}
