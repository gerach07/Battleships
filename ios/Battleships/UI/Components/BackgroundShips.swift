import SwiftUI

private struct ShipData: Identifiable {
    let id = UUID()
    let emoji: String
    let startX: CGFloat
    let startY: CGFloat
    let goRight: Bool
    let speed: Double
    let offset: Double
}

struct BackgroundShipsView: View {
    private let ships: [ShipData] = {
        let emojis = ["🚢","⛵","🛥️","🚤","⛴️","🛳️","🚢","⛵"]
        return emojis.enumerated().map { i, emoji in
            let goRight = Bool.random()
            return ShipData(
                emoji: emoji,
                startX: goRight ? -0.15 : 1.15,
                startY: CGFloat.random(in: 0.05...0.95),
                goRight: goRight,
                speed: Double.random(in: 18...42),
                offset: Double(i) * 3.0
            )
        }
    }()

    var body: some View {
        GeometryReader { geo in
            ForEach(ships) { ship in
                AnimatedShip(ship: ship, size: geo.size)
            }
        }
        .allowsHitTesting(false)
    }
}

private struct AnimatedShip: View {
    let ship: ShipData
    let size: CGSize

    @State private var progress: CGFloat = 0

    var body: some View {
        let startX = ship.startX * size.width
        let endX = ship.goRight ? size.width * 1.15 : -size.width * 0.15
        let currentX = startX + (endX - startX) * progress
        let y = ship.startY * size.height

        Text(ship.emoji)
            .font(.system(size: 28))
            .scaleEffect(x: ship.goRight ? 1 : -1, y: 1)
            .opacity(0.07)
            .position(x: currentX, y: y)
            .onAppear {
                withAnimation(
                    .linear(duration: ship.speed)
                    .repeatForever(autoreverses: false)
                    .delay(ship.offset)
                ) {
                    progress = 1
                }
            }
    }
}
