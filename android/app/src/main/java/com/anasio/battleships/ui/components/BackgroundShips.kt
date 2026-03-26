package com.anasio.battleships.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private val SHIP_EMOJIS = listOf("🚢", "⛵", "🛥️", "🚤", "🛳️", "⛴️")
private const val BG_SHIP_COUNT = 8

private data class ShipData(
    val emoji: String,
    val topFraction: Float,   // 0.08 – 0.88
    val sizeSp: Int,          // 16–36
    val durationMs: Int,      // 18_000–42_000
    val delayFraction: Float, // negative random start offset as fraction 0..1
    val goRight: Boolean,
)

private val shipDataList: List<ShipData> = List(BG_SHIP_COUNT) { i ->
    val goRight = Random.nextBoolean()
    ShipData(
        emoji = SHIP_EMOJIS[i % SHIP_EMOJIS.size],
        topFraction = 0.08f + Random.nextFloat() * 0.80f,
        sizeSp = 16 + Random.nextInt(21),
        durationMs = 18_000 + Random.nextInt(24_001),
        delayFraction = Random.nextFloat(),
        goRight = goRight,
    )
}

@Composable
fun BackgroundShips() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val marginPx = with(density) { 60.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val heightPx = with(density) { maxHeight.toPx() }

        shipDataList.forEach { ship ->
            val infiniteTransition = rememberInfiniteTransition(label = "ship_${ship.emoji}_${ship.topFraction}")

            // Animate from off-screen left to off-screen right (or reversed)
            val startX = -marginPx
            val endX = screenWidthPx + marginPx

            val animatedX by infiniteTransition.animateFloat(
                initialValue = if (ship.goRight) startX else endX,
                targetValue = if (ship.goRight) endX else startX,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ship.durationMs,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(
                        offsetMillis = (ship.delayFraction * ship.durationMs).toInt(),
                        offsetType = StartOffsetType.FastForward,
                    ),
                ),
                label = "shipX_${ship.topFraction}",
            )

            val topDp = with(density) { (ship.topFraction * heightPx).toDp() }
            val xDp = with(density) { animatedX.toDp() }

            Text(
                text = ship.emoji,
                fontSize = ship.sizeSp.sp,
                modifier = Modifier
                    .offset(x = xDp, y = topDp)
                    .alpha(0.07f)
                    .scale(scaleX = if (ship.goRight) 1f else -1f, scaleY = 1f),
            )
        }
    }
}
