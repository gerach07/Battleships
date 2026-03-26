package com.anasio.battleships.data

const val GRID_SIZE = 10

// Change to "http://10.0.2.2:3001" for local dev with Android emulator
const val SERVER_URL = "https://battleships-server-jtit.onrender.com"

data class ShipDef(val id: Int, val length: Int, val name: String, val emoji: String)

val SHIPS = listOf(
    ShipDef(0, 5, "Carrier", "🛥️"),
    ShipDef(1, 4, "Battleship", "⛵"),
    ShipDef(2, 3, "Destroyer", "🚢"),
    ShipDef(3, 3, "Submarine", "🤿"),
    ShipDef(4, 2, "Patrol", "🚤"),
)

object CellState {
    const val WATER = "W"
    const val SHIP = "S"
    const val HIT = "H"
    const val MISS = "M"
    const val SUNK = "X"
    const val SAFE = "Z"
}
