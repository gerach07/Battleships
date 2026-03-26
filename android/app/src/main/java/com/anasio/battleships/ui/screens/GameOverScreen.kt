package com.anasio.battleships.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.i18n.fmt
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.viewmodel.GameViewModel

@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val winner by viewModel.winner.collectAsState()
    val myId by viewModel.playerId.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val playAgainPending by viewModel.playAgainPending.collectAsState()
    val opponentWantsPlayAgain by viewModel.opponentWantsPlayAgain.collectAsState()
    val isSpectator by viewModel.isSpectator.collectAsState()
    val spectatorBoards by viewModel.spectatorBoards.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    val iWon = winner == myId && !isSpectator
    val winnerName = when {
        isSpectator -> spectatorBoards.find { it.playerId == winner }?.playerName ?: "?"
        iWon -> s.you
        else -> opponentName.ifEmpty { s.opponent }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Result card with gradient background
        val cardBg = if (iWon && !isSpectator) {
            Brush.verticalGradient(listOf(
                c.yellow.copy(alpha = .08f),
                c.green.copy(alpha = .05f),
                Color.Transparent,
            ))
        } else if (!isSpectator) {
            Brush.verticalGradient(listOf(
                c.red.copy(alpha = .08f),
                Color.Transparent,
            ))
        } else {
            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(
                    1.dp,
                    if (iWon && !isSpectator) c.yellow.copy(alpha = .25f)
                    else if (!isSpectator) c.red.copy(alpha = .2f)
                    else c.border.copy(alpha = .2f),
                    RoundedCornerShape(20.dp),
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val emoji = if (isSpectator) "🏁" else if (iWon) "🏆" else "💀"
                Text(emoji, fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isSpectator) s.gameOver else if (iWon) s.victory else s.defeat,
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    color = if (iWon || isSpectator) c.yellow else c.red,
                    letterSpacing = if (iWon) 2.sp else 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isSpectator) s.winsMessage.fmt(winnerName) else if (iWon) s.youSunkEnemy else s.destroyedYourFleet.fmt(winnerName),
                    fontSize = 14.sp, color = c.textPrimary, textAlign = TextAlign.Center,
                )
                if (iWon && !isSpectator) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { Text("⭐", fontSize = 20.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (message.isNotBlank()) {
            MessageBanner(message, messageType)
            Spacer(Modifier.height(12.dp))
        }

        // Play Again flow
        if (!isSpectator) {
            if (opponentWantsPlayAgain && !playAgainPending) {
                // Opponent wants rematch
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = c.accent.copy(alpha = .15f)),
                    border = BorderStroke(1.dp, c.accent.copy(alpha = .4f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🔄 ${s.opWantsRematch}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = viewModel::handlePlayAgain,
                                colors = ButtonDefaults.buttonColors(containerColor = c.green),
                            ) { Text(s.accept) }
                            OutlinedButton(
                                onClick = viewModel::handleDeclinePlayAgain,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.red),
                            ) { Text(s.decline) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (playAgainPending) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏳ ${s.waitingForOpponentRematch}", fontSize = 14.sp, color = c.textDim)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = viewModel::handleDeclinePlayAgain) {
                        Text(s.cancelRematch, fontSize = 12.sp, color = c.textDim)
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                GradientButton(
                    "🔄 ${s.playAgain}",
                    c.primaryDark, c.primary,
                ) { viewModel.handlePlayAgain() }
                Spacer(Modifier.height(12.dp))
            }
        }

        OutlinedButton(
            onClick = viewModel::handleBackToMenu,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("🏠 ${s.mainMenu}", fontSize = 15.sp) }

        Spacer(Modifier.height(12.dp))
        Text(
            "Created by Adrians Bergmanis",
            fontSize = 9.sp,
            color = c.textDim.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}
