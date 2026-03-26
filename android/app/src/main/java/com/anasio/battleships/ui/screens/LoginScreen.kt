package com.anasio.battleships.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.data.PendingJoin
import com.anasio.battleships.data.RoomInfo
import com.anasio.battleships.i18n.Language
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.ui.theme.ThemeId
import com.anasio.battleships.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(viewModel: GameViewModel) {
    val loginView by viewModel.loginView.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()

    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val language by viewModel.language.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    Box(Modifier.fillMaxSize()) {
        // Floating bubbles background
        FloatingBubblesOverlay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Language selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            listOf("🇬🇧" to Language.EN, "🇱🇻" to Language.LV, "🇷🇺" to Language.RU).forEach { (flag, lang) ->
                Text(
                    flag,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .then(
                            if (language == lang)
                                Modifier
                                    .border(2.dp, c.primary, RoundedCornerShape(6.dp))
                                    .padding(4.dp)
                            else Modifier.padding(4.dp)
                        )
                        .clickable { viewModel.setLanguage(lang) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("⚓", fontSize = 48.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            s.battleships.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.White, letterSpacing = 3.sp, maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.width(32.dp).height(1.dp).background(c.border.copy(alpha = .4f)))
            Spacer(Modifier.width(8.dp))
            Text(s.multiplayerNavalCombat, fontSize = 13.sp, color = c.textDim)
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.width(32.dp).height(1.dp).background(c.border.copy(alpha = .4f)))
        }
        Text(
            "Created by Adrians Bergmanis",
            fontSize = 10.sp,
            color = c.textDim.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = viewModel::toggleSound) {
                Text(if (soundEnabled) "🔊" else "🔇", fontSize = 22.sp)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = viewModel::toggleMusic) {
                Text(if (musicEnabled) "🎵" else "🔕", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Message
        if (message.isNotBlank()) {
            MessageBanner(message, messageType)
            Spacer(Modifier.height(8.dp))
        }

        when (loginView) {
            "menu" -> MenuView(viewModel)
            "create" -> CreateView(viewModel)
            "join" -> JoinView(viewModel)
            "enterName" -> EnterNameView(viewModel)
        }
    }
    } // Box
}

// ── Floating Bubbles ──
private data class BubbleSpec(val sizeDp: Dp, val xFrac: Float, val durationMs: Int, val delayMs: Int, val color: Color)

@Composable
private fun FloatingBubblesOverlay() {
    val specs = remember {
        val palette = listOf(
            Color(0xFF3B82F6).copy(alpha = 0.10f),
            Color(0xFF8B5CF6).copy(alpha = 0.10f),
            Color(0xFF10B981).copy(alpha = 0.08f),
            Color(0xFFEC4899).copy(alpha = 0.08f),
            Color(0xFFFBBF24).copy(alpha = 0.06f),
        )
        List(14) { i ->
            BubbleSpec(
                sizeDp = (20 + (Math.random() * 50)).dp,
                xFrac = Math.random().toFloat(),
                durationMs = (12000 + (Math.random() * 18000)).toInt(),
                delayMs = (Math.random() * 20000).toInt(),
                color = palette[i % palette.size],
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")

    Box(modifier = Modifier.fillMaxSize()) {
        specs.forEach { spec ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = spec.durationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(spec.delayMs),
                ),
                label = "bubble",
            )

            // fade in/out at edges
            val alpha = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.9f -> (1f - progress) / 0.1f
                else -> 1f
            }
            // moves from bottom (1.2) to top (-0.2)
            val yFrac = 1.2f - progress * 1.4f
            // slight scale-down near top
            val scale = 1f - 0.4f * progress

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val x = spec.xFrac * maxWidth.value
                val y = yFrac * maxHeight.value
                Box(
                    Modifier
                        .offset(x = x.dp, y = y.dp)
                        .size(spec.sizeDp * scale)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(spec.color)
                )
            }
        }
    }
}

// ── Menu ──
@Composable
private fun MenuView(viewModel: GameViewModel) {
    val s = LocalI18n.current
    val c = LocalColorPalette.current
    val themeId by viewModel.themeId.collectAsState()
    var serverInfo by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var showServerInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("${com.anasio.battleships.data.SERVER_URL}/version")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                try {
                    if (conn.responseCode == 200) {
                        val json = conn.inputStream.bufferedReader().use { it.readText() }
                        val obj = org.json.JSONObject(json)
                        serverInfo = mapOf(
                            "version" to obj.optString("version", "?"),
                            "node" to obj.optString("node", "?"),
                            "uptime" to obj.optInt("uptime", 0),
                            "activeRooms" to obj.optInt("activeRooms", 0),
                            "connectedSockets" to obj.optInt("connectedSockets", 0),
                            "memoryMB" to obj.optDouble("memoryMB", 0.0),
                            "platform" to obj.optString("platform", "?"),
                        )
                    }
                } finally { conn.disconnect() }
            } catch (_: Exception) {}
        }
    }

    // Theme selector
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
    ) {
        ThemeId.entries.forEach { theme ->
            val selected = theme == themeId
            Text(
                theme.icon,
                fontSize = 24.sp,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) c.primary.copy(alpha = .2f) else Color.Transparent)
                    .border(
                        if (selected) 2.dp else 1.dp,
                        if (selected) c.primary else c.border.copy(alpha = .3f),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { viewModel.setTheme(theme) }
                    .padding(8.dp),
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    GradientButton("🎮  ${s.createGame}", c.primaryDark, c.primary) {
        viewModel.setLoginView("create")
    }
    Spacer(Modifier.height(12.dp))
    GradientButton("🚀  ${s.joinGame}", c.accent, Color(0xFF7C3AED)) {
        viewModel.setLoginView("join"); viewModel.fetchRooms()
    }

    Spacer(Modifier.height(16.dp))

    if (serverInfo.isNotEmpty()) {
        val upSec = (serverInfo["uptime"] as? Int) ?: 0
        val uptimeStr = if (upSec >= 86400) "${upSec / 86400}d ${(upSec % 86400) / 3600}h"
                        else "${upSec / 3600}h ${(upSec % 3600) / 60}m"
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showServerInfo = !showServerInfo }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (showServerInfo) "▼ Server Info" else "▶ Server Info",
                fontSize = 11.sp,
                color = c.textDim.copy(alpha = 0.5f)
            )
            AnimatedVisibility(visible = showServerInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.surface.copy(alpha = 0.5f))
                        .border(1.dp, c.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Server v${serverInfo["version"]} \u2022 Node ${serverInfo["node"]}", fontSize = 11.sp, color = c.textDim)
                    Text("Uptime: $uptimeStr \u2022 Memory: ${serverInfo["memoryMB"]}MB", fontSize = 11.sp, color = c.textDim)
                    Text("Rooms: ${serverInfo["activeRooms"]} \u2022 Players: ${serverInfo["connectedSockets"]}", fontSize = 11.sp, color = c.textDim)
                    Text("Platform: ${serverInfo["platform"]}", fontSize = 11.sp, color = c.textDim)
                }
            }
        }
    }
}

