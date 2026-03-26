package com.anasio.battleships.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun GameTimer(
    playerTimeLeft: Map<String, Double>,
    turnStartedAt: Long?,
    currentTurn: String?,
    myId: String?,
    opponentName: String,
    isSpectator: Boolean = false,
    spectatorPlayerNames: Map<String, String> = emptyMap(),
) {
    if (playerTimeLeft.isEmpty()) return
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentTurn, turnStartedAt) {
        if (currentTurn != null && turnStartedAt != null) {
            while (true) { delay(250); tick++ }
        }
    }

    // Force recomposition
    @Suppress("UNUSED_EXPRESSION") tick

    val ids = playerTimeLeft.keys.toList()
    val ordered = if (!isSpectator && myId != null && myId in ids) {
        listOf(myId) + ids.filter { it != myId }
    } else ids

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ordered.forEach { pid ->
            val isActive = pid == currentTurn
            val stored = playerTimeLeft[pid] ?: 0.0
            val live = if (isActive && turnStartedAt != null && turnStartedAt > 0) {
                maxOf(0.0, stored - (System.currentTimeMillis() - turnStartedAt) / 1000.0)
            } else stored
            val isCritical = isActive && live <= 10
            val isLow = isActive && live <= 30
            val label = when {
                isSpectator -> spectatorPlayerNames[pid] ?: pid.take(6)
                pid == myId -> s.you
                else -> opponentName.ifEmpty { s.opponent }
            }
            val bgBrush = when {
                isCritical -> Brush.horizontalGradient(listOf(Color(0xFFB91C1C), Color(0xFF991B1B)))
                isLow -> Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                isActive -> Brush.horizontalGradient(listOf(c.primaryDark, c.primary))
                else -> Brush.horizontalGradient(listOf(c.card, c.card))
            }
            val borderColor = when {
                isCritical -> c.red
                isActive -> c.primary
                else -> c.border
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgBrush)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, fontSize = 11.sp, color = c.textDim, fontWeight = FontWeight.Medium)
                Text(
                    formatTime(live),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCritical -> c.yellow
                        isActive -> Color.White
                        else -> c.textPrimary
                    },
                    textAlign = TextAlign.Center,
                )
                if (isActive) {
                    Text("▶ ${s.ticking}", fontSize = 9.sp, color = c.yellow.copy(alpha = .7f))
                }
            }
        }
    }
}

private fun formatTime(secs: Double): String {
    val s = maxOf(0, ceil(secs).toInt())
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
