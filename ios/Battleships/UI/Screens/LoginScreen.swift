import SwiftUI

struct LoginScreen: View {
    @ObservedObject var vm: GameViewModel

    @State private var joinRoomCode = ""
    @State private var joinRoomPin = ""
    @State private var pendingJoin: PendingJoin?
    @State private var selectedRoomId: String?

    private var s: I18nStrings { vm.s }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Hero
                heroSection

                switch vm.loginView {
                case "menu":
                    menuView
                case "create":
                    createView
                case "join":
                    joinView
                case "enterPin":
                    enterPinView
                case "enterName":
                    enterNameView
                default:
                    menuView
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
    }

    // MARK: - Hero
    private var heroSection: some View {
        VStack(spacing: 12) {
            HStack(alignment: .bottom, spacing: 8) {
                Text("🚢").font(.system(size: 36)).opacity(0.2).scaleEffect(x: -1, y: 1)
                Text("⚓").font(.system(size: 72))
                    .shadow(color: .blue.opacity(0.5), radius: 16)
                Text("🚢").font(.system(size: 36)).opacity(0.2)
            }
            Text(s.battleships)
                .font(.system(size: 36, weight: .black))
                .foregroundStyle(
                    LinearGradient(colors: [.cyan, .blue, .purple], startPoint: .leading, endPoint: .trailing)
                )
            HStack(spacing: 8) {
                Rectangle().fill(Color.gray.opacity(0.4)).frame(width: 60, height: 1)
                Text(s.multiplayerNavalCombat)
                    .font(.caption)
                    .foregroundColor(.gray)
                Rectangle().fill(Color.gray.opacity(0.4)).frame(width: 60, height: 1)
            }
            Text("Created by Adrians Bergmanis")
                .font(.system(size: 10))
                .foregroundColor(.gray.opacity(0.55))
        }
    }

