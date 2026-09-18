import Foundation
#if canImport(ActivityKit)
import ActivityKit

/// Shared Live Activity attributes — must be identical in the widget extension.
@available(iOS 16.1, *)
struct BattleshipsAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var phase: String           // "waiting", "placement", "battle", "gameOver"
        var isMyTurn: Bool
        var myTime: Double          // seconds remaining on my clock
        var opponentTime: Double    // seconds remaining on opponent clock
        var mySunkCount: Int        // my ships that opponent has sunk
        var theirSunkCount: Int     // opponent ships I've sunk
        var lastShot: String?       // "hit", "miss", "sunk"
        var iWon: Bool?             // nil = game ongoing
        var opponentName: String
    }

    var roomCode: String
    var playerName: String
}
#endif
