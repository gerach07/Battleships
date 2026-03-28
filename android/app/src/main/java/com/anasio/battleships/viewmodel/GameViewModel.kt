package com.anasio.battleships.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anasio.battleships.data.*
import com.anasio.battleships.i18n.Language
import com.anasio.battleships.i18n.Strings
import com.anasio.battleships.i18n.fmt
import com.anasio.battleships.ui.theme.ThemeId
import com.anasio.battleships.util.*
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("battleships", Context.MODE_PRIVATE)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // ── Core state ──
    private val _phase = MutableStateFlow("login")
    val phase: StateFlow<String> = _phase
    private val _gameId = MutableStateFlow("")
    val gameId: StateFlow<String> = _gameId
    private val _roomPassword = MutableStateFlow("")
    val roomPassword: StateFlow<String> = _roomPassword
    private val _playerName = MutableStateFlow("")
    val playerName: StateFlow<String> = _playerName
    private val _loginView = MutableStateFlow("menu")
    val loginView: StateFlow<String> = _loginView
    private val _playerId = MutableStateFlow<String?>(null)
    val playerId: StateFlow<String?> = _playerId
    private val _opponentName = MutableStateFlow("")
    val opponentName: StateFlow<String> = _opponentName
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    private val _messageType = MutableStateFlow("info")
    val messageType: StateFlow<String> = _messageType
    private val _winner = MutableStateFlow<String?>(null)
    val winner: StateFlow<String?> = _winner
    private val _currentTurn = MutableStateFlow<String?>(null)
    val currentTurn: StateFlow<String?> = _currentTurn

    // ── Boards ──
    private val _playerBoard = MutableStateFlow(createEmptyBoard())
    val playerBoard: StateFlow<Board> = _playerBoard
    private val _opponentBoard = MutableStateFlow(createEmptyBoard())
    val opponentBoard: StateFlow<Board> = _opponentBoard

    // ── Placement ──
    private val _shipsPlaced = MutableStateFlow(0)
    val shipsPlaced: StateFlow<Int> = _shipsPlaced
    private val _clientPlacements = MutableStateFlow<List<PlacedShip>>(emptyList())
    val clientPlacements: StateFlow<List<PlacedShip>> = _clientPlacements
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady
    private val _opponentReady = MutableStateFlow(false)
    val opponentReady: StateFlow<Boolean> = _opponentReady
    private val _placementKey = MutableStateFlow(0)
    val placementKey: StateFlow<Int> = _placementKey

    // ── Play Again ──
    private val _playAgainPending = MutableStateFlow(false)
    val playAgainPending: StateFlow<Boolean> = _playAgainPending
    private val _opponentWantsPlayAgain = MutableStateFlow(false)
    val opponentWantsPlayAgain: StateFlow<Boolean> = _opponentWantsPlayAgain

    // ── Host & Kick ──
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost
    private val _opponentSocketId = MutableStateFlow<String?>(null)
    val opponentSocketId: StateFlow<String?> = _opponentSocketId
    private val _showKickDialog = MutableStateFlow(false)
    val showKickDialog: StateFlow<Boolean> = _showKickDialog

    // ── Ship Scoreboard ──
    private val _mySunkCount = MutableStateFlow(0)
    val mySunkCount: StateFlow<Int> = _mySunkCount
    private val _theirSunkCount = MutableStateFlow(0)
    val theirSunkCount: StateFlow<Int> = _theirSunkCount

    // ── Shot / Explosion Animations (per-board) ──
    /** "row,col" key of the most recent shot on opponent's board, auto-clears after 500ms */
    private val _opponentLastShotKey = MutableStateFlow<String?>(null)
    val opponentLastShotKey: StateFlow<String?> = _opponentLastShotKey
    /** "row,col" key of the most recent shot on player's board, auto-clears after 500ms */
    private val _playerLastShotKey = MutableStateFlow<String?>(null)
    val playerLastShotKey: StateFlow<String?> = _playerLastShotKey
    /** Set of "row,col" keys currently showing explosion overlay on opponent's board */
    private val _opponentExplosionKeys = MutableStateFlow<Set<String>>(emptySet())
    val opponentExplosionKeys: StateFlow<Set<String>> = _opponentExplosionKeys
    /** Set of "row,col" keys currently showing explosion overlay on player's board */
    private val _playerExplosionKeys = MutableStateFlow<Set<String>>(emptySet())
    val playerExplosionKeys: StateFlow<Set<String>> = _playerExplosionKeys

    // ── Surrender Confirmation ──
    private val _showSurrenderDialog = MutableStateFlow(false)
    val showSurrenderDialog: StateFlow<Boolean> = _showSurrenderDialog

    // ── Rooms ──
    private val _availableRooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val availableRooms: StateFlow<List<RoomInfo>> = _availableRooms
    private val _loadingRooms = MutableStateFlow(false)
    val loadingRooms: StateFlow<Boolean> = _loadingRooms
    private val _createPassword = MutableStateFlow("")
    val createPassword: StateFlow<String> = _createPassword
    private val _selectedRoom = MutableStateFlow<String?>(null)
    val selectedRoom: StateFlow<String?> = _selectedRoom
    private val _joinRoomCode = MutableStateFlow("")
    val joinRoomCode: StateFlow<String> = _joinRoomCode
    private val _joinRoomPin = MutableStateFlow("")
    val joinRoomPin: StateFlow<String> = _joinRoomPin
    private val _pendingJoin = MutableStateFlow<PendingJoin?>(null)
    val pendingJoin: StateFlow<PendingJoin?> = _pendingJoin

    // ── Features ──
    private val _language = MutableStateFlow(Language.fromCode(prefs.getString("lang", "en") ?: "en"))
    val language: StateFlow<Language> = _language
    private val s get() = Strings.forLanguage(_language.value) // shortcut for current strings

    fun setLanguage(lang: Language) {
        _language.value = lang
        prefs.edit().putString("lang", lang.code).apply()
    }

    private val _themeId = MutableStateFlow(
        try { ThemeId.valueOf(prefs.getString("theme", "CLASSIC") ?: "CLASSIC") }
        catch (_: Exception) { ThemeId.CLASSIC }
    )
    val themeId: StateFlow<ThemeId> = _themeId

    fun setTheme(id: ThemeId) {
        _themeId.value = id
        prefs.edit().putString("theme", id.name).apply()
    }

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound", false))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled
    private val _musicEnabled = MutableStateFlow(prefs.getBoolean("music", false))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    private val _chatOpen = MutableStateFlow(false)
    val chatOpen: StateFlow<Boolean> = _chatOpen
    private val _chatUnread = MutableStateFlow(0)
    val chatUnread: StateFlow<Int> = _chatUnread
    private val _isSpectator = MutableStateFlow(false)
    val isSpectator: StateFlow<Boolean> = _isSpectator
    private val _spectatorCount = MutableStateFlow(0)
    val spectatorCount: StateFlow<Int> = _spectatorCount
    private val _gameTimeLimit = MutableStateFlow(300) // Default 5 mins
    val gameTimeLimit: StateFlow<Int> = _gameTimeLimit
    private val _playerTimeLeft = MutableStateFlow<Map<String, Double>>(emptyMap())
    val playerTimeLeft: StateFlow<Map<String, Double>> = _playerTimeLeft
    private val _turnStartedAt = MutableStateFlow<Long?>(null)
    val turnStartedAt: StateFlow<Long?> = _turnStartedAt
    private val _spectatorBoards = MutableStateFlow<List<SpectatorBoard>>(emptyList())
    val spectatorBoards: StateFlow<List<SpectatorBoard>> = _spectatorBoards

    private val listeners = mutableMapOf<String, Emitter.Listener>()
    // ── Internal ──
    private var playerIdRef: String? = null
    private var playerLeftJob: Job? = null
    private var messageAutoClearJob: Job? = null
    private var lastShotClearJob: Job? = null
    private val shootPending = java.util.concurrent.atomic.AtomicBoolean(false)
    private var joiningGame = false
    private val playerSunk = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val opponentSunk = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    val isConnected: StateFlow<Boolean> = SocketManager.isConnected

    // ════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════

    init {
        val saved = prefs.getString("name", null)
        if (!saved.isNullOrBlank()) _playerName.value = saved
        SoundManager.enabled = _soundEnabled.value
        MusicManager.enabled = _musicEnabled.value
        SocketManager.connect()
        setupSocketListeners()
        
        viewModelScope.launch(Dispatchers.Main) {
            _phase.collectLatest { currentPhase ->
                when (currentPhase) {
                    "login", "waiting" -> MusicManager.playMenuMusic(getApplication())
                    "placement" -> MusicManager.playPlacementMusic(getApplication())
                    "battle" -> MusicManager.playBattleMusic(getApplication())
                    "gameOver" -> {
                        if (_winner.value == playerIdRef) MusicManager.playVictoryMusic(getApplication())
                        else MusicManager.playDefeatMusic(getApplication())
                    }
                    else -> MusicManager.stopMusic()
                }
            }
        }

        // Auto-rejoin after network reconnection
        var wasConnected = SocketManager.isConnected.value
        viewModelScope.launch(Dispatchers.Main) {
            SocketManager.isConnected.collect { connected ->
                if (!connected) joiningGame = false
                if (connected && !wasConnected) {
                    // Socket just reconnected — auto-rejoin if mid-game
                    val phase = _phase.value
                    val gId = _gameId.value
                    val pName = _playerName.value
                    if (phase != "login" && gId.isNotBlank() && pName.isNotBlank()) {
                        setMessage("🔄 ${s.reconnectingToGame}", "info")
                        val data = JSONObject().apply {
                            put("gameId", gId)
                            put("playerName", pName)
                            val pw = _roomPassword.value
                            if (pw.isNotBlank()) put("password", pw)
                            if (_isSpectator.value) put("isSpectator", true)
                        }
                        SocketManager.emit("rejoinGame", data)
                    }
                }
                wasConnected = connected
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageAutoClearJob?.cancel()
        lastShotClearJob?.cancel()
        playerLeftJob?.cancel()
        if (_gameId.value.isNotBlank()) {
            SocketManager.emit("leaveRoom")
        }
        removeSocketListeners()
        SocketManager.disconnect()
    }

    // ════════════════════════════════════════
    // SETTERS (for UI binding)
    // ════════════════════════════════════════

    fun setPlayerName(v: String) { val trimmed = v.take(50); _playerName.value = trimmed; if (trimmed.isNotBlank()) prefs.edit().putString("name", trimmed).apply() }
    fun setLoginView(v: String) { _loginView.value = v }
    fun setCreatePassword(v: String) { _createPassword.value = v }
    fun setJoinRoomCode(v: String) { _joinRoomCode.value = v.uppercase().replace(Regex("[^A-Z0-9]"), "") }
    fun setJoinRoomPin(v: String) { _joinRoomPin.value = v.replace(Regex("\\D"), "").take(3) }
    fun setRoomPassword(v: String) { _roomPassword.value = v.replace(Regex("\\D"), "").take(3) }
    fun setSelectedRoom(v: String?) { _selectedRoom.value = v }
    fun setPendingJoin(v: PendingJoin?) { _pendingJoin.value = v }
    fun setGameTimeLimit(v: Int) { _gameTimeLimit.value = v }
    fun setGameId(v: String) { _gameId.value = v }
    fun setMessage(msg: String, type: String = "info") {
        _message.value = msg; _messageType.value = type
        messageAutoClearJob?.cancel()
        if (msg.isNotBlank()) {
            messageAutoClearJob = viewModelScope.launch {
                kotlinx.coroutines.delay(4000)
                _message.value = ""; _messageType.value = "info"
            }
        }
    }
    fun toggleSound() { _soundEnabled.value = !_soundEnabled.value; SoundManager.enabled = _soundEnabled.value; prefs.edit().putBoolean("sound", _soundEnabled.value).apply() }
    fun toggleMusic() { 
        _musicEnabled.value = !_musicEnabled.value
        MusicManager.enabled = _musicEnabled.value
        prefs.edit().putBoolean("music", _musicEnabled.value).apply() 
    }
    fun toggleChat() { _chatOpen.value = !_chatOpen.value; if (_chatOpen.value) _chatUnread.value = 0 }
    fun forceReconnect() { SocketManager.forceReconnect() }

    // ════════════════════════════════════════
    // ACTIONS
    // ════════════════════════════════════════

    fun fetchRooms() {
        _loadingRooms.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val conn = URL("$SERVER_URL/rooms").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                if (conn.responseCode !in 200..299) {
                    withContext(Dispatchers.Main) { _availableRooms.value = emptyList(); setMessage(s.failedLoadRooms, "error") }
                    return@launch
                }
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val arr = json.getJSONArray("rooms")
                val rooms = (0 until arr.length()).map { i ->
                    val r = arr.getJSONObject(i)
                    RoomInfo(
                        roomId = r.getString("roomId"),
                        hostName = r.optString("hostName", ""),
                        hasPassword = r.optBoolean("hasPassword", false),
                        state = r.optString("state", ""),
                        playerCount = r.optInt("playerCount", 0),
                        spectatorCount = r.optInt("spectatorCount", 0),
                        timeLimit = r.optInt("timeLimit", 300),
                    )
                }
                withContext(Dispatchers.Main) { _availableRooms.value = rooms }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { _availableRooms.value = emptyList(); setMessage(s.failedLoadRooms, "error") }
            } finally {
                conn.disconnect()
                withContext(Dispatchers.Main) { _loadingRooms.value = false }
            }
        }
    }

    fun checkRoomPassword(roomId: String, pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val conn = URL("$SERVER_URL/rooms/$roomId/check-password").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true; conn.connectTimeout = 5000; conn.readTimeout = 5000
                conn.outputStream.write(org.json.JSONObject().put("password", pin).toString().toByteArray())
                val code = conn.responseCode
                val body = (if (code >= 400) conn.errorStream else conn.inputStream)?.bufferedReader()?.readText() ?: ""
                val json = JSONObject(if (body.isEmpty()) "{}" else body)
                withContext(Dispatchers.Main) {
                    if (json.optBoolean("valid", false)) onResult(true, null)
                    else onResult(false, json.optString("error", s.incorrectPin))
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(false, s.failedValidatePin) }
            } finally {
                conn.disconnect()
            }
        }
    }

    fun handleJoinGame() {
        val id = _joinRoomCode.value.trim()
        if (id.isEmpty()) { setMessage(s.roomCodeRequired, "error"); return }
        _pendingJoin.value = PendingJoin(roomId = id, password = _joinRoomPin.value.ifBlank { null })
        _loginView.value = "enterName"
    }

    fun handleFinalJoin() {
        val name = _playerName.value.trim()
        if (name.isBlank()) { setMessage(s.enterNameFirst, "error"); return }
        val pj = _pendingJoin.value ?: return
        if (joiningGame) return // prevent double-submission
        joiningGame = true

        if (SocketManager.isConnected.value != true) {
            joiningGame = false
            setMessage(s.unknownError, "error")
            return
        }

        val finalRoomId = if (pj.isCreating && pj.roomId.isBlank()) generateRoomCode() else pj.roomId
        _gameId.value = finalRoomId
        _roomPassword.value = pj.password ?: ""
        val data = JSONObject().apply {
            put("gameId", finalRoomId)
            put("playerName", name)
            if (pj.password != null) put("password", pj.password)
            put("isCreating", pj.isCreating)
            put("isSpectating", pj.isSpectating)
            put("timeLimit", pj.timeLimit)
        }
        SocketManager.emit("joinGame", data)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sr = java.security.SecureRandom()
        return (1..6).map { chars[sr.nextInt(chars.length)] }.joinToString("")
    }

    fun handleShipPlaced(placements: List<PlacedShip>) {
        _clientPlacements.value = placements
        _shipsPlaced.value = placements.size
    }

    // ── Connectivity-checked emit helpers ──
    private fun emitIfConnected(event: String): Boolean {
        if (!SocketManager.isConnected.value) {
            setMessage(s.connectionLost, "error")
            return false
        }
        SocketManager.emit(event)
        return true
    }

    private fun emitIfConnected(event: String, data: JSONObject): Boolean {
        if (!SocketManager.isConnected.value) {
            setMessage(s.connectionLost, "error")
            return false
        }
        SocketManager.emit(event, data)
        return true
    }

    fun handleFinishPlacement() {
        val ships = JSONArray()
        _clientPlacements.value.forEachIndexed { i, p ->
            ships.put(JSONObject().apply {
                put("row", p.row); put("col", p.col)
                put("length", p.length); put("direction", p.direction)
                put("name", SHIPS.getOrNull(p.shipId)?.name ?: "Ship ${i + 1}")
            })
        }
        emitIfConnected("finishPlacement", JSONObject().put("ships", ships))
    }

    fun handleUnready() { emitIfConnected("unreadyPlacement") }

    fun handleShoot(row: Int, col: Int) {
        if (!shootPending.compareAndSet(false, true)) return // atomic prevent double-fire
        if (_currentTurn.value == playerIdRef) {
            emitIfConnected("shoot", JSONObject().put("row", row).put("col", col))
        } else {
            shootPending.set(false)
        }
    }

    fun handlePlayAgain() {
        _playAgainPending.value = true
        emitIfConnected("requestPlayAgain")
    }

    fun handleDeclinePlayAgain() {
        emitIfConnected("declinePlayAgain")
        _playAgainPending.value = false
        _opponentWantsPlayAgain.value = false
    }

    private fun handleForfeit() { emitIfConnected("forfeit") }

    fun requestForfeit() { _showSurrenderDialog.value = true }
    fun confirmForfeit() { handleForfeit(); _showSurrenderDialog.value = false }
    fun cancelForfeit() { _showSurrenderDialog.value = false }

    fun handleBackToMenu() {
        playerLeftJob?.cancel()
        playerLeftJob = null
        joiningGame = false
        shootPending.set(false)
        emitIfConnected("leaveRoom")
        resetFullGameState()
        _phase.value = "login"; _gameId.value = ""
        _loginView.value = "menu"; _roomPassword.value = ""; _createPassword.value = ""
        _isSpectator.value = false; _spectatorCount.value = 0
        _isHost.value = false; _opponentSocketId.value = null
        _message.value = ""; _messageType.value = "info"
    }

    fun sendChat(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        val impMatch = Regex("^/imp\\s+(.+)", RegexOption.IGNORE_CASE).find(trimmed)
        if (impMatch != null) {
            emitIfConnected("sendChat", JSONObject().put("message", impMatch.groupValues[1]).put("isImportant", true))
        } else {
            emitIfConnected("sendChat", JSONObject().put("message", trimmed))
        }
    }

    fun handleStartGame() {
        if (!_isHost.value) return
        emitIfConnected("hostStartGame")
    }

    fun requestKickPlayer() { _showKickDialog.value = true }
    fun cancelKick() { _showKickDialog.value = false }
    fun confirmKick() {
        _showKickDialog.value = false
        val targetId = _opponentSocketId.value ?: return
        emitIfConnected("kickPlayer", JSONObject().put("targetId", targetId))
    }

    /** Resets battle/placement-related state shared across multiple reset scenarios. */
    private fun resetBattleState() {
        _showSurrenderDialog.value = false
        _showKickDialog.value = false
        shootPending.set(false)
        _winner.value = null
        _currentTurn.value = null
        _playAgainPending.value = false
        _opponentWantsPlayAgain.value = false
        _playerBoard.value = createEmptyBoard()
        _opponentBoard.value = createEmptyBoard()
        _isReady.value = false
        _opponentReady.value = false
        _clientPlacements.value = emptyList()
        _shipsPlaced.value = 0
        _placementKey.value = _placementKey.value + 1
        _turnStartedAt.value = null
        _playerTimeLeft.value = emptyMap()
        _chatOpen.value = false
        _chatUnread.value = 0
        _mySunkCount.value = 0
        _theirSunkCount.value = 0
        resetSunk()
    }

    private fun resetFullGameState() {
        resetBattleState()
        _chatMessages.value = emptyList()
        _opponentName.value = ""
        _opponentSocketId.value = null
        _isHost.value = false
        _spectatorBoards.value = emptyList()
    }

    private fun resetSunk() { playerSunk.clear(); opponentSunk.clear() }

    private fun localizeTs(data: JSONObject, ts: Long): Long {
        val sn = data.optLong("serverNow", 0L).takeIf { it > 0 } ?: return ts
        return System.currentTimeMillis() - (sn - ts)
    }

    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════
    // SOCKET LISTENERS
    // ════════════════════════════════════════


    private fun reg(event: String, handler: (Array<Any>) -> Unit) {
        val listener = Emitter.Listener { args -> viewModelScope.launch(Dispatchers.Main) { handler(args) } }
        listeners[event] = listener
        SocketManager.on(event, listener)
    }

    private fun removeSocketListeners() {
        listeners.forEach { (event, listener) -> SocketManager.off(event, listener) }
        listeners.clear()
    }

    private fun setupSocketListeners() {

        reg("gameJoined") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            joiningGame = false
            try {
                playerLeftJob?.cancel()
                playerLeftJob = null
                val pid = data.getString("playerId")
                playerIdRef = pid
                _playerId.value = pid
                _gameId.value = data.optString("roomId", "")
                _roomPassword.value = data.optString("password", "")
                _shipsPlaced.value = 0; _clientPlacements.value = emptyList(); resetSunk()
                _isReady.value = false; _opponentReady.value = false
                _mySunkCount.value = 0; _theirSunkCount.value = 0
                _playerBoard.value = data.optJSONArray("board")?.let { parseBoardFromJson(it) } ?: createEmptyBoard()
                data.optInt("timeLimit", 0).takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
                _isSpectator.value = false
                _isHost.value = data.optBoolean("isHost", false)

                // Restore chat history from server
                val chatHistory = data.optJSONArray("chatHistory")
                if (chatHistory != null && chatHistory.length() > 0) {
                    _chatMessages.value = (0 until chatHistory.length()).mapNotNull { i ->
                        val m = chatHistory.optJSONObject(i) ?: return@mapNotNull null
                        ChatMessage(
                            id = m.optString("id", java.util.UUID.randomUUID().toString()),
                            senderId = m.optString("senderId", ""),
                            senderName = m.optString("senderName", ""),
                            text = m.optString("text", ""),
                            timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                            isMine = m.optString("senderId") == pid,
                            isImportant = m.optBoolean("isImportant", false),
                        )
                    }.takeLast(200)
                } else {
                    _chatMessages.value = emptyList()
                }
                _chatUnread.value = 0

                val players = data.optJSONArray("players")
                if (players != null && players.length() == 2) {
                    for (i in 0 until players.length()) {
                        val p = players.optJSONObject(i) ?: continue
                        if (p.optString("id") != pid) {
                            _opponentName.value = p.optString("name", s.opponent)
                            _opponentSocketId.value = p.optString("id").ifEmpty { null }
                        }
                    }
                    val state = data.optString("state", "")
                    if (state == "PLACEMENT_PHASE") {
                        _phase.value = "placement"
                        setMessage(s.bothPlayersIn, "success")
                    } else {
                        // Both players in waiting room, wait for host to start
                        _phase.value = "waiting"
                        setMessage(s.opponentJoined, "success")
                    }
                } else {
                    _opponentName.value = ""
                    _opponentSocketId.value = null
                    _phase.value = "waiting"; _message.value = ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setMessage(s.errorJoining, "error")
            }
        }

        reg("playerJoined") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                val players = data.optJSONArray("players")
                if (players != null) {
                    for (i in 0 until players.length()) {
                        val p = players.optJSONObject(i) ?: continue
                        if (p.optString("id") != playerIdRef) {
                            _opponentName.value = p.optString("name", s.opponent)
                            _opponentSocketId.value = p.optString("id").ifEmpty { null }
                        }
                    }
                }
                val state = data.optString("state", "")
                if (state == "PLACEMENT_PHASE") {
                    // Server already in placement (host started before we got the event)
                    _phase.value = "placement"; _isReady.value = false; _opponentReady.value = false
                    setMessage(s.opponentJoined, "success")
                } else {
                    // Stay in waiting room — host will start the game
                    setMessage(s.opponentJoined, "success")
                }
                // Update host status if provided
                data.optString("hostId", "").takeIf { it.isNotEmpty() }?.let { hostId ->
                    _isHost.value = hostId == playerIdRef
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("error") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            joiningGame = false
            val err = data.optString("error", s.unknownError)
            setMessage("❌ $err", "error")
            if (err.contains("does not exist") || err.contains("Incorrect password") || err.contains("not found")) {
                _loginView.value = "join"; _gameId.value = ""; _roomPassword.value = ""
                _pendingJoin.value = null
            }
        }

        reg("placementFinished") { _ ->
            _isReady.value = true
            setMessage(s.waitingOpponentPlace, "info")
        }

        reg("playerReady") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            if (data.optString("playerId") != playerIdRef) _opponentReady.value = true
        }

        reg("placementUnreadied") { _ ->
            _isReady.value = false; _message.value = ""; _messageType.value = "info"
        }

        reg("playerUnreadied") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            if (data.optString("playerId") != playerIdRef) _opponentReady.value = false
        }

        reg("battleStarted") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                _phase.value = "battle"
                _currentTurn.value = data.optString("currentTurn").ifEmpty { null }
                data.optJSONArray("playerBoard")?.let { _playerBoard.value = parseBoardFromJson(it) }
                _opponentBoard.value = createEmptyBoard(); resetSunk()
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }
                data.optInt("timeLimit", 0).takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
                // Play turn notification if it's our turn first (mirrors web client)
                if (SoundManager.enabled && _currentTurn.value == playerIdRef) SoundManager.playTurn()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("shotResult") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            shootPending.set(false) // allow next shot
            try {
                val shooterId = data.optString("shooterId", "")
                val iShot = shooterId == playerIdRef
                _currentTurn.value = data.optString("currentTurn").ifEmpty { null }
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }

                val isHit = data.optBoolean("isHit", false)
                val shipSunk = data.optBoolean("shipSunk", false)
                val gameWon = data.optBoolean("gameWon", false)

                if (SoundManager.enabled) {
                    when {
                        shipSunk -> SoundManager.playSunk()
                        isHit -> SoundManager.playHit()
                        else -> SoundManager.playMiss()
                    }
                    // Play turn notification when it becomes our turn (opponent missed), mirrors web
                    if (!iShot && !isHit && _currentTurn.value == playerIdRef) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            SoundManager.playTurn()
                        }
                    }
                }
                if (isHit && !iShot) vibrate(200)
                if (shipSunk && !iShot) vibratePattern(longArrayOf(0, 100, 50, 200))

                // Last-shot pop animation (auto-clears after 500ms) — route to correct board
                val shotRow = data.optInt("row", -1)
                val shotCol = data.optInt("col", -1)
                if (shotRow >= 0 && shotCol >= 0) {
                    lastShotClearJob?.cancel()
                    val shotState = if (iShot) _opponentLastShotKey else _playerLastShotKey
                    shotState.value = "$shotRow,$shotCol"
                    lastShotClearJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(500)
                        shotState.value = null
                    }
                }

                if (shipSunk && data.has("sunkShipCells")) {
                    // Track sunk counts for scoreboard
                    if (iShot) _mySunkCount.value = _mySunkCount.value + 1
                    else _theirSunkCount.value = _theirSunkCount.value + 1

                    val cellsArr = data.optJSONArray("sunkShipCells")
                    if (cellsArr != null) {
                        val cells = (0 until cellsArr.length()).mapNotNull { i ->
                            val c = cellsArr.optJSONObject(i) ?: return@mapNotNull null
                            c.optInt("row", -1) to c.optInt("col", -1)
                        }.filter { it.first >= 0 && it.second >= 0 }
                        val ref = if (iShot) opponentSunk else playerSunk
                        cells.forEach { ref.add("${it.first},${it.second}") }
                        getSurroundingKeys(cells).forEach { ref.add("${it}_safe") }

                        // Explosion overlay animation (auto-clears after 1.5s) — route to correct board
                        val expKeys = cells.map { "${it.first},${it.second}" }.toSet()
                        val expState = if (iShot) _opponentExplosionKeys else _playerExplosionKeys
                        expState.value = expState.value + expKeys
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1500)
                            expState.value = expState.value - expKeys
                        }
                    }
                }

                val rawP = data.optJSONArray("playerBoard")?.let { parseBoardFromJson(it) } ?: createEmptyBoard()
                val rawO = data.optJSONArray("opponentBoard")?.let { parseBoardFromJson(it) } ?: createEmptyBoard()
                _playerBoard.value = overlayBoard(rawP, playerSunk)
                _opponentBoard.value = overlayBoard(rawO, opponentSunk)

                val shipName = data.optString("sunkShipName", "ship")
                val msg = when {
                    shipSunk && iShot -> s.sunkTheirShip.fmt(shipName)
                    shipSunk -> s.yourShipSunk.fmt(shipName)
                    isHit && iShot -> s.hitShootAgain
                    isHit -> s.theyHitYourShip
                    iShot -> s.missOpponentTurn
                    else -> s.theyMissedYourTurn
                }
                _message.value = msg
                _messageType.value =
                    when {
                        isHit && iShot -> "success"
                        isHit -> "error"
                        iShot -> "info"
                        else -> "success"
                    }

                if (gameWon) {
                    _phase.value = "gameOver"
                    _winner.value = data.optString("winner").ifEmpty { null }
                    if (SoundManager.enabled) {
                        if (data.optString("winner") == playerIdRef) SoundManager.playVictory() else SoundManager.playDefeat()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("gameReset") { args ->
            val data = args.getOrNull(0) as? JSONObject
            resetBattleState()
            _phase.value = "placement"
            data?.optInt("timeLimit", 0)?.takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
            setMessage(s.newGamePlaceShips, "info")
        }

        reg("playerLeft") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            setMessage(s.playerLeftGame.fmt(data.optString("playerName", s.opponent)), "info")
            resetFullGameState()
            playerLeftJob?.cancel()
            playerLeftJob = viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _phase.value = "login"; _gameId.value = ""; resetSunk()
            }
        }

        reg("opponentLeft") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            resetBattleState()
            _opponentName.value = ""
            _opponentSocketId.value = null
            if (data.has("isHost")) _isHost.value = data.optBoolean("isHost", false)
            setMessage(s.playerLeftWaiting.fmt(data.optString("playerName", s.opponent)), "info")
            _phase.value = "waiting"
        }

        reg("leftRoom") { _ ->
            _chatMessages.value = emptyList()
            _phase.value = "login"; _gameId.value = ""; resetSunk()
            _message.value = ""; _messageType.value = "info"
        }

        reg("gameForfeited") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            val iForfeited = data.optString("forfeiterId") == playerIdRef
            _winner.value = data.optString("winner").ifEmpty { null }  // set winner BEFORE phase so observer plays correct music
            _phase.value = "gameOver"
            if (iForfeited) SoundManager.playDefeat() else SoundManager.playVictory()
            _message.value = if (iForfeited) s.youSurrendered else s.opponentSurrendered.fmt(data.optString("forfeiterName", s.opponent))
            _messageType.value = if (iForfeited) "info" else "success"
        }

        reg("playAgainRequested") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (_isSpectator.value) {
                val name = data?.optString("requesterName", "")?.takeIf { it.isNotEmpty() } ?: s.opponent
                setMessage("🎮 $name wants a rematch!", "info")
            } else { _opponentWantsPlayAgain.value = true }
        }

        reg("playAgainDeclined") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (_isSpectator.value) {
                val name = data?.optString("declinerName", "")?.takeIf { it.isNotEmpty() } ?: s.opponent
                setMessage("❌ $name declined the rematch", "error")
            } else {
                _playAgainPending.value = false; _opponentWantsPlayAgain.value = false
                setMessage(s.opponentDeclinedRematch, "error")
            }
        }

        reg("gameStartedByHost") { _ ->
            _phase.value = "placement"
            _isReady.value = false
            _opponentReady.value = false
            setMessage(s.gameStartedByHost, "success")
        }

        reg("kicked") { args ->
            val data = args.getOrNull(0) as? JSONObject
            val msg = data?.optString("message", "")?.takeIf { it.isNotEmpty() } ?: s.youWereKicked
            resetFullGameState()
            setMessage("❌ $msg", "error")
            _phase.value = "login"
            _loginView.value = "menu"
            _gameId.value = ""; _roomPassword.value = ""
            _isSpectator.value = false; _spectatorCount.value = 0
        }

        reg("playerKicked") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            if (data.has("targetId")) {
                _opponentName.value = ""
                _opponentSocketId.value = null
                setMessage(s.playerKicked, "info")
            }
        }

        // ── Reconnection handlers ──
        reg("rejoinSuccess") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                shootPending.set(false)
                val pid = data.optString("playerId").ifEmpty { null }
                if (pid != null) { _playerId.value = pid; playerIdRef = pid }
                _isHost.value = data.optBoolean("isHost", false)
                data.optString("roomId").ifEmpty { null }?.let { _gameId.value = it }
                data.optJSONArray("board")?.let { _playerBoard.value = parseBoardFromJson(it) }
                data.optJSONArray("opponentBoard")?.let { _opponentBoard.value = parseBoardFromJson(it) }
                data.optString("currentTurn").ifEmpty { null }?.let { _currentTurn.value = it }
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }
                data.optInt("timeLimit", 0).takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
                _winner.value = data.optString("winner").ifEmpty { null }
                _opponentName.value = data.optString("opponentName", "").ifEmpty { "" }
                if (data.optBoolean("shipsPlaced", false)) _isReady.value = true

                // Restore chat history
                data.optJSONArray("chatHistory")?.let { arr ->
                    val msgs = mutableListOf<ChatMessage>()
                    for (i in 0 until arr.length()) {
                        val m = arr.optJSONObject(i) ?: continue
                        msgs += ChatMessage(
                            id = m.optString("id", "").ifEmpty { java.util.UUID.randomUUID().toString() },
                            senderId = m.optString("senderId", ""),
                            senderName = m.optString("senderName", ""),
                            text = m.optString("text", ""),
                            timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                            isMine = m.optString("senderId") == pid,
                            isImportant = m.optBoolean("isImportant", false),
                        )
                    }
                    _chatMessages.value = msgs.takeLast(200)
                }

                // Restore phase from server state
                when (data.optString("state")) {
                    "BATTLE_PHASE" -> _phase.value = "battle"
                    "PLACEMENT_PHASE" -> _phase.value = "placement"
                    "GAME_OVER" -> _phase.value = "gameOver"
                    "WAITING_FOR_PLAYERS" -> _phase.value = "waiting"
                }
                // Clear play-again state on rejoin
                _playAgainPending.value = false
                _opponentWantsPlayAgain.value = false
                joiningGame = false
                setMessage("✅ ${s.reconnected}", "success")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("rejoinFailed") { args ->
            val data = args.getOrNull(0) as? JSONObject
            val reason = data?.optString("reason", "")?.ifEmpty { null } ?: s.couldNotRejoin
            setMessage("❌ $reason", "error")
            _phase.value = "login"
            _gameId.value = ""
            _roomPassword.value = ""
            _isSpectator.value = false
        }

        reg("opponentReconnecting") { args ->
            val data = args.getOrNull(0) as? JSONObject
            val name = data?.optString("playerName", "")?.ifEmpty { null } ?: _opponentName.value
            setMessage("⏳ ${s.opponentLostConnection.replace("{0}", name)}", "info")
        }

        reg("opponentReconnected") { args ->
            val data = args.getOrNull(0) as? JSONObject
            data?.optString("playerId", "")?.ifEmpty { null }?.let { _opponentSocketId.value = it }
            val name = data?.optString("playerName", "")?.ifEmpty { null } ?: _opponentName.value
            setMessage("✅ ${s.opponentReconnected.replace("{0}", name)}", "success")
        }

        reg("opponentReconnectFailed") { args ->
            val data = args.getOrNull(0) as? JSONObject
            val name = data?.optString("playerName", "")?.ifEmpty { null } ?: _opponentName.value
            _playAgainPending.value = false
            _opponentWantsPlayAgain.value = false
            setMessage("❌ ${s.opponentDisconnected.replace("{0}", name)}", "error")
        }

        reg("roomClosed") { args ->
            val data = args.getOrNull(0) as? JSONObject
            val reason = data?.optString("reason", "")?.ifEmpty { null } ?: "Room was closed"
            setMessage("⚠\uFE0F $reason", "error")
            _phase.value = "login"
            _gameId.value = ""
            _isSpectator.value = false
        }

        reg("chatMessage") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                val isMine = data.optString("senderId") == playerIdRef
                val isImportant = data.optBoolean("isImportant", false)
                val msg = ChatMessage(
                    id = data.optString("id", "").ifEmpty { java.util.UUID.randomUUID().toString() },
                    senderId = data.optString("senderId", ""),
                    senderName = data.optString("senderName", ""),
                    text = data.optString("text", ""),
                    timestamp = data.optLong("timestamp", System.currentTimeMillis()),
                    isMine = isMine,
                    isImportant = isImportant,
                )
                _chatMessages.value = (_chatMessages.value + msg).takeLast(200)
                // Show important messages as banner notification
                if (isImportant) {
                    setMessage("📢 ${msg.senderName}: ${msg.text}", "info")
                }
                if (!isMine && !_chatOpen.value) {
                    _chatUnread.value = _chatUnread.value + 1
                    if (SoundManager.enabled) SoundManager.playChat()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("spectatorJoined") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                joiningGame = false
                _isSpectator.value = true
                data.optString("roomId").ifEmpty { null }?.let { _gameId.value = it }
                parseSpectatorBoards(data.optJSONArray("boards"))
                data.optJSONArray("chatHistory")?.let { arr ->
                    if (arr.length() > 0) {
                        _chatMessages.value = (0 until arr.length()).mapNotNull { i ->
                            val m = arr.optJSONObject(i) ?: return@mapNotNull null
                            ChatMessage(
                                id = m.optString("id", "").ifEmpty { java.util.UUID.randomUUID().toString() },
                                senderId = m.optString("senderId", ""),
                                senderName = m.optString("senderName", ""),
                                text = m.optString("text", ""),
                                timestamp = m.optLong("timestamp", 0L),
                                isMine = false,
                                isImportant = m.optBoolean("isImportant", false),
                            )
                        }
                    }
                }
                data.optInt("timeLimit", 0).takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }
                _currentTurn.value = data.optString("currentTurn").ifEmpty { null }
                when (data.optString("state")) {
                    "BATTLE_PHASE" -> _phase.value = "battle"
                    "PLACEMENT_PHASE" -> _phase.value = "placement"
                    "GAME_OVER" -> _phase.value = "gameOver"
                }
                val players = data.optJSONArray("players")
                val names =
                    if (players != null) {
                        (0 until players.length())
                            .map { players.optJSONObject(it)?.optString("name", "?") ?: "?" }
                            .joinToString(" vs ")
                    } else ""
                setMessage(s.spectatingNames.fmt(names), "info")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("spectatorShotResult") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                parseSpectatorBoards(data.optJSONArray("boards"))
                _currentTurn.value = data.optString("currentTurn").ifEmpty { null }
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }
                if (data.optBoolean("gameWon", false)) {
                    _phase.value = "gameOver"
                    _winner.value = data.optString("winner").ifEmpty { null }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("spectatorBattleStarted") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            try {
                _phase.value = "battle"
                parseSpectatorBoards(data.optJSONArray("boards"))
                _currentTurn.value = data.optString("currentTurn").ifEmpty { null }
                data.optJSONObject("playerTimeLeft")?.let { _playerTimeLeft.value = parseTimeLeft(it) }
                data.optLong("turnStartedAt", 0L).takeIf { it > 0 }?.let { _turnStartedAt.value = localizeTs(data, it) }
                data.optInt("timeLimit", 0).takeIf { it > 0 }?.let { _gameTimeLimit.value = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reg("spectatorUpdate") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            _spectatorCount.value = data.optInt("count", 0)
        }

        reg("timeUp") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@reg
            _phase.value = "gameOver"
            _winner.value = data.optString("winner").ifEmpty { null }
            val iLost = data.optString("loser") == playerIdRef
            _message.value = if (iLost) s.yourClockRanOut else s.opponentClockRanOut
            _messageType.value = if (iLost) "error" else "success"
            if (SoundManager.enabled) {
                if (data.optString("winner") == playerIdRef) SoundManager.playVictory() else SoundManager.playDefeat()
            }
        }
    }

    private fun parseSpectatorBoards(arr: JSONArray?) {
        if (arr == null) return
        _spectatorBoards.value = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            SpectatorBoard(
                playerId = b.optString("playerId", ""),
                playerName = b.optString("playerName", ""),
                board = b.optJSONArray("board")?.let { parseBoardFromJson(it) } ?: createEmptyBoard(),
            )
        }
    }
}