    // MARK: - Menu
    private var menuView: some View {
        VStack(spacing: 16) {
            Button {
                vm.loginView = "create"
            } label: {
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(LinearGradient(colors: [.green, .green.opacity(0.8)], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 56, height: 56)
                        .overlay(Text("⚔️").font(.title))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(s.createGame).font(.headline).foregroundColor(.white)
                        Text(s.createNewGame).font(.caption).foregroundColor(.gray)
                    }
                    Spacer()
                    Text("›").font(.title).foregroundColor(.gray)
                }
                .padding(16)
                .background(glassCard)
            }

            Button {
                vm.loginView = "join"
                vm.fetchRooms()
            } label: {
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(LinearGradient(colors: [.blue, .indigo], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 56, height: 56)
                        .overlay(Text("⛵").font(.title))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(s.joinGame).font(.headline).foregroundColor(.white)
                        Text(s.joinGameTitle).font(.caption).foregroundColor(.gray)
                    }
                    Spacer()
                    Text("›").font(.title).foregroundColor(.gray)
                }
                .padding(16)
                .background(glassCard)
            }
        }
        .transition(.opacity)
    }

    // MARK: - Create
    private var createView: some View {
        VStack(spacing: 16) {
            VStack(spacing: 20) {
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.green.opacity(0.15))
                        .frame(width: 36, height: 36)
                        .overlay(Text("⚔️"))
                    Text(s.createNewGame).font(.headline).foregroundColor(.white)
                    Spacer()
                }

                // PIN
                VStack(alignment: .leading, spacing: 6) {
                    Label(s.roomPinOptional, systemImage: "lock")
                        .font(.caption).foregroundColor(.gray)
                    TextField("• • •", text: $vm.createPassword)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.center)
                        .font(.system(size: 24, design: .monospaced))
                        .padding(10)
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(10)
                        .onChange(of: vm.createPassword) { newVal in
                            vm.createPassword = String(newVal.filter(\.isNumber).prefix(3))
                        }
                    Text(s.threedigitPin).font(.caption2).foregroundColor(.gray.opacity(0.6))
                }

                Divider().background(Color.white.opacity(0.1))

                // Time
                VStack(alignment: .leading, spacing: 8) {
                    Label(s.timePerPlayer, systemImage: "timer")
                        .font(.caption).foregroundColor(.gray)
                    HStack {
                        Slider(value: Binding(
                            get: { Double(vm.gameTimeLimit / 60) },
                            set: { vm.gameTimeLimit = Int($0) * 60 }
                        ), in: 2...10, step: 1)
                        .tint(.green)
                        Text("\(vm.gameTimeLimit / 60) \(s.min)")
                            .font(.system(.body, design: .monospaced))
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                            .frame(width: 60)
                            .padding(6)
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(8)
                    }
                }

                // Create button
                Button {
                    if !vm.createPassword.isEmpty && vm.createPassword.count != 3 {
                        vm.setMessage("❌ PIN must be 3 digits", "error")
                        return
                    }
                    let roomId = generateRoomId()
                    pendingJoin = PendingJoin(
                        roomId: roomId,
                        password: vm.createPassword.isEmpty ? nil : vm.createPassword,
                        isCreating: true,
                        timeLimit: vm.gameTimeLimit
                    )
                    vm.loginView = "enterName"
                } label: {
                    Text(s.createRoom)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(LinearGradient(colors: [.green, .green.opacity(0.8)], startPoint: .leading, endPoint: .trailing))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
            }
            .padding(20)
            .background(glassCard)

            backButton { vm.loginView = "menu" }
        }
        .transition(.opacity)
    }

    // MARK: - Join
    private var joinView: some View {
        VStack(spacing: 16) {
            // Direct code entry
            VStack(spacing: 14) {
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.blue.opacity(0.15))
                        .frame(width: 36, height: 36)
                        .overlay(Text("🎯"))
                    Text(s.roomCode).font(.headline).foregroundColor(.white)
                    Spacer()
                }

                TextField(s.roomCodePlaceholder, text: $joinRoomCode)
                    .textInputAutocapitalization(.characters)
                    .multilineTextAlignment(.center)
                    .font(.system(size: 20, design: .monospaced))
                    .padding(12)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(10)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.blue.opacity(0.25), lineWidth: 2))
                    .onChange(of: joinRoomCode) { newVal in
                        joinRoomCode = String(newVal.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(10))
                    }

                HStack(spacing: 8) {
                    Button {
                        guard !joinRoomCode.isEmpty else { return }
                        pendingJoin = PendingJoin(roomId: joinRoomCode)
                        vm.loginView = "enterName"
                    } label: {
                        Text(s.joinRoom)
                            .font(.subheadline.bold())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(LinearGradient(colors: [.blue, .indigo], startPoint: .leading, endPoint: .trailing))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .disabled(joinRoomCode.isEmpty)
                    .opacity(joinRoomCode.isEmpty ? 0.4 : 1)

                    Button {
                        guard !joinRoomCode.isEmpty else { return }
                        pendingJoin = PendingJoin(roomId: joinRoomCode, isSpectating: true)
                        vm.loginView = "enterName"
                    } label: {
                        Text("👁️ \(s.spectate)")
                            .font(.subheadline.bold())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.purple.opacity(0.4)))
                            .foregroundColor(.purple)
                            .cornerRadius(12)
                    }
                    .disabled(joinRoomCode.isEmpty)
                    .opacity(joinRoomCode.isEmpty ? 0.4 : 1)
                }
            }
            .padding(16)
            .background(glassCard)

            // Available rooms
            VStack(spacing: 12) {
                HStack {
                    Label(s.availableRooms, systemImage: "antenna.radiowaves.left.and.right")
                        .font(.caption.bold())
                        .foregroundColor(.gray)
                    Spacer()
                    Button {
                        vm.fetchRooms()
                    } label: {
                        Text("🔄 \(s.refresh)")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                }

                if vm.availableRooms.isEmpty {
                    VStack(spacing: 4) {
                        Text(s.noRoomsAvailable)
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    .padding(.vertical, 20)
                } else {
                    ForEach(vm.availableRooms) { room in
                        roomRow(room)
                    }
                }
            }
            .padding(16)
            .background(glassCard)

            backButton {
                vm.loginView = "menu"
                joinRoomCode = ""
                joinRoomPin = ""
            }
        }
        .transition(.opacity)
    }

    // MARK: - Room Row
    @ViewBuilder
    private func roomRow(_ room: RoomInfo) -> some View {
        VStack(spacing: 0) {
            Button {
                selectedRoomId = selectedRoomId == room.id ? nil : room.id
                joinRoomPin = ""
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(room.roomId)
                            .font(.system(.subheadline, design: .monospaced).bold())
                            .foregroundColor(.yellow)
                        Text(room.hostName)
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                    Spacer()
                    if room.hasPassword {
                        Text("🔒").font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.orange.opacity(0.15))
                            .cornerRadius(8)
                    } else if room.state != "WAITING_FOR_PLAYERS" {
                        Text(s.spectate).font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.purple.opacity(0.15))
                            .foregroundColor(.purple)
                            .cornerRadius(8)
                    } else {
                        Text("Open").font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green.opacity(0.15))
                            .foregroundColor(.green)
                            .cornerRadius(8)
                    }
                }
                .padding(12)
            }

            if selectedRoomId == room.id {
                VStack(spacing: 10) {
                    HStack(spacing: 12) {
                        Text("👥 \(room.playerCount)/2").font(.caption).foregroundColor(.gray)
                        Text("⏱ \(room.timeLimit / 60) \(s.min)").font(.caption).foregroundColor(.gray)
                        Text(room.state == "WAITING_FOR_PLAYERS" ? s.waiting : s.battle)
                            .font(.caption)
                            .foregroundColor(room.state == "WAITING_FOR_PLAYERS" ? .green : .purple)
                    }

                    if room.hasPassword {
                        TextField(s.threedigitPin, text: $joinRoomPin)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.center)
                            .font(.system(.body, design: .monospaced))
                            .padding(8)
                            .background(Color.white.opacity(0.08))
                            .cornerRadius(8)
                            .onChange(of: joinRoomPin) { newVal in
                                joinRoomPin = String(newVal.filter(\.isNumber).prefix(3))
                            }
                    }

                    HStack(spacing: 8) {
                        Button {
                            pendingJoin = PendingJoin(
                                roomId: room.roomId,
                                password: room.hasPassword ? joinRoomPin : nil
                            )
                            vm.loginView = "enterName"
                        } label: {
                            Text(room.playerCount >= 2 ? "Full" : s.joinRoom)
                                .font(.caption.bold())
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(LinearGradient(colors: [.blue, .indigo], startPoint: .leading, endPoint: .trailing))
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .disabled(room.playerCount >= 2 || room.state != "WAITING_FOR_PLAYERS" || (room.hasPassword && joinRoomPin.count != 3))
                        .opacity((room.playerCount >= 2 || (room.hasPassword && joinRoomPin.count != 3)) ? 0.4 : 1)

                        Button {
                            pendingJoin = PendingJoin(
                                roomId: room.roomId,
                                password: room.hasPassword ? joinRoomPin : nil,
                                isSpectating: true
                            )
                            vm.loginView = "enterName"
                        } label: {
                            Text("👁️ \(s.spectate)")
                                .font(.caption.bold())
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.purple.opacity(0.4)))
                                .foregroundColor(.purple)
                                .cornerRadius(10)
                        }
                        .disabled(room.hasPassword && joinRoomPin.count != 3)
                        .opacity((room.hasPassword && joinRoomPin.count != 3) ? 0.4 : 1)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 12)
                .background(Color.white.opacity(0.03))
            }
        }
        .background(Color.white.opacity(0.05))
        .cornerRadius(12)
    }

    // MARK: - Enter PIN
    private var enterPinView: some View {
        VStack(spacing: 20) {
            if let pj = pendingJoin {
                VStack(spacing: 8) {
                    Text(pj.isSpectating ? s.spectatorName : s.joinGameTitle)
                        .font(.caption).foregroundColor(.blue)
                    Text(pj.roomId)
                        .font(.system(size: 32, weight: .black, design: .monospaced))
                        .foregroundColor(.yellow)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(glassCard)
            }

            VStack(spacing: 20) {
                Text("🔐").font(.system(size: 44))
                Text(s.enterPin).font(.headline).foregroundColor(.white)

                TextField("• • •", text: $joinRoomPin)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.center)
                    .font(.system(size: 28, design: .monospaced))
                    .padding(12)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(10)
                    .onChange(of: joinRoomPin) { newVal in
                        joinRoomPin = String(newVal.filter(\.isNumber).prefix(3))
                    }

                Button {
                    guard joinRoomPin.count == 3 else {
                        vm.setMessage(s.incorrectPin, "error")
                        return
                    }
                    pendingJoin?.password = joinRoomPin
                    vm.loginView = "enterName"
                } label: {
                    Text(joinRoomPin.count == 3 ? "🔓 Continue" : "🔒 Continue")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(joinRoomPin.count == 3
                            ? LinearGradient(colors: [.orange, .yellow], startPoint: .leading, endPoint: .trailing)
                            : LinearGradient(colors: [.gray.opacity(0.4), .gray.opacity(0.3)], startPoint: .leading, endPoint: .trailing))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
                .disabled(joinRoomPin.count != 3)
            }
            .padding(24)
            .background(glassCard)

            backButton {
                vm.loginView = "join"
                joinRoomPin = ""
            }
        }
        .transition(.opacity)
    }

    // MARK: - Enter Name
    private var enterNameView: some View {
        VStack(spacing: 20) {
            if let pj = pendingJoin {
                VStack(spacing: 4) {
                    Text(pj.isCreating ? s.createNewGame : pj.isSpectating ? s.watchGame : s.joinGameTitle)
                        .font(.caption)
                        .foregroundColor(.blue)
                    if !pj.isCreating {
                        Text(pj.roomId)
                            .font(.system(size: 28, weight: .black, design: .monospaced))
                            .foregroundColor(.yellow)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity)
                .background(glassCard)
            }

            VStack(spacing: 16) {
                Text(s.enterYourName).font(.headline).foregroundColor(.white)

                TextField(s.yourNamePlaceholder, text: $vm.playerName)
                    .textInputAutocapitalization(.words)
                    .padding(12)
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(10)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.blue.opacity(0.3)))
                    .foregroundColor(.white)

                Button {
                    guard let pj = pendingJoin else { return }
                    vm.joinGame(
                        roomId: pj.roomId,
                        password: pj.password,
                        name: vm.playerName,
                        isCreating: pj.isCreating,
                        isSpectating: pj.isSpectating,
                        timeLimit: pj.timeLimit
                    )
                } label: {
                    Text(pendingJoin?.isCreating == true ? s.createAndJoin
                        : pendingJoin?.isSpectating == true ? s.watchGame
                        : s.joinGameBtn)
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(LinearGradient(
                            colors: pendingJoin?.isCreating == true ? [.green, .green.opacity(0.8)] : [.blue, .indigo],
                            startPoint: .leading, endPoint: .trailing))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
                .disabled(vm.playerName.trimmingCharacters(in: .whitespaces).isEmpty)
                .opacity(vm.playerName.trimmingCharacters(in: .whitespaces).isEmpty ? 0.5 : 1)
            }
            .padding(20)
            .background(glassCard)

            backButton {
                if pendingJoin?.isCreating == true {
                    vm.loginView = "create"
                } else {
                    vm.loginView = "join"
                }
                pendingJoin = nil
            }
        }
        .transition(.opacity)
    }

    // MARK: - Helpers
    private var glassCard: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(Color.white.opacity(0.06))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )
    }

    private func backButton(action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text("← \(s.back)")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.gray)
        }
    }

    private func generateRoomId() -> String {
        let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<6).map { _ in chars.randomElement()! })
    }
}
