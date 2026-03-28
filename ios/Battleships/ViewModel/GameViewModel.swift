import Foundation
import Combine
import UIKit

final class GameViewModel: ObservableObject {
    // MARK: - Phase & Login
    @Published var phase = "login" {
        didSet {
            guard oldValue != phase else { return }
            switch phase {
            case "login", "waiting": MusicManager.shared.playMenuMusic()
            case "placement":       MusicManager.shared.playPlacementMusic()
            case "battle":          MusicManager.shared.playBattleMusic()
            case "gameOver":
                if winner == playerIdRef { MusicManager.shared.playVictoryMusic() }
                else { MusicManager.shared.playDefeatMusic() }
            default: break
            }
        }
    }
    @Published var loginView = "menu"  // menu, create, join, enterPin, enterName
    @Published var playerName: String = UserDefaults.standard.string(forKey: "battleships-name") ?? ""
    @Published var gameId = ""
    @Published var roomPassword = ""
    @Published var createPassword = ""
    @Published var gameTimeLimit: Int = 300
    @Published var isHost = false
    @Published var isSpectator = false
    @Published var spectatorCount = 0
    @Published var availableRooms: [RoomInfo] = []

    // MARK: - Opponent
    @Published var opponentName = ""
    @Published var opponentReady = false
    private var opponentSocketId: String?

    // MARK: - Boards
    @Published var playerBoard: Board = createEmptyBoard()
    @Published var opponentBoard: Board = createEmptyBoard()

    // MARK: - Placement
    @Published var clientPlacements: [PlacedShip] = []
    @Published var isReady = false
    @Published var shipsPlaced = 0
    @Published var selectedShipId: Int? = nil
    @Published var placementDirection = "horizontal"
    @Published var placementKey = 0

    // MARK: - Battle
    @Published var currentTurn: String?
    @Published var winner: String?
    @Published var mySunkCount = 0
    @Published var theirSunkCount = 0
    @Published var shotKeys: Set<String> = []         // for opponent board
    @Published var explosionKeys: Set<String> = []     // for opponent board
    @Published var playerShotKeys: Set<String> = []    // for player board
    @Published var playerExplosionKeys: Set<String> = [] // for player board

    // MARK: - Timer
    @Published var turnStartedAt: Double?
    @Published var playerTimeLeft: [String: Double] = [:]

    // MARK: - Play Again
    @Published var playAgainPending = false
    @Published var opponentWantsPlayAgain = false

    // MARK: - Chat
    @Published var chatMessages: [ChatMessage] = []
    @Published var chatOpen = false
    @Published var chatUnread = 0

    // MARK: - UI
    @Published var message = ""
    @Published var messageType = "info"
    @Published var showSurrenderDialog = false
    @Published var showKickDialog = false

    // MARK: - Settings
    @Published var language: Language {
        didSet { UserDefaults.standard.set(language.rawValue, forKey: "battleships-lang") }
    }
    @Published var soundEnabled: Bool = true {
        didSet {
            SoundManager.shared.enabled = soundEnabled
            UserDefaults.standard.set(soundEnabled, forKey: "battleships-sound")
        }
    }
    @Published var musicEnabled: Bool = true {
        didSet {
            MusicManager.shared.enabled = musicEnabled
            UserDefaults.standard.set(musicEnabled, forKey: "battleships-music")
        }
    }

    // MARK: - Spectator boards
    @Published var spectatorBoards: [SpectatorBoard] = []
    /// Per-player accumulated sunk-cell keys (used to overlay 💀 on the rendered board)
    private var spectatorSunkDict: [String: Set<String>] = [:]

    // MARK: - Connection
    let socketManager = GameSocketManager.shared

    // MARK: - Private
    private var playerId = ""
    private(set) var playerIdRef = ""
    private var joiningGame = false
    private var shootPending = false
    private var shootTimeoutWork: DispatchWorkItem?
    private var hasConnectedOnce = false
    private var playerLeftJob: DispatchWorkItem?
    private var messageTimer: DispatchWorkItem?
    private var opponentSunkOverlay: Set<String> = []
    private var playerSunkOverlay: Set<String> = []
    private var cancellables = Set<AnyCancellable>()

    var s: I18nStrings { stringsFor(language) }
    var isMyTurn: Bool { currentTurn == playerIdRef }

    init() {
        let langCode = UserDefaults.standard.string(forKey: "battleships-lang") ?? "en"
        language = Language(rawValue: langCode) ?? .en
        soundEnabled = UserDefaults.standard.object(forKey: "battleships-sound") as? Bool ?? true
        SoundManager.shared.enabled = soundEnabled
        musicEnabled = UserDefaults.standard.object(forKey: "battleships-music") as? Bool ?? true
        MusicManager.shared.enabled = musicEnabled
        // Delay to allow AVAudioSession to be ready before first playback
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard self != nil else { return }
            MusicManager.shared.playMenuMusic()
        }

