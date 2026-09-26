package com.anasio.battleships

import com.anasio.battleships.data.CellState
import com.anasio.battleships.util.mutableBoard
import com.anasio.battleships.util.restorePlacementsFromBoard
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun restoresShipPlacementsFromBoard() {
        val board = mutableBoard()
        for (col in 0..4) board[0][col] = CellState.SHIP
        for (row in 2..5) board[row][7] = CellState.SHIP
        for (col in 0..2) board[7][col] = CellState.SHIP
        for (row in 7..9) board[row][5] = CellState.SHIP
        for (col in 8..9) board[9][col] = CellState.SHIP

        val placements = restorePlacementsFromBoard(board.map { it.toList() })

        assertEquals(5, placements.size)
        assertEquals("horizontal", placements.single { it.shipId == 0 }.direction)
        assertEquals("vertical", placements.single { it.shipId == 1 }.direction)
        assertEquals(3, placements.single { it.shipId == 2 }.length)
        assertEquals(3, placements.single { it.shipId == 3 }.length)
        assertEquals("horizontal", placements.single { it.shipId == 4 }.direction)
    }
}