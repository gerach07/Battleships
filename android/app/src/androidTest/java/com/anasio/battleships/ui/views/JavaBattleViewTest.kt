package com.anasio.battleships.ui.views

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anasio.battleships.data.CellState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JavaBattleViewTest {

    @Test
    fun testJavaBattleViewInitialization() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val view = JavaBattleView(appContext)
        assertNotNull(view)
    }

    @Test
    fun testJavaBattleViewBoardsUpdate() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val view = JavaBattleView(appContext)

        val playerBoard = List(10) { List(10) { CellState.WATER } }
        val opponentBoard = List(10) { List(10) { CellState.WATER } }

        // Must run layout on the UI thread for some view assertions if needed, 
        // but just updating data should be fine
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.updateBoards(playerBoard, opponentBoard, true)
            // Just verifying it doesn't crash on update
            assertNotNull(view)
        }
    }
}
