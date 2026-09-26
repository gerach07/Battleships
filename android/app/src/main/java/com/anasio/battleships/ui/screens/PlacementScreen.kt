package com.anasio.battleships.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.data.CellState
import com.anasio.battleships.data.GRID_SIZE
import com.anasio.battleships.data.PlacedShip
import com.anasio.battleships.data.SHIPS
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.components.GameBoard
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.ui.components.bounceClick
import com.anasio.battleships.util.canPlaceShipOnBoard
import com.anasio.battleships.util.SoundManager
import com.anasio.battleships.util.createEmptyBoard
import com.anasio.battleships.util.generateRandomPlacement
import com.anasio.battleships.viewmodel.GameViewModel

@Composable
fun PlacementScreen(viewModel: GameViewModel) {
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val isReady by viewModel.isReady.collectAsState()
    val opponentReady by viewModel.opponentReady.collectAsState()
    val placementKey by viewModel.placementKey.collectAsState()
    val isSpectator by viewModel.isSpectator.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    // Local placement state (reset when placementKey changes, restored from ViewModel after config change)
    val savedPlacements by viewModel.clientPlacements.collectAsState()
    var board by remember(placementKey) {
        val initialPlacements = viewModel.clientPlacements.value
        val initialBoard = if (initialPlacements.isNotEmpty()) {
            val b = createEmptyBoard().toMutableList().map { it.toMutableList() }
            initialPlacements.forEach { p -> p.cells.forEach { (cr, cc) -> b[cr][cc] = CellState.SHIP } }
            b.map { it.toList() }
        } else createEmptyBoard()
        mutableStateOf(initialBoard)
    }
    var placements by remember(placementKey) { mutableStateOf(viewModel.clientPlacements.value) }
    var selectedShip by remember(placementKey) {
        val placedIds = viewModel.clientPlacements.value.map { it.shipId }.toSet()
        val next = SHIPS.firstOrNull { sh -> sh.id !in placedIds }
        mutableIntStateOf(next?.id ?: 0)
    }
    var direction by remember(placementKey) { mutableStateOf("horizontal") }

    // Drag-to-move state
    var dragShipId by remember { mutableStateOf<Int?>(null) }
    var dragDirection by remember { mutableStateOf("horizontal") }
    var dragOffsetInShip by remember { mutableIntStateOf(0) }
    var dragOriginalPlacement by remember { mutableStateOf<PlacedShip?>(null) }
    var dragTargetRow by remember { mutableIntStateOf(0) }
    var dragTargetCol by remember { mutableIntStateOf(0) }

    // Drag preview cells & validity
    val dragPreviewCells = remember(dragShipId, dragTargetRow, dragTargetCol, board, dragDirection) {
        val id = dragShipId ?: return@remember emptySet<String>()
        val ship = SHIPS.find { it.id == id } ?: return@remember emptySet<String>()
        val (valid, cells) = canPlaceShipOnBoard(board, dragTargetRow, dragTargetCol, ship.length, dragDirection)
        if (valid) {
            cells.map { "${it.first},${it.second}" }.toSet()
        } else {
            // Show raw outline even for invalid positions
            (0 until ship.length).mapNotNull { i ->
                val r = if (dragDirection == "horizontal") dragTargetRow else dragTargetRow + i
                val c = if (dragDirection == "horizontal") dragTargetCol + i else dragTargetCol
                if (r in 0 until GRID_SIZE && c in 0 until GRID_SIZE) "$r,$c" else null
            }.toSet()
        }
    }
    val dragPreviewValid = remember(dragShipId, dragTargetRow, dragTargetCol, board, dragDirection) {
        val id = dragShipId ?: return@remember true
        val ship = SHIPS.find { it.id == id } ?: return@remember false
        canPlaceShipOnBoard(board, dragTargetRow, dragTargetCol, ship.length, dragDirection).first
    }

    val placedIds = savedPlacements.map { it.shipId }.toSet()
    val allPlaced = placedIds.size == SHIPS.size

    if (isSpectator) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface.copy(alpha = 0.72f))
                    .border(1.dp, c.border.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("🚢", fontSize = 48.sp)
                Text(s.spectating, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(s.playersPlacingShips, fontSize = 14.sp, color = c.textDim, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(c.primary.copy(alpha = 0.8f))
                        )
                    }
                }
            }
            TextButton(
                onClick = viewModel::handleBackToMenu,
                modifier = Modifier.padding(top = 12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = c.red),
            ) {
                Text("← ${s.leave}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    1.dp,
                    if (isReady) c.green.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SHIPS.forEach { ship ->
                    val placed = ship.id in placedIds
                    val selected = ship.id == selectedShip && ship.id !in placedIds && !isReady
                    val shipName = when (ship.name) {
                        "Carrier" -> s.shipCarrier
                        "Battleship" -> s.shipBattleship
                        "Destroyer" -> s.shipDestroyer
                        "Submarine" -> s.shipSubmarine
                        "Patrol" -> s.shipPatrol
                        else -> ship.name
                    }
                    val shape = RoundedCornerShape(9.dp)
                    val tileColor = when {
                        placed -> c.green.copy(alpha = 0.65f)
                        selected -> Color(0xFFAF52DE).copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.08f)
                    }
                    val tileBorder = when {
                        placed -> c.green.copy(alpha = 0.85f)
                        selected -> c.yellow
                        else -> c.border.copy(alpha = 0.45f)
                    }
                    val clickModifier = if (!isReady && !placed) {
                        Modifier.bounceClick { selectedShip = ship.id }
                    } else Modifier

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .clip(shape)
                            .background(tileColor)
                            .border(if (selected) 2.dp else 1.dp, tileBorder, shape)
                            .then(clickModifier)
                            .padding(horizontal = 3.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(if (placed) "✅" else ship.emoji, fontSize = 17.sp)
                        Text(
                            shipName,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Text("${ship.length}×", fontSize = 8.sp, color = c.textDim)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(c.card.copy(alpha = 0.75f))
                        .padding(3.dp),
                ) {
                    DirectionButton(
                        label = s.horiz,
                        selected = direction == "horizontal",
                        enabled = !isReady,
                    ) { direction = "horizontal" }
                    DirectionButton(
                        label = s.vert,
                        selected = direction == "vertical",
                        enabled = !isReady,
                    ) { direction = "vertical" }
                }
                Spacer(Modifier.weight(1f))
                RandomPlacementButton(enabled = !isReady) {
                    val result = generateRandomPlacement()
                    if (result != null) {
                        board = result.first
                        placements = result.second
                        viewModel.handleShipPlaced(result.second)
                        SoundManager.playPlace()
                    }
                }
                Spacer(Modifier.width(6.dp))
                SmallBtn("🗑️", false, !isReady && placements.isNotEmpty()) {
                    board = createEmptyBoard()
                    placements = emptyList()
                    viewModel.handleShipPlaced(emptyList())
                    selectedShip = 0
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${s.shipsCount}: ${placements.size}/${SHIPS.size}",
                        fontSize = 11.sp,
                        color = c.textDim,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${SHIPS.size - placements.size} ${s.shipsRemaining}",
                        fontSize = 11.sp,
                        color = c.textDim,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(placements.size.toFloat() / SHIPS.size)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(c.green, c.green.copy(alpha = 0.7f)))),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            if (isReady) s.lockedHint else s.placementHint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            fontSize = 11.sp,
            color = if (isReady) c.green else c.textDim,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface.copy(alpha = 0.48f))
                .border(
                    1.dp,
                    if (isReady) c.green.copy(alpha = 0.3f) else c.border.copy(alpha = 0.25f),
                    RoundedCornerShape(12.dp),
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameBoard(
            board = board,
            showShips = true,
            showCombatLegend = false,
            interactive = !isReady,
            previewCells = if (dragShipId != null) dragPreviewCells else emptySet(),
            previewValid = if (dragShipId != null) dragPreviewValid else true,
            onCellClick = { r, c ->
                if (isReady) return@GameBoard

                // If tapping on an existing ship, remove it
                val existing = placements.find { p -> (r to c) in p.cells }
                if (existing != null) {
                    val newBoard = createEmptyBoard().toMutableList().map { it.toMutableList() }
                    val newPlacements = placements.filter { it.shipId != existing.shipId }
                    newPlacements.forEach { p -> p.cells.forEach { (cr, cc) -> newBoard[cr][cc] = CellState.SHIP } }
                    board = newBoard.map { it.toList() }
                    placements = newPlacements
                    viewModel.handleShipPlaced(newPlacements)
                    selectedShip = existing.shipId
                    return@GameBoard
                }

                // Place selected ship
                val ship = SHIPS.find { it.id == selectedShip && it.id !in placedIds } ?: return@GameBoard
                val (valid, cells) = canPlaceShipOnBoard(board, r, c, ship.length, direction)
                if (!valid) {
                    viewModel.setMessage("❌ ${s.cantPlaceThere}", "error")
                    return@GameBoard
                }
                val newBoard = board.map { it.toMutableList() }
                cells.forEach { (cr, cc) -> newBoard[cr][cc] = CellState.SHIP }
                board = newBoard.map { it.toList() }

                val placed = PlacedShip(ship.id, r, c, ship.length, direction, cells)
                val newPlacements = placements + placed
                placements = newPlacements
                viewModel.handleShipPlaced(newPlacements)
                SoundManager.playPlace()

                // Auto-select next unplaced ship
                val next = SHIPS.firstOrNull { s -> s.id !in newPlacements.map { it.shipId }.toSet() }
                if (next != null) selectedShip = next.id
            },
            // Drag-to-move callbacks
            onDragStart = if (!isReady) { { r, c ->
                val existing = placements.find { p -> (r to c) in p.cells }
                val ship = existing?.let { e -> SHIPS.find { it.id == e.shipId } }
                if (existing != null && ship != null) {
                    val offsetInShip = if (existing.direction == "horizontal") c - existing.col else r - existing.row

                    // Remove ship temporarily
                    val newPlacements = placements.filter { it.shipId != existing.shipId }
                    val newBoard = createEmptyBoard().toMutableList().map { it.toMutableList() }
                    newPlacements.forEach { p -> p.cells.forEach { (cr, cc) -> newBoard[cr][cc] = CellState.SHIP } }
                    board = newBoard.map { it.toList() }
                    placements = newPlacements
                    viewModel.handleShipPlaced(newPlacements)

                    dragShipId = existing.shipId
                    dragDirection = existing.direction
                    dragOffsetInShip = offsetInShip
                    dragOriginalPlacement = existing
                    dragTargetRow = existing.row
                    dragTargetCol = existing.col
                    selectedShip = existing.shipId
                }
            } } else null,
            onDragMove = if (!isReady && dragShipId != null) { { r, c ->
                val targetR = if (dragDirection == "horizontal") r else r - dragOffsetInShip
                val targetC = if (dragDirection == "horizontal") c - dragOffsetInShip else c
                dragTargetRow = targetR
                dragTargetCol = targetC
            } } else null,
            onDragEnd = if (!isReady && dragShipId != null) { {
                val id = dragShipId
                val ship = id?.let { sid -> SHIPS.find { it.id == sid } }
                val orig = dragOriginalPlacement

                if (id != null && ship != null && orig != null) {
                    val (valid, cells) = canPlaceShipOnBoard(board, dragTargetRow, dragTargetCol, ship.length, dragDirection)
                    if (valid) {
                        val newBoard = board.map { it.toMutableList() }
                        cells.forEach { (cr, cc) -> newBoard[cr][cc] = CellState.SHIP }
                        board = newBoard.map { it.toList() }
                        val newPlacements = placements + PlacedShip(id, dragTargetRow, dragTargetCol, ship.length, dragDirection, cells)
                        placements = newPlacements
                        viewModel.handleShipPlaced(newPlacements)
                        viewModel.setMessage("✅ ${s.shipMoved}", "success")
                        SoundManager.playPlace()
                    } else {
                        // Restore to original position — this should always succeed since we just vacated it
                        val (origValid, origCells) = canPlaceShipOnBoard(board, orig.row, orig.col, ship.length, orig.direction)
                        val newBoard = board.map { it.toMutableList() }
                        if (origValid) {
                            origCells.forEach { (cr, cc) -> newBoard[cr][cc] = CellState.SHIP }
                            board = newBoard.map { it.toList() }
                            placements = placements + orig
                        } else {
                            // Fallback: force-restore using the original cells
                            orig.cells.forEach { (cr, cc) ->
                                if (cr in 0 until GRID_SIZE && cc in 0 until GRID_SIZE) newBoard[cr][cc] = CellState.SHIP
                            }
                            board = newBoard.map { it.toList() }
                            placements = placements + orig
                        }
                        viewModel.handleShipPlaced(placements)
                        viewModel.setMessage("❌ ${s.cantPlaceThere}", "error")
                    }
                }
                dragShipId = null
                dragOriginalPlacement = null
            } } else null,
            )
        }
        Spacer(Modifier.height(4.dp))

        if (opponentName.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (opponentReady) c.green else c.orange)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (opponentReady) s.opponentIsReady.replace("{0}", opponentName)
                    else s.opponentPlacing.replace("{0}", opponentName),
                    fontSize = 12.sp,
                    color = if (opponentReady) c.green else c.orange,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Ready / Unready
        if (isReady) {
            Button(
                onClick = {
                    viewModel.handleUnready()
                    // Re-enable editing is handled by server event
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.orange),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text("↩ ${s.notReady}", fontWeight = FontWeight.Bold) }
        } else {
            Button(
                onClick = { viewModel.handleFinishPlacement() },
                enabled = allPlaced,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (allPlaced) c.green else c.card),
            ) {
                Text("✅ ${s.ready}", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::handleBackToMenu,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.red),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("← ${s.leave}") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RandomPlacementButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val background = Brush.horizontalGradient(listOf(Color(0xFFFF8A00), Color(0xFFFFD54F)))
    val clickModifier = if (enabled) Modifier.bounceClick(onClick) else Modifier
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 52.dp, minHeight = 44.dp)
            .clip(shape)
            .background(background)
            .then(clickModifier)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text("🎲", fontSize = 19.sp, color = Color.White)
    }
}

@Composable
private fun DirectionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalColorPalette.current
    val shape = RoundedCornerShape(7.dp)
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(36.dp),
        shape = shape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) c.primary else Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = c.textDim,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallBtn(text: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalColorPalette.current
    val bg = if (active) c.primary.copy(alpha = .25f) else c.card
    val border = if (active) c.primary else c.border.copy(alpha = .3f)
    val shape = RoundedCornerShape(16.dp)
    val clickModifier = if (enabled) Modifier.bounceClick(onClick) else Modifier
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (enabled) Color.White else c.textDim,
        )
    }
}
