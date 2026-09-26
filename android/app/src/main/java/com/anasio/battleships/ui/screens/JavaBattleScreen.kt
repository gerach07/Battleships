package com.anasio.battleships.ui.screens

import androidx.compose.runtime.*
import android.app.Dialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.anasio.battleships.data.CellState
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.i18n.fmt
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.ui.views.JavaBattleSurrenderDialog
import com.anasio.battleships.ui.views.JavaBattleView
import com.anasio.battleships.viewmodel.GameViewModel

/**
 * Kotlin Composable adapter that bridges [GameViewModel] state to [JavaBattleView].
 *
 * Collects all StateFlow values from the ViewModel and forwards them to the
 * programmatic Java View via its update methods. Routes user-action callbacks
 * from the Java View back to the ViewModel.
 */
@Composable
fun JavaBattleScreen(viewModel: GameViewModel) {
    // ── Collect all ViewModel state ──
    val playerBoard by viewModel.playerBoard.collectAsState()
    val opponentBoard by viewModel.opponentBoard.collectAsState()
    val spectatorBoards by viewModel.spectatorBoards.collectAsState()
    val opponentLastShotKey by viewModel.opponentLastShotKey.collectAsState()
    val playerLastShotKey by viewModel.playerLastShotKey.collectAsState()
    val opponentExplosionKeys by viewModel.opponentExplosionKeys.collectAsState()
    val playerExplosionKeys by viewModel.playerExplosionKeys.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val myId by viewModel.playerId.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val isSpectator by viewModel.isSpectator.collectAsState()
    val spectatorCount by viewModel.spectatorCount.collectAsState()
    val playerTimeLeft by viewModel.playerTimeLeft.collectAsState()
    val turnStartedAt by viewModel.turnStartedAt.collectAsState()
    val showSurrenderDialog by viewModel.showSurrenderDialog.collectAsState()
    val bombUsed by viewModel.bombUsed.collectAsState()
    val bombMode by viewModel.bombMode.collectAsState()
    val mySunkCount by viewModel.mySunkCount.collectAsState()
    val theirSunkCount by viewModel.theirSunkCount.collectAsState()

    // ── Derive turn state ──
    val isMyTurn by remember { derivedStateOf { !isSpectator && currentTurn == myId } }

    // ── Read Compose locals ──
    val s = LocalI18n.current
    val c = LocalColorPalette.current
    val ctx = LocalContext.current

    // ── Turn text and colour (mirrors original BattleScreen logic) ──
    val activePlayerName = spectatorBoards.firstOrNull { it.playerId == currentTurn }?.playerName ?: opponentName
    val turnText = when {
        isSpectator -> s.namesTurn.fmt(activePlayerName)
        isMyTurn -> "🎯 ${s.yourTurnFire}"
        else -> "⏳ ${s.namesTurn.fmt(opponentName)}"
    }
    val turnColor = when {
        isSpectator -> c.primary
        isMyTurn -> c.green
        else -> c.orange
    }

    // ── Surrender dialog ──
    DisposableEffect(showSurrenderDialog) {
        val dialog: Dialog? = if (showSurrenderDialog) {
            JavaBattleSurrenderDialog.show(
                ctx,
                s.surrender,
                s.confirmSurrender,
                s.yes,
                s.no,
                c.surface.toArgb(),
                c.border.toArgb(),
                c.primary.toArgb(),
                c.red.toArgb(),
                c.textPrimary.toArgb(),
                c.textDim.toArgb(),
                { viewModel.confirmForfeit() },
                { viewModel.cancelForfeit() },
            )
        } else null
        onDispose { dialog?.dismiss() }
    }

    // ── AndroidView hosting the Java battle UI ──
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            JavaBattleView(context).apply {
                // Wire callbacks once during creation
                setOnCellClickListener { row, col ->
                    val cell = viewModel.opponentBoard.value[row][col]
                    if (cell == CellState.WATER) {
                        if (viewModel.bombMode.value) {
                            viewModel.handleUseBomb(row, col)
                        } else {
                            viewModel.handleShoot(row, col)
                        }
                    }
                }
                setOnSurrenderListener { viewModel.requestForfeit() }
                setOnBombToggleListener { viewModel.toggleBombMode() }
                setOnLeaveListener { viewModel.handleBackToMenu() }
            }
        },
        update = { view ->
            // ── Push colours ──
            view.updateColors(
                c.background.toArgb(), c.surface.toArgb(), c.card.toArgb(), c.border.toArgb(),
                c.textPrimary.toArgb(), c.textDim.toArgb(), c.primary.toArgb(), c.primaryDark.toArgb(),
                c.yellow.toArgb(), c.green.toArgb(), c.red.toArgb(), c.orange.toArgb(),
                c.cellWater.toArgb(), c.cellShip.toArgb(), c.cellHit.toArgb(), c.cellMiss.toArgb(),
                c.cellSunk.toArgb(), c.cellSafe.toArgb(),
            )

            // ── Push strings ──
            view.updateStrings(
                s.you, s.opponent,
                s.yourTurnFire, s.extraShotHint,
                s.bomb, s.bombTargetHint,
                s.enemyWaters, s.yourFleet,
                s.surrender, s.leave,
                s.yourHits, s.theirHits,
                s.namesTurn, s.spectatorCount,
                s.boardWater, s.boardShip,
                s.boardHit, s.boardMiss, s.boardSunk,
                s.cellHit, s.cellMiss, s.cellSunk,
                s.cellShip, s.cellWater, s.cellSafe,
            )

            // ── Push game state ──
            view.updateTurn(isMyTurn, turnText, turnColor.toArgb(), isSpectator, opponentName, playerName)
            if (isSpectator) {
                view.updateSpectatorBoards(
                    spectatorBoards.map { it.playerId },
                    spectatorBoards.map { it.playerName },
                    spectatorBoards.map { it.board },
                )
            } else {
                view.updateBoards(playerBoard, opponentBoard, isMyTurn)
            }
            view.updateShotEffects(
                opponentLastShotKey,
                playerLastShotKey,
                opponentExplosionKeys,
                playerExplosionKeys,
            )
            view.updateTimers(playerTimeLeft, turnStartedAt, currentTurn, myId)
            view.updateScoreboard(mySunkCount, theirSunkCount)
            view.updateBombState(bombUsed, bombMode, isMyTurn)
            view.updateSpectatorCount(spectatorCount)
        },
    )
}
