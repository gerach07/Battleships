import SwiftUI

// Loaded once from bundle; nil if asset is missing (falls back to orange overlay)
private let explosionUIImage: UIImage? = {
    guard let url = Bundle.main.url(forResource: "ship-sink-explosion", withExtension: "webp"),
          let data = try? Data(contentsOf: url) else { return nil }
    return UIImage(data: data)
}()

struct GameBoardView: View {
    let board: Board
    let isOpponentBoard: Bool
    let isInteractive: Bool
    let shotKeys: Set<String>
    let explosionKeys: Set<String>
    var onCellTap: ((Int, Int) -> Void)? = nil

    var body: some View {
        VStack(spacing: 1) {
            ForEach(0..<GRID_SIZE, id: \.self) { row in
                HStack(spacing: 1) {
                    ForEach(0..<GRID_SIZE, id: \.self) { col in
                        let cell = board[row][col]
                        let key = "\(row),\(col)"
                        let isShot = shotKeys.contains(key)
                        let isExplosion = explosionKeys.contains(key)

                        ZStack {
                            cellColor(cell, isOpponent: isOpponentBoard)
                                .overlay(
                                    isShot ? Color.white.opacity(0.4) : Color.clear
                                )

                            cellContent(cell)

                            if isExplosion {
                                if let img = explosionUIImage {
                                    Image(uiImage: img)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                                        .clipped()
                                        .allowsHitTesting(false)
                                } else {
                                    Color.orange.opacity(0.7)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .aspectRatio(1, contentMode: .fit)
                        .border(
                            isShot ? Color.yellow : Color.black.opacity(0.15),
                            width: isShot ? 2 : 0.5
                        )
                        .scaleEffect(isShot ? 1.15 : 1.0)
                        .animation(.spring(response: 0.3, dampingFraction: 0.5), value: isShot)
                        .onTapGesture {
                            if isInteractive { onCellTap?(row, col) }
                        }
                    }
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .background(Color.black.opacity(0.1))
        .cornerRadius(6)
    }

    private func cellColor(_ cell: String, isOpponent: Bool) -> Color {
        switch cell {
        case CellState.WATER: return Color(red: 0.1, green: 0.3, blue: 0.6)
        case CellState.SHIP:  return isOpponent ? Color(red: 0.1, green: 0.3, blue: 0.6) : Color(red: 0.4, green: 0.5, blue: 0.65)
        case CellState.HIT:   return Color(red: 0.85, green: 0.2, blue: 0.2)
        case CellState.MISS:  return Color(red: 0.2, green: 0.25, blue: 0.4)
        case CellState.SUNK:  return Color(red: 0.6, green: 0.1, blue: 0.1)
        case CellState.SAFE:  return Color(red: 0.15, green: 0.35, blue: 0.55)
        default:               return Color(red: 0.1, green: 0.3, blue: 0.6)
        }
    }

    @ViewBuilder
    private func cellContent(_ cell: String) -> some View {
        switch cell {
        case CellState.HIT:
            Text("🔥").font(.system(size: 12))
        case CellState.MISS:
            Circle()
                .fill(Color.white.opacity(0.3))
                .frame(width: 6, height: 6)
        case CellState.SUNK:
            Text("💀").font(.system(size: 12))
        case CellState.SAFE:
            Text("·").font(.system(size: 8)).foregroundColor(.white.opacity(0.3))
        default:
            EmptyView()
        }
    }
}
