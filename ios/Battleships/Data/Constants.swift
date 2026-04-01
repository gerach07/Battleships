import Foundation

let GRID_SIZE = 10

#if DEBUG
let SERVER_URL = "http://localhost:3001"
#else
let SERVER_URL = "https://battleships-server-jtit.onrender.com"
#endif

struct ShipDef: Identifiable {
    let id: Int
    let length: Int
    let name: String
    let emoji: String
}

let SHIPS: [ShipDef] = [
    ShipDef(id: 0, length: 5, name: "Carrier", emoji: "🛥️"),
    ShipDef(id: 1, length: 4, name: "Battleship", emoji: "⛵"),
    ShipDef(id: 2, length: 3, name: "Destroyer", emoji: "🚢"),
    ShipDef(id: 3, length: 3, name: "Submarine", emoji: "🤿"),
    ShipDef(id: 4, length: 2, name: "Patrol", emoji: "🚤"),
]

// Validation limits (must match server)
let MAX_CHAT_LENGTH = 200
let MAX_SPECTATORS = 10
let MIN_GAME_TIME_SECONDS = 120
let MAX_GAME_TIME_SECONDS = 600
let DEFAULT_GAME_TIME_SECONDS = 300

enum CellState {
    static let WATER = "W"
    static let SHIP  = "S"
    static let HIT   = "H"
    static let MISS  = "M"
    static let SUNK  = "X"
    static let SAFE  = "Z"
}
