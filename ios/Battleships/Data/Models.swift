import Foundation

typealias Board = [[String]]

struct RoomInfo: Identifiable {
    let id: String  // roomId
    let hostName: String
    let hasPassword: Bool
    let state: String
    let playerCount: Int
    let spectatorCount: Int
    let timeLimit: Int

    var roomId: String { id }
}

struct ChatMessage: Identifiable, Equatable {
    let id: String
    let senderId: String
    let senderName: String
    let text: String
    let timestamp: Double
    var isMine: Bool = false
    var isImportant: Bool = false
}

struct PlacedShip: Equatable {
    let shipId: Int
    let row: Int
    let col: Int
    let length: Int
    let direction: String
    let cells: [(Int, Int)]

    static func == (lhs: PlacedShip, rhs: PlacedShip) -> Bool {
        lhs.shipId == rhs.shipId && lhs.row == rhs.row && lhs.col == rhs.col
            && lhs.length == rhs.length && lhs.direction == rhs.direction
    }
}

struct PendingJoin {
    var roomId: String = ""
    var password: String? = nil
    var isCreating: Bool = false
    var isSpectating: Bool = false
    var timeLimit: Int = 300
}

struct SpectatorBoard {
    let playerId: String
    let playerName: String
    let board: Board
}
