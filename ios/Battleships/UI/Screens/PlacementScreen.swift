import SwiftUI

struct PlacementScreen: View {
    @ObservedObject var vm: GameViewModel
    @State private var selectedShip: Int = 0
    @State private var hoverCell: (Int, Int)?

    private var s: I18nStrings { vm.s }
    private let colHeaders = (0..<GRID_SIZE).map { String(UnicodeScalar(65 + $0)!) }

    var body: some View {
        if vm.isSpectator {
            spectatorView
        } else {
            playerView
        }
    }

    // MARK: - Spectator Full-Screen View
    private var spectatorView: some View {
        VStack(spacing: 24) {
            Spacer()

            VStack(spacing: 20) {
                Text("🚢")
                    .font(.system(size: 48))

                Text(s.spectating)
                    .font(.title3.bold())
                    .foregroundColor(.white)

                Text(s.playersPlacingShips)
                    .font(.subheadline)
                    .foregroundColor(.gray)

                BouncingDotsView()
                    .padding(.top, 4)
            }
            .padding(32)
            .frame(maxWidth: 320)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.white.opacity(0.06))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1)))
            )

            Button {
                vm.handleBackToMenu()
            } label: {
                Text("← \(s.leave)")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.red.opacity(0.8))
            }

            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Player View
    private var playerView: some View {
        ScrollView {
            VStack(spacing: 16) {
                controlsPanel
                boardSection
                readySection

                Button {
                    vm.handleBackToMenu()
                } label: {
                    Text("← \(s.leave)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.red.opacity(0.8))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
    }

    // MARK: - Controls
    private var controlsPanel: some View {
        VStack(spacing: 14) {
            // Ship selector
            HStack(spacing: 6) {
                ForEach(SHIPS) { ship in
                    let isPlaced = vm.clientPlacements.contains { $0.shipId == ship.id }
                    let isSelected = selectedShip == ship.id && !isPlaced

                    Button {
                        if !isPlaced {
                            selectedShip = ship.id
                        }
                    } label: {
                        VStack(spacing: 4) {
                            Text(isPlaced ? "✅" : ship.emoji)
                                .font(.title3)
                            Text(ship.name)
                                .font(.system(size: 8, weight: .bold))
                                .lineLimit(1)
                            Text("\(ship.length)×")
                                .font(.system(size: 7))
                                .foregroundColor(.gray)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(isPlaced
                                    ? Color.green.opacity(0.3)
                                    : isSelected
                                        ? Color.purple.opacity(0.5)
                                        : Color.white.opacity(0.08))
                        )
                        .overlay(
                            isSelected
                                ? RoundedRectangle(cornerRadius: 10).stroke(Color.yellow, lineWidth: 2)
                                : nil
                        )
                    }
                    .disabled(vm.isReady)
                }
            }

            // Direction + action buttons
            HStack {
                // Direction toggle
                HStack(spacing: 0) {
                    directionButton(s.horiz, "horizontal")
                    directionButton(s.vert, "vertical")
                }
                .background(Color.white.opacity(0.08))
                .cornerRadius(10)

                Spacer()

                Button {
                    vm.randomPlacement()
                } label: {
                    Text("🎲")
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(LinearGradient(colors: [.orange, .yellow.opacity(0.8)], startPoint: .leading, endPoint: .trailing))
                        .cornerRadius(10)
                }
                .disabled(vm.isReady)

                Button {
                    vm.clientPlacements = []
                    vm.playerBoard = createEmptyBoard()
                    vm.shipsPlaced = 0
                } label: {
                    Text("🗑️")
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(10)
                }
                .disabled(vm.isReady || vm.clientPlacements.isEmpty)
            }

            // Progress bar
            VStack(spacing: 6) {
                HStack {
                    Text(s.shipsCount.fmt("\(vm.clientPlacements.count)/\(SHIPS.count)"))
                        .font(.caption2)
                        .foregroundColor(.gray)
                    Spacer()
                    Text("\(SHIPS.count - vm.clientPlacements.count) remaining")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.white.opacity(0.08))
                            .frame(height: 6)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(LinearGradient(colors: [.green, .green.opacity(0.7)], startPoint: .leading, endPoint: .trailing))
                            .frame(width: geo.size.width * CGFloat(vm.clientPlacements.count) / CGFloat(SHIPS.count), height: 6)
                            .animation(.easeInOut(duration: 0.3), value: vm.clientPlacements.count)
                    }
                }
                .frame(height: 6)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(
                    vm.isReady ? Color.green.opacity(0.3) : Color.white.opacity(0.08), lineWidth: 1))
        )
        .opacity(vm.isReady ? 0.6 : 1)
        .allowsHitTesting(!vm.isReady)
    }

    private func directionButton(_ label: String, _ dir: String) -> some View {
        Button {
            vm.placementDirection = dir
        } label: {
            Text(label)
                .font(.caption.bold())
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(vm.placementDirection == dir ? Color.blue : Color.clear)
                .foregroundColor(.white)
                .cornerRadius(10)
        }
    }

    // MARK: - Board
    private var boardSection: some View {
        VStack(spacing: 8) {
            if vm.isReady && !vm.isSpectator {
                Text(s.lockedHint)
                    .font(.caption2.bold())
                    .foregroundColor(.green)
            } else if !vm.isSpectator {
                Text(s.placementHint)
                    .font(.caption2)
                    .foregroundColor(.gray)
            }

            // 10x10 grid with headers
            VStack(spacing: 1) {
                // Column headers
                HStack(spacing: 1) {
                    Color.clear.frame(width: 20, height: 20)
                    ForEach(0..<GRID_SIZE, id: \.self) { c in
                        Text(colHeaders[c])
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.blue.opacity(0.7))
                            .frame(maxWidth: .infinity)
                            .frame(height: 20)
                    }
                }

                ForEach(0..<GRID_SIZE, id: \.self) { row in
                    HStack(spacing: 1) {
                        // Row header
                        Text("\(row + 1)")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.blue.opacity(0.7))
                            .frame(width: 20)

                        ForEach(0..<GRID_SIZE, id: \.self) { col in
                            let cell = vm.playerBoard[row][col]
                            placementCell(cell: cell, row: row, col: col)
                        }
                    }
                }
            }
            .aspectRatio(1.05, contentMode: .fit)
            .padding(8)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white.opacity(0.04))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(
                        vm.isReady ? Color.green.opacity(0.3) : Color.white.opacity(0.08), lineWidth: 1))
            )

            // Legend
            HStack(spacing: 12) {
                legendItem(color: Color(red: 0.1, green: 0.3, blue: 0.6), label: "Water")
                legendItem(color: Color(red: 0.2, green: 0.7, blue: 0.4), label: "Ship")
            }
            .font(.caption2)
            .foregroundColor(.gray)
        }
    }

    private func placementCell(cell: String, row: Int, col: Int) -> some View {
        let isShip = cell == CellState.SHIP

        return Rectangle()
            .fill(isShip
                ? Color(red: 0.2, green: 0.7, blue: 0.4)
                : Color(red: 0.1, green: 0.3, blue: 0.6))
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .border(Color.black.opacity(0.15), width: 0.5)
            .onTapGesture {
                guard !vm.isReady && !vm.isSpectator else { return }
                let shipId = selectedShip
                vm.placeShip(shipId: shipId, row: row, col: col)
                // Auto-advance to next unplaced ship
                if let next = SHIPS.first(where: { s in !vm.clientPlacements.contains { $0.shipId == s.id } }) {
                    selectedShip = next.id
                }
            }
    }

    private func legendItem(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 10, height: 10)
            Text(label)
        }
    }

    // MARK: - Ready Section
    private var readySection: some View {
        VStack(spacing: 12) {
            // Opponent status
            HStack(spacing: 8) {
                Circle()
                    .fill(vm.opponentReady ? Color.green : Color.orange)
                    .frame(width: 8, height: 8)
                Text(vm.opponentReady ? s.opponentIsReady : s.opponentPlacing.fmt(vm.opponentName))
                    .font(.caption)
                    .foregroundColor(vm.opponentReady ? .green : .orange)
            }

            if vm.isReady {
                Button {
                    vm.unreadyPlacement()
                } label: {
                    Text("🔓 \(s.notReady)")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.orange)
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
            } else {
                Button {
                    vm.confirmPlacement()
                } label: {
                    Text("✅ \(s.ready)")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(
                            vm.clientPlacements.count == SHIPS.count
                                ? LinearGradient(colors: [.green, .green.opacity(0.8)], startPoint: .leading, endPoint: .trailing)
                                : LinearGradient(colors: [.gray.opacity(0.3), .gray.opacity(0.2)], startPoint: .leading, endPoint: .trailing)
                        )
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
                .disabled(vm.clientPlacements.count != SHIPS.count)
            }
        }
    }
}

// MARK: - Bouncing Dots Animation
private struct BouncingDotsView: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(Color.blue.opacity(0.8))
                    .frame(width: 8, height: 8)
                    .offset(y: animating ? -6 : 0)
                    .animation(
                        .easeInOut(duration: 0.5)
                            .repeatForever(autoreverses: true)
                            .delay(Double(i) * 0.15),
                        value: animating
                    )
            }
        }
        .onAppear { animating = true }
    }
}
