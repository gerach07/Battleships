package com.anasio.battleships.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.anasio.battleships.data.CellState
import com.anasio.battleships.data.GRID_SIZE
import com.anasio.battleships.data.Board
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette

@Composable
fun GameBoard(
    board: Board,
    label: String? = null,
    interactive: Boolean = false,
    showShips: Boolean = true,
    previewCells: Set<String> = emptySet(),
    previewValid: Boolean = true,
    lastShotKey: String? = null,
    explosionKeys: Set<String> = emptySet(),
    onCellClick: (Int, Int) -> Unit = { _, _ -> },
    onDragStart: ((Int, Int) -> Unit)? = null,
    onDragMove: ((Int, Int) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Keep updated references for use inside pointerInput (which doesn't recompose)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDragMove by rememberUpdatedState(onDragMove)
    val currentDragEnd by rememberUpdatedState(onDragEnd)

    val c = LocalColorPalette.current

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (label != null) {
            Text(
                label, color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val headerSize = 18.dp
            val gap = 1.dp
            val totalGap = gap * (GRID_SIZE + 1)
            val cellSize = ((maxWidth - headerSize - totalGap) / GRID_SIZE).coerceAtLeast(28.dp)

            val density = LocalDensity.current
            val cellPx = with(density) { cellSize.toPx() }
            val headerPx = with(density) { headerSize.toPx() }
            val gapPx = with(density) { gap.toPx() }

            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                // Column headers (A-J)
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(Modifier.width(headerSize))
                    repeat(GRID_SIZE) { col ->
                        Text(
                            "${'A' + col}",
                            modifier = Modifier.width(cellSize),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp, color = c.textDim,
                        )
                    }
                }

                // Data rows — wrapped in Box with optional drag gesture
                Box(
                    modifier = if (onDragStart != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val row = (offset.y / (cellPx + gapPx)).toInt()
                                    val col = ((offset.x - headerPx - gapPx) / (cellPx + gapPx)).toInt()
                                    if (row in 0 until GRID_SIZE && col in 0 until GRID_SIZE) {
                                        currentDragStart?.invoke(row, col)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val row = (change.position.y / (cellPx + gapPx)).toInt().coerceIn(0, GRID_SIZE - 1)
                                    val col = ((change.position.x - headerPx - gapPx) / (cellPx + gapPx)).toInt().coerceIn(0, GRID_SIZE - 1)
                                    currentDragMove?.invoke(row, col)
                                },
                                onDragEnd = { currentDragEnd?.invoke() },
                                onDragCancel = { currentDragEnd?.invoke() },
                            )
                        }
                    } else Modifier,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(GRID_SIZE) { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                // Row header
                                Box(
                                    modifier = Modifier.width(headerSize).height(cellSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${r + 1}", fontSize = 10.sp, color = c.textDim)
                                }
                                repeat(GRID_SIZE) { col ->
                                    val state = board[r][col]
                                    val key = "$r,$col"
                                    val isPreview = key in previewCells
                                    val isLastShot = lastShotKey == key
                                    val isExploding = key in explosionKeys
                                    BoardCell(
                                        state = state,
                                        size = cellSize,
                                        showShip = showShips,
                                        isPreview = isPreview,
                                        previewValid = previewValid,
                                        isLastShot = isLastShot,
                                        isExploding = isExploding,
                                        onClick = if (interactive) {
                                            { onCellClick(r, col) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Board legend
        val s = LocalI18n.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            LegendItem(color = c.cellWater, label = s.boardWater)
            if (showShips) LegendItem(color = c.cellShip, label = s.boardShip)
            LegendItem(color = c.cellHit, label = s.boardHit)
            LegendItem(color = c.cellMiss, label = s.boardMiss)
            LegendItem(color = c.cellSunk, label = s.boardSunk)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(label, fontSize = 9.sp, color = LocalColorPalette.current.textDim)
    }
}

@Composable
private fun BoardCell(
    state: String,
    size: Dp,
    showShip: Boolean,
    isPreview: Boolean,
    previewValid: Boolean,
    isLastShot: Boolean = false,
    isExploding: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val c = LocalColorPalette.current
    val bgColor = when {
        isPreview -> if (previewValid) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
        state == CellState.HIT -> c.cellHit
        state == CellState.MISS -> c.cellMiss
        state == CellState.SUNK -> c.cellSunk
        state == CellState.SAFE -> c.cellSafe
        state == CellState.SHIP && showShip -> c.cellShip
        else -> c.cellWater
    }
    val borderColor = when {
        isPreview -> if (previewValid) c.green else c.red
        state == CellState.SUNK -> c.red.copy(alpha = .6f)
        else -> c.border.copy(alpha = .3f)
    }
    val emoji = when (state) {
        CellState.HIT -> "🔥"
        CellState.MISS -> "·"
        CellState.SUNK -> "💀"
        else -> ""
    }

    // Last-shot pop animation
    val shotScale by animateFloatAsState(
        targetValue = if (isLastShot) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "shotPop",
    )

    // Explosion flash animation
    val explosionAlpha by animateFloatAsState(
        targetValue = if (isExploding) 0.7f else 0f,
        animationSpec = if (isExploding) tween(200) else tween(600),
        label = "explosion",
    )

    val s = LocalI18n.current
    val cellLabel = when (state) {
        CellState.HIT -> s.cellHit
        CellState.MISS -> s.cellMiss
        CellState.SUNK -> s.cellSunk
        CellState.SHIP -> if (showShip) s.cellShip else s.cellWater
        CellState.SAFE -> s.cellSafe
        else -> s.cellWater
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(shotScale)
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(3.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = cellLabel },
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotEmpty()) {
            Text(
                emoji,
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (size * 0.5f).toSp()
                },
                textAlign = TextAlign.Center,
            )
        }
        // Explosion flash overlay
        if (explosionAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = explosionAlpha }
                    .background(Color(0xFFFF6B00)),
            )
        }
    }
}