// ── Create ──
@Composable
private fun CreateView(viewModel: GameViewModel) {
    val createPassword by viewModel.createPassword.collectAsState()
    val gameTimeLimit by viewModel.gameTimeLimit.collectAsState()
    var timeSlider by remember { mutableFloatStateOf(gameTimeLimit / 60f) }
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    SectionCard {
        Text(s.createNewGame, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.height(12.dp))

        Text(s.roomPinOptional, fontSize = 12.sp, color = c.textDim)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = createPassword,
            onValueChange = viewModel::setCreatePassword,
            placeholder = { Text(s.threedigitPin) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = tfColors(),
        )
        Spacer(Modifier.height(16.dp))

        Text("${s.timePerPlayer}: ${timeSlider.toInt()} min", fontSize = 13.sp, color = c.textPrimary)
        Slider(
            value = timeSlider,
            onValueChange = { timeSlider = it; viewModel.setGameTimeLimit((it * 60).toInt()) },
            valueRange = 2f..10f,
            steps = 7,  // (10 - 2) - 1
            colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary),
        )
        Spacer(Modifier.height(12.dp))

        GradientButton(s.createRoom, c.primaryDark, c.primary) {
            viewModel.setPendingJoin(
                PendingJoin(
                    roomId = "",
                    password = createPassword.ifBlank { null },
                    isCreating = true,
                    timeLimit = (timeSlider * 60).toInt(),
                )
            )
            viewModel.setLoginView("enterName")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { viewModel.setLoginView("menu") }) {
            Text("← ${s.back}", color = c.textDim)
        }
    }
}

