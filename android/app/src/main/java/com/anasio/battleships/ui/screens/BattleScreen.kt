package com.anasio.battleships.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.anasio.battleships.data.CellState
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.i18n.fmt
import com.anasio.battleships.ui.components.GameBoard
import com.anasio.battleships.ui.components.GameTimer
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.viewmodel.GameViewModel

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val playerBoard by viewModel.playerBoard.collectAsState()
    val opponentBoard by viewModel.opponentBoard.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val myId by viewModel.playerId.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val isSpectator by viewModel.isSpectator.collectAsState()
    val spectatorCount by viewModel.spectatorCount.collectAsState()
    val spectatorBoards by viewModel.spectatorBoards.collectAsState()
    val playerTimeLeft by viewModel.playerTimeLeft.collectAsState()
    val turnStartedAt by viewModel.turnStartedAt.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val showSurrenderDialog by viewModel.showSurrenderDialog.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    val mySunkCount by viewModel.mySunkCount.collectAsState()
    val theirSunkCount by viewModel.theirSunkCount.collectAsState()
    val lastShotKey by viewModel.lastShotKey.collectAsState()
    val explosionKeys by viewModel.explosionKeys.collectAsState()

    val isMyTurn by remember { derivedStateOf { !isSpectator && currentTurn == myId } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Audio toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = viewModel::toggleSound,
                modifier = Modifier.size(44.dp).semantics { contentDescription = s.toggleSound },
            ) {
                Text(if (soundEnabled) "🔊" else "🔇", fontSize = 18.sp)
            }
            IconButton(
                onClick = viewModel::toggleMusic,
                modifier = Modifier.size(44.dp).semantics { contentDescription = s.toggleMusic },
            ) {
                Text(if (musicEnabled) "🎵" else "🔕", fontSize = 18.sp)
            }
        }

        // Timer
        val spectatorNameMap = remember(spectatorBoards) {
            spectatorBoards.associateBy({ it.playerId }, { it.playerName })
        }
        if (playerTimeLeft.isNotEmpty()) {
            GameTimer(
                playerTimeLeft = playerTimeLeft,
                turnStartedAt = turnStartedAt,
                currentTurn = currentTurn,
                myId = myId,
                opponentName = opponentName,
                isSpectator = isSpectator,
                spectatorPlayerNames = spectatorNameMap,
            )
            Spacer(Modifier.height(4.dp))
        }

        // Turn indicator
        val turnText = when {
            isSpectator -> {
                val name = spectatorBoards.find { it.playerId == currentTurn }?.playerName ?: "?"
                s.namesTurn.fmt(name)
            }
            isMyTurn -> "🎯 ${s.yourTurnFire}"
            else -> "⏳ ${s.opponentsTurn}"
        }
        val turnColor = when {
            isSpectator -> c.primary
            isMyTurn -> c.green
            else -> c.orange
        }
        AnimatedContent(
            targetState = isMyTurn,
            transitionSpec = {
                (fadeIn() + slideInVertically { -it / 2 }) togetherWith
                        (fadeOut() + slideOutVertically { it / 2 })
            },
            label = "turnTransition",
        ) { myTurn ->
            val text = if (myTurn) "🎯 ${s.yourTurnFire}" else if (isSpectator) turnText else "⏳ ${s.opponentsTurn}"
            val color = if (myTurn) c.green else if (isSpectator) c.primary else c.orange
            Text(
                text,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = .1f), RoundedCornerShape(8.dp))
                    .border(1.dp, color.copy(alpha = .3f), RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp),
            )
        }
        // Extra shot hint (always rendered to prevent layout shift)
        Text(
            s.extraShotHint, fontSize = 10.sp, color = c.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().alpha(if (isMyTurn) 1f else 0f),
        )
        Spacer(Modifier.height(4.dp))

        // Ship scoreboard
        if (!isSpectator) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SunkDots(label = s.yourHits, count = mySunkCount, dotColor = c.green)
                SunkDots(label = s.theirHits, count = theirSunkCount, dotColor = c.red)
            }
            Spacer(Modifier.height(4.dp))
        }

        // Message
        if (message.isNotBlank()) {
            MessageBanner(message, messageType)
            Spacer(Modifier.height(2.dp))
        }

        if (isSpectator) {
            // Spectator: show both boards
            spectatorBoards.forEach { sb ->
                GameBoard(
                    board = sb.board,
                    label = sb.playerName,
                    showShips = true,
                    interactive = false,
                )
                Spacer(Modifier.height(8.dp))
            }
        } else {
            // Enemy waters (interactive)
            GameBoard(
                board = opponentBoard,
                label = "🎯 ${s.enemyWaters}",
                showShips = false,
                interactive = isMyTurn,
                lastShotKey = lastShotKey,
                explosionKeys = explosionKeys,
                onCellClick = { r, col ->
                    val cell = opponentBoard[r][col]
                    if (cell == CellState.WATER) viewModel.handleShoot(r, col)
                },
            )
            Spacer(Modifier.height(8.dp))
            // Your fleet (read only)
            GameBoard(
                board = playerBoard,
                label = "🚥 ${s.yourFleet}",
                showShips = true,
                interactive = false,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spectatorCount > 0) {
                Text("👁 $spectatorCount ${s.spectatorCount}", fontSize = 11.sp, color = c.textDim)
            } else Spacer(Modifier.width(1.dp))

            if (!isSpectator) {
                OutlinedButton(
                    onClick = viewModel::requestForfeit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.red),
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("🏳 ${s.surrender}", fontSize = 12.sp) }
            } else {
                OutlinedButton(
                    onClick = viewModel::handleBackToMenu,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("← ${s.leave}", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showSurrenderDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelForfeit,
            title = { Text(s.surrender) },
            text = { Text(s.confirmSurrender) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmForfeit) { Text(s.yes) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelForfeit) { Text(s.no) }
            },
        )
    }
}

@Composable
private fun SunkDots(label: String, count: Int, dotColor: Color) {
    val c = LocalColorPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 10.sp, color = dotColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(1.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i < count) dotColor else c.card)
                    .border(0.5.dp, if (i < count) dotColor else c.border, RoundedCornerShape(50)),
            )
        }
    }
}
