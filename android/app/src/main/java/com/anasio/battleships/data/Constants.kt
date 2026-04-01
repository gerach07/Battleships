package com.anasio.battleships.data

const val GRID_SIZE = 10

val SERVER_URL: String = com.anasio.battleships.BuildConfig.SERVER_URL

data class ShipDef(val id: Int, val length: Int, val name: String, val emoji: String)

val SHIPS = listOf(
    ShipDef(0, 5, "Carrier", "🛥️"),
    ShipDef(1, 4, "Battleship", "⛵"),
    ShipDef(2, 3, "Destroyer", "🚢"),
    ShipDef(3, 3, "Submarine", "🤿"),
    ShipDef(4, 2, "Patrol", "🚤"),
)

// Validation limits (must match server)
const val MAX_CHAT_LENGTH = 200
const val MAX_SPECTATORS = 10
const val MIN_GAME_TIME_SECONDS = 120
const val MAX_GAME_TIME_SECONDS = 600
const val DEFAULT_GAME_TIME_SECONDS = 300

object CellState {
    const val WATER = "W"
    const val SHIP = "S"
    const val HIT = "H"
    const val MISS = "M"
    const val SUNK = "X"
    const val SAFE = "Z"
}