// ── Join ──
@Composable
private fun JoinView(viewModel: GameViewModel) {
    val joinRoomCode by viewModel.joinRoomCode.collectAsState()
    val joinRoomPin by viewModel.joinRoomPin.collectAsState()
    val rooms by viewModel.availableRooms.collectAsState()
    val loadingRooms by viewModel.loadingRooms.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    SectionCard {
        Text(s.joinGameTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.height(12.dp))

        Text(s.roomCode, fontSize = 12.sp, color = c.textDim)
        OutlinedTextField(
            value = joinRoomCode,
            onValueChange = viewModel::setJoinRoomCode,
            placeholder = { Text(s.roomCodePlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = tfColors(),
        )
        Spacer(Modifier.height(8.dp))
        Text(s.pinIfRequired, fontSize = 12.sp, color = c.textDim)
        OutlinedTextField(
            value = joinRoomPin,
            onValueChange = viewModel::setJoinRoomPin,
            placeholder = { Text(s.threedigitPin) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = tfColors(),
        )
        Spacer(Modifier.height(12.dp))

        GradientButton(s.joinRoom, c.accent, Color(0xFF7C3AED)) {
            viewModel.handleJoinGame()
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.border.copy(alpha = .4f))
        Spacer(Modifier.height(8.dp))

        // Room list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.availableRooms, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            IconButton(onClick = viewModel::fetchRooms, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, s.refresh, tint = c.primary, modifier = Modifier.size(18.dp))
            }
        }

        when {
            loadingRooms -> {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp), color = c.primary)
            }
            rooms.isEmpty() -> {
                Text(s.noRoomsAvailable, fontSize = 13.sp, color = c.textDim, modifier = Modifier.padding(16.dp))
            }
            else -> {
                Column(modifier = Modifier.heightIn(max = 250.dp)) {
                    rooms.forEach { room ->
                        RoomItem(room, room.roomId == selectedRoom, viewModel)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { viewModel.setLoginView("menu"); viewModel.setSelectedRoom(null) }) {
            Text("← ${s.back}", color = c.textDim)
        }
    }
}

@Composable
private fun RoomItem(room: RoomInfo, isSelected: Boolean, viewModel: GameViewModel) {
    val s = LocalI18n.current
    val c = LocalColorPalette.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) c.card else c.surface)
            .border(
                1.dp,
                if (isSelected) c.primary.copy(alpha = .5f) else c.border.copy(alpha = .3f),
                RoundedCornerShape(8.dp),
            )
            .clickable { viewModel.setSelectedRoom(if (isSelected) null else room.roomId) }
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(room.roomId, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.primary)
                Text(
                    "${room.hostName} · ${room.playerCount}/2 · ${room.timeLimit / 60} ${s.min}" +
                            if (room.hasPassword) " 🔒" else "",
                    fontSize = 11.sp, color = c.textDim,
                )
            }
            val stateLabel = when (room.state) {
                "WAITING_FOR_PLAYERS" -> s.waiting
                "PLACEMENT_PHASE" -> s.placing
                "BATTLE_PHASE" -> s.battle
                else -> room.state
            }
            Text(stateLabel, fontSize = 10.sp, color = c.emerald)
        }

        AnimatedVisibility(visible = isSelected) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (room.hasPassword) {
                    Text(s.enterPin, fontSize = 11.sp, color = c.textDim)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(3); pinError = null },
                        placeholder = { Text(s.pinPlaceholder) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = c.red) } },
                        colors = tfColors(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Join as player
                    if (room.playerCount < 2) {
                        Button(
                            onClick = {
                                if (room.hasPassword) {
                                    checking = true
                                    viewModel.checkRoomPassword(room.roomId, pinInput) { valid, err ->
                                        checking = false
                                        if (valid) {
                                            viewModel.setJoinRoomCode(room.roomId)
                                            viewModel.setJoinRoomPin(pinInput)
                                            viewModel.setPendingJoin(PendingJoin(roomId = room.roomId, password = pinInput))
                                            viewModel.setLoginView("enterName")
                                        } else pinError = err ?: s.incorrectPin
                                    }
                                } else {
                                    viewModel.setPendingJoin(PendingJoin(roomId = room.roomId))
                                    viewModel.setLoginView("enterName")
                                }
                            },
                            enabled = !checking && (!room.hasPassword || pinInput.length == 3),
                            colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                            modifier = Modifier.height(36.dp),
                        ) { Text(s.join, fontSize = 12.sp) }
                    }
                    // Spectate
                    OutlinedButton(
                        onClick = {
                            if (room.hasPassword) {
                                checking = true
                                viewModel.checkRoomPassword(room.roomId, pinInput) { valid, err ->
                                    checking = false
                                    if (valid) {
                                        viewModel.setPendingJoin(PendingJoin(roomId = room.roomId, password = pinInput, isSpectating = true))
                                        viewModel.setLoginView("enterName")
                                    } else pinError = err ?: s.incorrectPin
                                }
                            } else {
                                viewModel.setPendingJoin(PendingJoin(roomId = room.roomId, isSpectating = true))
                                viewModel.setLoginView("enterName")
                            }
                        },
                        enabled = !checking && (!room.hasPassword || pinInput.length == 3),
                        modifier = Modifier.height(36.dp),
                    ) { Text("👁 ${s.spectate}", fontSize = 12.sp) }
                }
            }
        }
    }
}

