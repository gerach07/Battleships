package com.anasio.battleships.util

import com.anasio.battleships.data.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

fun createEmptyBoard(): Board = List(GRID_SIZE) { List(GRID_SIZE) { CellState.WATER } }

fun mutableBoard(): MutableList<MutableList<String>> =
    MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { CellState.WATER } }

fun canPlaceShipOnBoard(
    board: Board, row: Int, col: Int, length: Int, dir: String
): Pair<Boolean, List<Pair<Int, Int>>> {
    val cells = mutableListOf<Pair<Int, Int>>()
    if (dir == "horizontal") {
        if (col + length > GRID_SIZE) return false to emptyList()
        for (i in 0 until length) {
            if (board[row][col + i] != CellState.WATER) return false to emptyList()
            cells.add(row to (col + i))
        }
    } else {
        if (row + length > GRID_SIZE) return false to emptyList()
        for (i in 0 until length) {
            if (board[row + i][col] != CellState.WATER) return false to emptyList()
            cells.add((row + i) to col)
        }
    }
    val cellSet = cells.map { "${it.first},${it.second}" }.toSet()
    for (c in cells) {
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = c.first + dr; val nc = c.second + dc
            if (nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE) continue
            if ("$nr,$nc" in cellSet) continue
            if (board[nr][nc] == CellState.SHIP) return false to emptyList()
        }
    }
    return true to cells
}

fun generateRandomPlacement(): Pair<Board, List<PlacedShip>>? {
    repeat(500) {
        val board = mutableBoard()
        val placements = mutableListOf<PlacedShip>()
        var failed = false
        for (ship in SHIPS) {
            var placed = false
            var attempts = 0
            while (!placed && attempts < 150) {
                attempts++
                val dir = if (Random.nextBoolean()) "horizontal" else "vertical"
                val r = Random.nextInt(GRID_SIZE)
                val c = Random.nextInt(GRID_SIZE)
                val boardList: Board = board.map { it.toList() }
                val (valid, cells) = canPlaceShipOnBoard(boardList, r, c, ship.length, dir)
                if (valid) {
                    cells.forEach { (cr, cc) -> board[cr][cc] = CellState.SHIP }
                    placements.add(PlacedShip(ship.id, r, c, ship.length, dir, cells))
                    placed = true
                }
            }
            if (!placed) { failed = true; break }
        }
        if (!failed) return board.map { it.toList() } to placements
    }
    return null
}

fun getSurroundingKeys(shipCells: List<Pair<Int, Int>>): Set<String> {
    val shipSet = shipCells.map { "${it.first},${it.second}" }.toSet()
    val ring = mutableSetOf<String>()
    for (c in shipCells) {
        for (dr in -1..1) for (dc in -1..1) {
            val nr = c.first + dr; val nc = c.second + dc
            if (nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE) continue
            val key = "$nr,$nc"
            if (key !in shipSet) ring.add(key)
        }
    }
    return ring
}

fun overlayBoard(board: Board, sunkSet: Set<String>): Board {
    if (sunkSet.isEmpty()) return board
    return board.mapIndexed { ri, row ->
        row.mapIndexed { ci, cell ->
            val key = "$ri,$ci"
            when {
                sunkSet.contains(key) -> CellState.SUNK
                sunkSet.contains("${key}_safe") && (cell == CellState.WATER || cell == CellState.MISS) -> CellState.SAFE
                else -> cell
            }
        }
    }
}

fun parseBoardFromJson(json: JSONArray): Board {
    return try {
        (0 until GRID_SIZE.coerceAtMost(json.length())).map { i ->
            val row = json.getJSONArray(i)
            (0 until GRID_SIZE.coerceAtMost(row.length())).map { j -> row.getString(j) }
                .let { cells -> cells + List((GRID_SIZE - cells.size).coerceAtLeast(0)) { CellState.WATER } }
        }.let { rows -> rows + List((GRID_SIZE - rows.size).coerceAtLeast(0)) { List(GRID_SIZE) { CellState.WATER } } }
    } catch (e: Exception) {
        createEmptyBoard()
    }
}

fun parseTimeLeft(json: JSONObject): Map<String, Double> {
    val map = mutableMapOf<String, Double>()
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = json.getDouble(key)
    }
    return map
}
