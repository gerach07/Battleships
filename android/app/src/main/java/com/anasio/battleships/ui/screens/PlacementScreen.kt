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
        val initialBoard = if (savedPlacements.isNotEmpty()) {
            val b = createEmptyBoard().toMutableList().map { it.toMutableList() }
            savedPlacements.forEach { p -> p.cells.forEach { (cr, cc) -> b[cr][cc] = CellState.SHIP } }
            b.map { it.toList() }
        } else createEmptyBoard()
        mutableStateOf(initialBoard)
    }
    var placements by remember(placementKey) { mutableStateOf(savedPlacements) }
    var selectedShip by remember(placementKey) {
        val placedIds = savedPlacements.map { it.shipId }.toSet()
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

    val allPlaced = placements.size == SHIPS.size
    val placedIds = placements.map { it.shipId }.toSet()

    if (isSpectator) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("👁 ${s.spectating}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(s.playersPlacingShips, fontSize = 14.sp, color = c.textDim)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚓ ${s.placeYourShips}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Row {
                IconButton(onClick = viewModel::toggleSound, modifier = Modifier.size(36.dp)) {
                    Text(if (soundEnabled) "🔊" else "🔇", fontSize = 18.sp)
                }
                IconButton(onClick = viewModel::toggleMusic, modifier = Modifier.size(36.dp)) {
                    Text(if (musicEnabled) "🎵" else "🔕", fontSize = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        if (message.isNotBlank()) {
            MessageBanner(message, messageType)
            Spacer(Modifier.height(4.dp))
        }

        // Ship selector
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SHIPS.forEach { ship ->
                val placed = ship.id in placedIds
                val selected = ship.id == selectedShip && !isReady
                val bg = when {
                    placed -> c.green.copy(alpha = .15f)
                    selected -> c.primary.copy(alpha = .2f)
                    else -> c.card
                }
                val border = when {
                    placed -> c.green.copy(alpha = .4f)
                    selected -> c.primary
                    else -> c.border.copy(alpha = .3f)
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isReady && !placed) { selectedShip = ship.id }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val shipName = when (ship.name) {
                        "Carrier" -> s.shipCarrier
                        "Battleship" -> s.shipBattleship
                        "Destroyer" -> s.shipDestroyer
                        "Submarine" -> s.shipSubmarine
                        "Patrol" -> s.shipPatrol
                        else -> ship.name
                    }
                    Text(ship.emoji, fontSize = 18.sp)
                    Text(shipName, fontSize = 10.sp, color = if (placed) c.green else Color.White)
                    Text("(${ship.length})", fontSize = 9.sp, color = c.textDim)
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Direction + actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallBtn(
                    "↔ ${s.horiz}",
                    direction == "horizontal" && !isReady,
                    !isReady
                ) { direction = "horizontal" }
                SmallBtn(
                    "↕ ${s.vert}",
                    direction == "vertical" && !isReady,
                    !isReady,
                ) { direction = "vertical" }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallBtn("🎲", false, !isReady) {
                    val result = generateRandomPlacement()
                    if (result != null) {
                        board = result.first; placements = result.second
                        viewModel.handleShipPlaced(result.second)
                        SoundManager.playPlace()
                    }
                }
                SmallBtn("🗑", false, !isReady && placements.isNotEmpty()) {
                    board = createEmptyBoard(); placements = emptyList()
                    viewModel.handleShipPlaced(emptyList())
                    selectedShip = 0
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // Board
        GameBoard(
            board = board,
            showShips = true,
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
        Spacer(Modifier.height(8.dp))

        // Status (always rendered to prevent layout shift)
        Text(
            when {
                opponentReady -> "✅ ${s.opponentIsReady}"
                opponentName.isNotEmpty() -> s.opponentPlacing.replace("{0}", opponentName)
                else -> "\u00A0"
            },
            fontSize = 13.sp,
            color = if (opponentReady) c.green else c.textDim,
            modifier = Modifier.alpha(if (opponentReady || opponentName.isNotEmpty()) 1f else 0f),
        )
        Spacer(Modifier.height(4.dp))
        Text("${s.shipsCount}: ${placements.size}/${SHIPS.size}", fontSize = 12.sp, color = c.textDim)
        Spacer(Modifier.height(8.dp))

        // Ready / Unready
        if (isReady) {
            // Locked-in hint
            Text(
                s.lockedHint,
                fontSize = 10.sp, color = c.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
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
                Text(
                    if (allPlaced) "✅ ${s.ready} ${placements.size}/${SHIPS.size}" else s.placeAllFirst,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::handleBackToMenu,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.red),
        ) { Text("← ${s.leave}") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SmallBtn(text: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalColorPalette.current
    val bg = if (active) c.primary.copy(alpha = .25f) else c.card
    val border = if (active) c.primary else c.border.copy(alpha = .3f)
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (enabled) Color.White else c.textDim,
        )
    }
}