// ── Enter Name ──
@Composable
private fun EnterNameView(viewModel: GameViewModel) {
    val playerName by viewModel.playerName.collectAsState()
    val pending by viewModel.pendingJoin.collectAsState()
    val isCreating = pending?.isCreating == true
    val isSpectating = pending?.isSpectating == true
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    SectionCard {
        Text(
            when {
                isSpectating -> s.spectatorName
                isCreating -> s.enterYourName
                else -> s.enterYourName
            },
            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = playerName,
            onValueChange = viewModel::setPlayerName,
            placeholder = { Text(s.yourNamePlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { viewModel.handleFinalJoin() }),
            colors = tfColors(),
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = when {
                isSpectating -> "👁 ${s.watchGame}"
                isCreating -> "🎮 ${s.createAndJoin}"
                else -> "🚀 ${s.joinGameBtn}"
            },
            start = if (isCreating) c.primaryDark else c.accent,
            end = if (isCreating) c.primary else Color(0xFF7C3AED),
        ) { viewModel.handleFinalJoin() }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            viewModel.setPendingJoin(null)
            viewModel.setLoginView(if (isCreating) "create" else "join")
        }) { Text("← ${s.back}", color = c.textDim) }
    }
}

// ── Helpers ──

@Composable
fun MessageBanner(message: String, type: String) {
    val c = LocalColorPalette.current
    val bg = when (type) {
        "success" -> c.green.copy(alpha = .15f)
        "error" -> c.red.copy(alpha = .15f)
        else -> c.primary.copy(alpha = .15f)
    }
    val border = when (type) {
        "success" -> c.green.copy(alpha = .4f)
        "error" -> c.red.copy(alpha = .4f)
        else -> c.primary.copy(alpha = .4f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(message, fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun GradientButton(text: String, start: Color, end: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(start, end)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalColorPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface.copy(alpha = .7f)),
        border = BorderStroke(1.dp, c.border.copy(alpha = .3f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun tfColors(): TextFieldColors {
    val c = LocalColorPalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.border,
        cursorColor = c.primary,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )
}