        socketManager.connect()
        setupSocketListeners()

        socketManager.$isConnected
            .receive(on: DispatchQueue.main)
            .sink { [weak self] connected in
                guard let self else { return }
                if connected && self.hasConnectedOnce && !self.gameId.isEmpty {
                    self.setMessage("🔄 \(self.s.reconnectingToGame)", "info")
                    var payload: [String: Any] = [
                        "gameId": self.gameId,
                        "playerName": self.playerName,
                        "password": self.roomPassword,
                    ]
                    if self.isSpectator { payload["isSpectator"] = true }
                    self.socketManager.emit("rejoinGame", payload)
                }
                if connected { self.hasConnectedOnce = true }
            }
            .store(in: &cancellables)
    }

    // MARK: - Lifecycle
    func handleSceneActive() {
        if musicEnabled { MusicManager.shared.resumeMusic() }
        socketManager.forceReconnect()
        // Spectators have no grace period on the server — proactively re-register
        // so we get fresh board state even when the socket never fully dropped.
        if isSpectator && !gameId.isEmpty && socketManager.isConnected {
            socketManager.emit("rejoinGame", [
                "gameId": gameId,
                "playerName": playerName,
                "password": roomPassword,
                "isSpectator": true,
            ])
        }
    }

    func handleSceneBackground() {
        MusicManager.shared.pauseMusic()
    }

    deinit {
        playerLeftJob?.cancel()
        socketManager.disconnect()
    }

    // MARK: - Message helpers
    func setMessage(_ msg: String, _ type: String, duration: TimeInterval = 4) {
        DispatchQueue.main.async {
            self.message = msg
            self.messageType = type
        }
        messageTimer?.cancel()
        if duration > 0 {
            let work = DispatchWorkItem { [weak self] in
                DispatchQueue.main.async { self?.message = "" }
            }
            messageTimer = work
            DispatchQueue.main.asyncAfter(deadline: .now() + duration, execute: work)
        }
    }

    // MARK: - Room fetching
    func fetchRooms() {
        guard let url = URL(string: "\(SERVER_URL)/rooms") else { return }
        URLSession.shared.dataTask(with: url) { [weak self] data, _, error in
            guard let self, let data, error == nil else {
                DispatchQueue.main.async { self?.setMessage(self?.s.failedLoadRooms ?? "", "error") }
                return
            }
            guard let wrapper = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let json = wrapper["rooms"] as? [[String: Any]] else { return }
            let rooms = json.compactMap { dict -> RoomInfo? in
                guard let id = dict["roomId"] as? String else { return nil }
                return RoomInfo(
                    id: id,
                    hostName: dict["hostName"] as? String ?? "",
                    hasPassword: dict["hasPassword"] as? Bool ?? false,
                    state: dict["state"] as? String ?? "",
                    playerCount: dict["playerCount"] as? Int ?? 0,
                    spectatorCount: dict["spectatorCount"] as? Int ?? 0,
                    timeLimit: dict["timeLimit"] as? Int ?? 300
                )
            }
            DispatchQueue.main.async { self.availableRooms = rooms }
        }.resume()
    }

    // MARK: - Join / Create
    func joinGame(roomId: String, password: String?, name: String, isCreating: Bool, isSpectating: Bool, timeLimit: Int) {
        guard !joiningGame else { return }
        let trimmedName = String(name.trimmingCharacters(in: .whitespaces).prefix(50))
        guard !trimmedName.isEmpty else { setMessage(s.enterNameFirst, "error"); return }

        joiningGame = true
        playerName = trimmedName
        UserDefaults.standard.set(trimmedName, forKey: "battleships-name")

        var payload: [String: Any] = ["playerName": trimmedName]
        payload["gameId"] = roomId
        payload["isCreating"] = isCreating
        payload["isSpectating"] = isSpectating
        if !password.isNilOrEmpty { payload["password"] = password }
        if isCreating { payload["timeLimit"] = timeLimit }

        socketManager.emit("joinGame", payload)
    }

    // MARK: - Placement
    func placeShip(shipId: Int, row: Int, col: Int) {
        guard let ship = SHIPS.first(where: { $0.id == shipId }) else { return }
        let (valid, cells) = canPlaceShipOnBoard(board: playerBoard, row: row, col: col, length: ship.length, dir: placementDirection)
        guard valid else {
            setMessage(s.cantPlaceThere, "error", duration: 2)
            return
        }

        var board = playerBoard
        // Remove previous placement of this ship
        if let existing = clientPlacements.first(where: { $0.shipId == shipId }) {
            for (r, c) in existing.cells { board[r][c] = CellState.WATER }
            clientPlacements.removeAll { $0.shipId == shipId }
        }
        // Place the ship
        for (r, c) in cells { board[r][c] = CellState.SHIP }
        playerBoard = board
        clientPlacements.append(PlacedShip(shipId: shipId, row: row, col: col, length: ship.length, direction: placementDirection, cells: cells))
        shipsPlaced = clientPlacements.count
        SoundManager.shared.playPlace()
    }

    func randomPlacement() {
        guard let (board, placements) = generateRandomPlacement() else { return }
        playerBoard = board
        clientPlacements = placements
        shipsPlaced = placements.count
        SoundManager.shared.playPlace()
    }

    func confirmPlacement() {
        guard clientPlacements.count == SHIPS.count else {
            setMessage(s.placeAllFirst, "error", duration: 2)
            return
        }
        let shipsPayload = clientPlacements.map { p -> [String: Any] in
            ["shipId": p.shipId, "row": p.row, "col": p.col, "length": p.length, "direction": p.direction]
        }
        socketManager.emit("finishPlacement", ["ships": shipsPayload])
        isReady = true
    }

    func unreadyPlacement() {
        socketManager.emit("unreadyPlacement")
        isReady = false
    }

    // MARK: - Battle
    func shoot(row: Int, col: Int) {
        guard phase == "battle", isMyTurn, !shootPending, !isSpectator else { return }
        let cell = opponentBoard[row][col]
        guard cell == CellState.WATER || cell == CellState.SAFE else { return }
        shootPending = true
        socketManager.emit("shoot", ["row": row, "col": col])
        // Auto-reset if server never responds (e.g. connection lost mid-shot)
        shootTimeoutWork?.cancel()
        let work = DispatchWorkItem { [weak self] in self?.shootPending = false }
        shootTimeoutWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 10, execute: work)
    }

    // MARK: - Play Again
    func handlePlayAgain() {
        playAgainPending = true
        socketManager.emit("requestPlayAgain")
    }

    func handleDeclinePlayAgain() {
        socketManager.emit("declinePlayAgain")
        playAgainPending = false
        opponentWantsPlayAgain = false
    }

    // MARK: - Forfeit
    func requestForfeit() { showSurrenderDialog = true }
    func confirmForfeit() {
        socketManager.emit("forfeit")
        showSurrenderDialog = false
    }
    func cancelForfeit() { showSurrenderDialog = false }

    // MARK: - Host actions
    func handleStartGame() {
        guard isHost else { return }
        socketManager.emit("hostStartGame")
    }

    func requestKickPlayer() { showKickDialog = true }
    func cancelKick() { showKickDialog = false }
    func confirmKick() {
        showKickDialog = false
        guard let targetId = opponentSocketId else { return }
        socketManager.emit("kickPlayer", ["targetId": targetId])
    }

    // MARK: - Chat
    func toggleChat() {
        chatOpen.toggle()
        if chatOpen { chatUnread = 0 }
    }

    func sendChat(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        let trimmed = String(text.trimmingCharacters(in: .whitespaces).prefix(MAX_CHAT_LENGTH))
        let impRegex = try? NSRegularExpression(pattern: "^/imp\\s+(.+)", options: .caseInsensitive)
        if let match = impRegex?.firstMatch(in: trimmed, range: NSRange(trimmed.startIndex..., in: trimmed)),
           let range = Range(match.range(at: 1), in: trimmed) {
            socketManager.emit("sendChat", ["message": String(trimmed[range]), "isImportant": true])
        } else {
            socketManager.emit("sendChat", ["message": trimmed])
        }
    }

    // MARK: - Navigation
    func handleBackToMenu() {
        playerLeftJob?.cancel()
        playerLeftJob = nil
        joiningGame = false
        shootPending = false
        socketManager.emit("leaveRoom")
        resetFullState()
        phase = "login"
        gameId = ""
        loginView = "menu"
        roomPassword = ""
        createPassword = ""
        isSpectator = false
        spectatorCount = 0
        isHost = false
        opponentSocketId = nil
        message = ""
        messageType = "info"
    }

    func forceReconnect() {
        socketManager.forceReconnect()
    }

    // MARK: - Helpers
    private func localizeTs(_ data: [String: Any], _ ts: Double) -> Double {
        guard let sn = data["serverNow"] as? Double, sn > 0 else { return ts }
        return Date().timeIntervalSince1970 * 1000.0 - (sn - ts)
    }

    // MARK: - Reset
    private func resetBattleState() {
        showSurrenderDialog = false
        showKickDialog = false
        shootPending = false
        winner = nil
        currentTurn = nil
        playAgainPending = false
        opponentWantsPlayAgain = false
        playerBoard = createEmptyBoard()
        opponentBoard = createEmptyBoard()
        isReady = false
        opponentReady = false
        clientPlacements = []
        shipsPlaced = 0
        placementKey += 1
        turnStartedAt = nil
        playerTimeLeft = [:]
        chatOpen = false
        chatUnread = 0
        mySunkCount = 0
        theirSunkCount = 0
        opponentSunkOverlay = []
        playerSunkOverlay = []
        shotKeys = []
        explosionKeys = []
        playerShotKeys = []
        playerExplosionKeys = []
    }

    private func resetFullState() {
        resetBattleState()
        chatMessages = []
        opponentName = ""
        spectatorBoards = []
        spectatorSunkDict = [:]
    }

    /// Build SpectatorBoard list from server board data, applying any accumulated sunk overlay.
    private func buildSpectatorBoards(_ boards: [[String: Any]]) -> [SpectatorBoard] {
        boards.compactMap { b -> SpectatorBoard? in
            guard let pid = b["playerId"] as? String,
                  let name = b["playerName"] as? String,
                  let boardData = b["board"] as? [[Any]] else { return nil }
            let rawBoard = parseBoardFromJson(boardData)
            let sunkSet = spectatorSunkDict[pid] ?? []
            let board = sunkSet.isEmpty ? rawBoard : overlayBoard(rawBoard, sunkSet: sunkSet)
            return SpectatorBoard(playerId: pid, playerName: name, board: board)
        }
    }

    /// Parse sunkShipData (from spectatorJoined) and populate spectatorSunkDict.
    private func applySpectatorSunkDataFromJoin(_ sunkData: [[String: Any]]) {
        for pd in sunkData {
            guard let pid = pd["playerId"] as? String,
                  let ships = pd["sunkShips"] as? [[String: Any]] else { continue }
            var sunkSet = Set<String>()
            for ship in ships {
                guard let cells = ship["cells"] as? [[String: Any]] else { continue }
                let tuples = cells.compactMap { c -> (Int, Int)? in
                    guard let r = c["row"] as? Int, let col = c["col"] as? Int else { return nil }
                    return (r, col)
                }
                tuples.forEach { sunkSet.insert("\($0.0),\($0.1)") }
                getSurroundingKeys(shipCells: tuples).forEach { sunkSet.insert($0 + "_safe") }
            }
            if !sunkSet.isEmpty { spectatorSunkDict[pid] = sunkSet }
        }
    }

    // MARK: - Socket listeners
    private func setupSocketListeners() {
        let sm = socketManager

        sm.on("gameJoined") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.joiningGame = false
                let pid = data["playerId"] as? String ?? ""
                self.playerId = pid
                self.playerIdRef = pid
                self.gameId = data["roomId"] as? String ?? ""
                self.isHost = data["isHost"] as? Bool ?? false
                if let tl = data["timeLimit"] as? Int { self.gameTimeLimit = tl }
                if let pwd = data["password"] as? String { self.roomPassword = pwd }
                if let players = data["players"] as? [[String: Any]] {
                    let myId = data["playerId"] as? String ?? ""
                    if let opp = players.first(where: { ($0["id"] as? String) != myId }) {
                        self.opponentName = opp["name"] as? String ?? ""
                        self.opponentSocketId = opp["id"] as? String
                    }
                }
                if data["spectator"] as? Bool == true {
                    self.isSpectator = true
                    // spectator might join mid-battle
                } else {
                    self.phase = "waiting"
                    if self.opponentName.isEmpty {
                        self.setMessage(self.s.waitingForOpponent, "info", duration: 0)
                    } else {
                        self.setMessage(self.s.bothPlayersIn, "success")
                    }
                }
            }
        }

        sm.on("playerJoined") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                if let players = data["players"] as? [[String: Any]] {
                    let myId = self.playerIdRef
                    if let opp = players.first(where: { ($0["id"] as? String) != myId }) {
                        self.opponentName = opp["name"] as? String ?? ""
                        self.opponentSocketId = opp["id"] as? String
                    }
                }
                self.setMessage(self.s.opponentJoined, "success")
            }
        }

        sm.on("error") { [weak self] args in
            guard let self else { return }
            // Ignore system-level socket errors (no dict payload)
            guard let data = args.first as? [String: Any],
                  data["error"] != nil || data["message"] != nil else { return }
            DispatchQueue.main.async {
                self.joiningGame = false
                let msg = data["error"] as? String ?? data["message"] as? String ?? self.s.unknownError
                self.setMessage("❌ \(msg)", "error")
            }
        }

        sm.on("placementFinished") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async { self.isReady = true }
        }

        sm.on("playerReady") { [weak self] _ in
            guard let self else { return }
            DispatchQueue.main.async {
                self.opponentReady = true
                self.setMessage(self.s.opponentIsReady, "success")
            }
        }

        sm.on("placementUnreadied") { [weak self] _ in
            DispatchQueue.main.async { self?.isReady = false }
        }

        sm.on("playerUnreadied") { [weak self] _ in
            DispatchQueue.main.async { self?.opponentReady = false }
        }

        sm.on("battleStarted") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.phase = "battle"
                self.currentTurn = data["currentTurn"] as? String
                if let tl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(tl) }
                if let ts = data["turnStartedAt"] as? Double { self.turnStartedAt = self.localizeTs(data, ts) }
                if self.isMyTurn { SoundManager.shared.playTurn() }
            }
        }

        sm.on("shotResult") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.shootPending = false
                self.shootTimeoutWork?.cancel()
                let row = data["row"] as? Int ?? 0
                let col = data["col"] as? Int ?? 0
                let isHit = data["isHit"] as? Bool ?? false
                let shipSunk = data["shipSunk"] as? Bool ?? false
                let isMine = (data["shooterId"] as? String) == self.playerIdRef
                let cellKey = "\(row),\(col)"

                // Use server-provided boards and apply local sunk overlay (server has no SUNK state)
                if let rawPlayer = data["playerBoard"] as? [[Any]] {
                    self.playerBoard = overlayBoard(parseBoardFromJson(rawPlayer), sunkSet: self.playerSunkOverlay)
                }
                if let rawOpponent = data["opponentBoard"] as? [[Any]] {
                    self.opponentBoard = overlayBoard(parseBoardFromJson(rawOpponent), sunkSet: self.opponentSunkOverlay)
                }

                if shipSunk {
                    let shipName = data["sunkShipName"] as? String ?? ""
                    let sunkCells = data["sunkShipCells"] as? [[String: Any]] ?? []
                    if isMine {
                        self.theirSunkCount += 1
                        self.setMessage(self.s.sunkTheirShip.fmt(shipName), "success")
                    } else {
                        self.mySunkCount += 1
                        self.setMessage(self.s.yourShipSunk.fmt(shipName), "error")
                        UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                    }
                    SoundManager.shared.playSunk()
                    let sunkTuples = sunkCells.compactMap { d -> (Int,Int)? in
                        guard let r = d["row"] as? Int, let c = d["col"] as? Int else { return nil }
                        return (r, c)
                    }
                    let sunkKeys = Set(sunkTuples.map { "\($0.0),\($0.1)" })
                    let safeKeys = Set(getSurroundingKeys(shipCells: sunkTuples).map { $0 + "_safe" })
                    if isMine {
                        self.opponentSunkOverlay.formUnion(sunkKeys)
                        self.opponentSunkOverlay.formUnion(safeKeys)
                        self.opponentBoard = overlayBoard(self.opponentBoard, sunkSet: self.opponentSunkOverlay)
                    } else {
                        self.playerSunkOverlay.formUnion(sunkKeys)
                        self.playerSunkOverlay.formUnion(safeKeys)
                        self.playerBoard = overlayBoard(self.playerBoard, sunkSet: self.playerSunkOverlay)
                    }
                    for (r, c) in sunkTuples {
                        let key = "\(r),\(c)"
                        if isMine {
                            self.explosionKeys.insert(key)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in self?.explosionKeys.remove(key) }
                        } else {
                            self.playerExplosionKeys.insert(key)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in self?.playerExplosionKeys.remove(key) }
                        }
                    }
                } else if isHit {
                    if isMine {
                        self.setMessage(self.s.hitShootAgain, "success")
                    } else {
                        self.setMessage(self.s.theyHitYourShip, "error")
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    }
                    SoundManager.shared.playHit()
                } else {
                    if isMine {
                        self.setMessage(self.s.missOpponentTurn, "info")
                    } else {
                        self.setMessage(self.s.theyMissedYourTurn, "success")
                    }
                    SoundManager.shared.playMiss()
                }

                if isMine {
                    self.shotKeys.insert(cellKey)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in self?.shotKeys.remove(cellKey) }
                } else {
                    self.playerShotKeys.insert(cellKey)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in self?.playerShotKeys.remove(cellKey) }
                }

                self.currentTurn = data["currentTurn"] as? String
                if let tl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(tl) }
                if let ts = data["turnStartedAt"] as? Double { self.turnStartedAt = self.localizeTs(data, ts) }
                if self.isMyTurn { SoundManager.shared.playTurn() }

                if let w = data["winner"] as? String {
                    self.winner = w
                    self.phase = "gameOver"
                    if w == self.playerIdRef {
                        SoundManager.shared.playVictory()
                    } else {
                        SoundManager.shared.playDefeat()
                    }
                }
            }
        }

        sm.on("gameReset") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                self.resetBattleState()
                self.phase = "placement"
                self.setMessage(self.s.newGamePlaceShips, "success")
            }
        }

        sm.on("playerLeft") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                let name = data["playerName"] as? String ?? self.s.opponent
                self.setMessage(self.s.playerLeftGame.fmt(name), "info")
                self.resetFullState()
                self.playerLeftJob?.cancel()
                let work = DispatchWorkItem { [weak self] in
                    DispatchQueue.main.async {
                        self?.phase = "login"
                        self?.gameId = ""
                        self?.opponentSunkOverlay = []
                        self?.playerSunkOverlay = []
                    }
                }
                self.playerLeftJob = work
                DispatchQueue.main.asyncAfter(deadline: .now() + 2, execute: work)
            }
        }

        sm.on("opponentLeft") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.resetBattleState()
                self.opponentName = ""
                self.opponentSocketId = nil
                if let h = data["isHost"] as? Bool { self.isHost = h }
                let name = data["playerName"] as? String ?? self.s.opponent
                self.setMessage(self.s.playerLeftWaiting.fmt(name), "info")
                self.phase = "waiting"
            }
        }

        sm.on("leftRoom") { [weak self] _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.chatMessages = []
                self.phase = "login"
                self.gameId = ""
                self.message = ""
                self.messageType = "info"
            }
        }

        sm.on("gameForfeited") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                let forfeiterId = data["forfeiterId"] as? String ?? ""
                let iForfeited = forfeiterId == self.playerIdRef
                self.winner = data["winner"] as? String   // set winner BEFORE phase so didSet plays correct music
                self.phase = "gameOver"
                if iForfeited {
                    SoundManager.shared.playDefeat()
                    self.message = self.s.youSurrendered
                    self.messageType = "info"
                } else {
                    SoundManager.shared.playVictory()
                    let name = data["forfeiterName"] as? String ?? self.s.opponent
                    self.message = self.s.opponentSurrendered.fmt(name)
                    self.messageType = "success"
                }
            }
        }

        sm.on("playAgainRequested") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                if self.isSpectator {
                    let name = (args.first as? [String: Any])?["requesterName"] as? String ?? self.s.opponent
                    self.setMessage("🎮 \(name) wants a rematch!", "info")
                } else { self.opponentWantsPlayAgain = true }
            }
        }

        sm.on("playAgainDeclined") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                if self.isSpectator {
                    let name = (args.first as? [String: Any])?["declinerName"] as? String ?? self.s.opponent
                    self.setMessage("❌ \(name) declined the rematch", "error")
                } else {
                    self.playAgainPending = false
                    self.opponentWantsPlayAgain = false
                    self.setMessage(self.s.opponentDeclinedRematch, "error")
                }
            }
        }

        sm.on("gameStartedByHost") { [weak self] _ in
            guard let self else { return }
            DispatchQueue.main.async {
                self.phase = "placement"
                self.isReady = false
                self.opponentReady = false
                self.setMessage(self.s.gameStartedByHost, "success")
            }
        }

        sm.on("kicked") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                let msg = (data?["error"] as? String ?? data?["message"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? self.s.youWereKicked
                self.resetFullState()
                self.setMessage("❌ \(msg)", "error")
                self.phase = "login"
                self.loginView = "menu"
                self.gameId = ""
                self.roomPassword = ""
                self.isSpectator = false
                self.spectatorCount = 0
            }
        }

        sm.on("playerKicked") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                if data["targetId"] != nil {
                    self.opponentName = ""
                    self.opponentSocketId = nil
                    self.opponentReady = false
                    self.setMessage(self.s.playerKicked, "info")
                }
            }
        }

        sm.on("timeUp") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.winner = data["winner"] as? String
                self.phase = "gameOver"
                let loserId = data["loser"] as? String ?? ""
                if loserId == self.playerIdRef {
                    self.setMessage(self.s.yourClockRanOut, "error")
                    SoundManager.shared.playDefeat()
                } else {
                    self.setMessage(self.s.opponentClockRanOut, "success")
                    SoundManager.shared.playVictory()
                }
            }
        }

        // Spectator events
        sm.on("spectatorJoined") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.joiningGame = false
                self.isSpectator = true
                if let rid = data["roomId"] as? String, !rid.isEmpty { self.gameId = rid }
                let state = data["state"] as? String ?? ""
                switch state {
                case "BATTLE_PHASE": self.phase = "battle"
                case "GAME_OVER": self.phase = "gameOver"
                default: self.phase = "placement"
                }
                // Restore sunk overlay for ships already sunk before spectator joined
                self.spectatorSunkDict = [:]
                if let sunkData = data["sunkShipData"] as? [[String: Any]] {
                    self.applySpectatorSunkDataFromJoin(sunkData)
                }
                if let boards = data["boards"] as? [[String: Any]] {
                    self.spectatorBoards = self.buildSpectatorBoards(boards)
                }
                if let chatHistory = data["chatHistory"] as? [[String: Any]], !chatHistory.isEmpty {
                    self.chatMessages = chatHistory.compactMap { m -> ChatMessage? in
                        guard let id = m["id"] as? String,
                              let senderId = m["senderId"] as? String,
                              let senderName = m["senderName"] as? String,
                              let text = m["text"] as? String else { return nil }
                        let ts = m["timestamp"] as? Double ?? 0
                        let isImportant = m["isImportant"] as? Bool ?? false
                        return ChatMessage(id: id, senderId: senderId, senderName: senderName,
                                          text: text, timestamp: ts, isMine: false, isImportant: isImportant)
                    }
                }
                if let ct = data["currentTurn"] as? String { self.currentTurn = ct }
                if let tl = data["timeLimit"] as? Int, tl > 0 { self.gameTimeLimit = tl }
                if let ptl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(ptl) }
                if let ts = data["turnStartedAt"] as? Double, ts > 0 { self.turnStartedAt = self.localizeTs(data, ts) }
                if let players = data["players"] as? [[String: Any]] {
                    let names = players.compactMap { $0["name"] as? String }
                    if !names.isEmpty {
                        self.setMessage(self.s.spectatingNames.fmt(names.joined(separator: " vs ")), "info", duration: 0)
                    }
                }
            }
        }

        sm.on("spectatorShotResult") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                let boards = data["boards"] as? [[String: Any]] ?? []
                // Track sunk cells so we can render 💀 instead of plain HIT
                if let shipSunk = data["shipSunk"] as? Bool, shipSunk,
                   let sunkCells = data["sunkShipCells"] as? [[String: Any]], !sunkCells.isEmpty,
                   let shooterId = data["shooterId"] as? String {
                    let targetPid = boards.first { ($0["playerId"] as? String) != shooterId }?["playerId"] as? String
                    if let targetPid {
                        let tuples = sunkCells.compactMap { c -> (Int, Int)? in
                            guard let r = c["row"] as? Int, let col = c["col"] as? Int else { return nil }
                            return (r, col)
                        }
                        if self.spectatorSunkDict[targetPid] == nil { self.spectatorSunkDict[targetPid] = [] }
                        tuples.forEach { self.spectatorSunkDict[targetPid]?.insert("\($0.0),\($0.1)") }
                        getSurroundingKeys(shipCells: tuples).forEach { self.spectatorSunkDict[targetPid]?.insert($0 + "_safe") }
                    }
                }
                if !boards.isEmpty {
                    self.spectatorBoards = self.buildSpectatorBoards(boards)
                }
                if let ct = data["currentTurn"] as? String { self.currentTurn = ct }
                if let ptl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(ptl) }
                if let ts = data["turnStartedAt"] as? Double, ts > 0 { self.turnStartedAt = self.localizeTs(data, ts) }
                if let w = data["winner"] as? String {
                    self.winner = w
                    self.phase = "gameOver"
                }
            }
        }

        sm.on("spectatorBattleStarted") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.spectatorSunkDict = [:]   // new game, reset sunk overlay
                self.phase = "battle"
                if let ct = data["currentTurn"] as? String { self.currentTurn = ct }
                if let tl = data["timeLimit"] as? Int, tl > 0 { self.gameTimeLimit = tl }
                if let ptl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(ptl) }
                if let ts = data["turnStartedAt"] as? Double, ts > 0 { self.turnStartedAt = self.localizeTs(data, ts) }
                if let boards = data["boards"] as? [[String: Any]] {
                    self.spectatorBoards = self.buildSpectatorBoards(boards)
                }
            }
        }

        sm.on("spectatorUpdate") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                if let count = data["count"] as? Int { self.spectatorCount = count }
            }
        }

        // Reconnection
        sm.on("rejoinSuccess") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                self.joiningGame = false
                self.shootPending = false
                if let pid = data["playerId"] as? String, !pid.isEmpty {
                    self.playerId = pid; self.playerIdRef = pid
                }
                self.isHost = data["isHost"] as? Bool ?? false
                if let rid = data["roomId"] as? String, !rid.isEmpty { self.gameId = rid }
                if let b = data["board"] as? [[Any]] { self.playerBoard = parseBoardFromJson(b) }
                if let b = data["opponentBoard"] as? [[Any]] { self.opponentBoard = parseBoardFromJson(b) }
                if let ct = data["currentTurn"] as? String, !ct.isEmpty { self.currentTurn = ct }
                if let tl = data["playerTimeLeft"] as? [String: Any] { self.playerTimeLeft = parseTimeLeft(tl) }
                if let ts = data["turnStartedAt"] as? Double, ts > 0 { self.turnStartedAt = self.localizeTs(data, ts) }
                if let tl = data["timeLimit"] as? Int, tl > 0 { self.gameTimeLimit = tl }
                self.winner = data["winner"] as? String
                self.opponentName = (data["opponentName"] as? String) ?? ""
                if data["shipsPlaced"] as? Bool == true { self.isReady = true }

                // Restore chat
                if let chatHistory = data["chatHistory"] as? [[String: Any]] {
                    self.chatMessages = chatHistory.compactMap { m -> ChatMessage? in
                        ChatMessage(
                            id: (m["id"] as? String) ?? UUID().uuidString,
                            senderId: m["senderId"] as? String ?? "",
                            senderName: m["senderName"] as? String ?? "",
                            text: m["text"] as? String ?? "",
                            timestamp: m["timestamp"] as? Double ?? Date().timeIntervalSince1970 * 1000,
                            isMine: (m["senderId"] as? String) == self.playerIdRef,
                            isImportant: m["isImportant"] as? Bool ?? false
                        )
                    }
                }

                // Clear play-again state
                self.playAgainPending = false
                self.opponentWantsPlayAgain = false

                switch data["state"] as? String {
                case "BATTLE_PHASE": self.phase = "battle"
                case "PLACEMENT_PHASE": self.phase = "placement"
                case "GAME_OVER": self.phase = "gameOver"
                case "WAITING_FOR_PLAYERS": self.phase = "waiting"
                default: break
                }
                self.setMessage("✅ \(self.s.reconnected)", "success")
            }
        }

        sm.on("rejoinFailed") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                let reason = (data?["reason"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? self.s.couldNotRejoin
                self.setMessage("❌ \(reason)", "error")
                self.phase = "login"
                self.gameId = ""
                self.roomPassword = ""
                self.isSpectator = false
            }
        }

        sm.on("opponentReconnecting") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                let name = (data?["playerName"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? self.opponentName
                self.setMessage("⏳ \(self.s.opponentLostConnection.fmt(name))", "info", duration: 0)
            }
        }

        sm.on("opponentReconnected") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                if let pid = data?["playerId"] as? String, !pid.isEmpty { self.opponentSocketId = pid }
                let name = (data?["playerName"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? self.opponentName
                self.setMessage("✅ \(self.s.opponentReconnected.fmt(name))", "success")
            }
        }

        sm.on("opponentReconnectFailed") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                let name = (data?["playerName"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? self.opponentName
                self.playAgainPending = false
                self.opponentWantsPlayAgain = false
                self.setMessage("❌ \(self.s.opponentDisconnected.fmt(name))", "error")
            }
        }

        sm.on("roomClosed") { [weak self] args in
            guard let self else { return }
            DispatchQueue.main.async {
                let data = args.first as? [String: Any]
                let reason = data?["reason"] as? String ?? "Room was closed"
                self.setMessage("⚠️ \(reason)", "error")
                self.phase = "login"
                self.gameId = ""
            }
        }

        sm.on("chatMessage") { [weak self] args in
            guard let self, let data = args.first as? [String: Any] else { return }
            DispatchQueue.main.async {
                let isMine = (data["senderId"] as? String) == self.playerIdRef
                let msg = ChatMessage(
                    id: (data["id"] as? String) ?? UUID().uuidString,
                    senderId: data["senderId"] as? String ?? "",
                    senderName: data["senderName"] as? String ?? "",
                    text: data["text"] as? String ?? "",
                    timestamp: data["timestamp"] as? Double ?? Date().timeIntervalSince1970 * 1000,
                    isMine: isMine,
                    isImportant: data["isImportant"] as? Bool ?? false
                )
                self.chatMessages.append(msg)
                if self.chatMessages.count > 200 { self.chatMessages.removeFirst(self.chatMessages.count - 200) }
                if !isMine {
                    if !self.chatOpen { self.chatUnread += 1 }
                    SoundManager.shared.playChat()
                }
            }
        }
    }
}

// MARK: - Helpers
extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        self?.isEmpty ?? true
    }
}
