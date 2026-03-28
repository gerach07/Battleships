const crypto = require('crypto');
const {
    GRID_SIZE,
    SHIPS,
    SHIP_NAMES,
    TOTAL_SEGMENTS,
    CellState,
    GameState,
    RATE_LIMIT_SHOTS_PER_SECOND,
    RATE_LIMIT_PLACEMENTS_PER_SECOND,
    RATE_LIMIT_CHAT_PER_SECOND,
    DEFAULT_GAME_TIME_SECONDS,
    MAX_SPECTATORS,
    MAX_CHAT_LENGTH,
} = require('../constants');
const {
    deepCloneBoard,
    isValidCoordinate,
} = require('../utils/sanitizers');
const RateLimiter = require('../utils/RateLimiter');

class Room {
    constructor(roomId, password = null, hostName = null, timeLimit = DEFAULT_GAME_TIME_SECONDS, hostId = null) {
        this.roomId = roomId;
        this.password = password;
        this.hostName = hostName;
        this.hostId = hostId;
        this.hostStartedGame = false;
        this.timeLimit = Math.max(120, Math.min(600, timeLimit || DEFAULT_GAME_TIME_SECONDS));
        this.createdAt = Date.now();
        this.lastActivity = Date.now();
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.players = {};
        this.spectators = new Set();
        this.currentTurn = null;
        this.winner = null;
        this.firstPlayer = null;
        this.playerTimeLeft = {};
        this.turnStartedAt = null;
        this.shotLimiter = new RateLimiter(RATE_LIMIT_SHOTS_PER_SECOND, 1000);
        this.placementLimiter = new RateLimiter(RATE_LIMIT_PLACEMENTS_PER_SECOND, 1000);
        this.chatLimiter = new RateLimiter(RATE_LIMIT_CHAT_PER_SECOND, 1000);
        this.playAgainVotes = new Set();
        this.chatMessages = [];
        this._chatIdCounter = 0;
    }

    touch() {
        this.lastActivity = Date.now();
    }

    isInactive(ms) {
        return Date.now() - this.lastActivity > ms;
    }

    isEmpty() {
        return Object.keys(this.players).length === 0;
    }

    playerCount() {
        return Object.keys(this.players).length;
    }

    /** Serialise player list for client emission (avoids repeating this pattern) */
    getPlayerList() {
        return Object.values(this.players).map(p => ({ id: p.id, name: p.name }));
    }

    addSpectator(socketId) {
        if (this.spectators.has(socketId)) return true;  // idempotent — already registered
        if (this.spectators.size >= MAX_SPECTATORS) return false;
        this.spectators.add(socketId);
        return true;
    }

    hasPassword() {
        return this.password !== null && this.password !== '';
    }

    checkPassword(password) {
        if (!this.hasPassword()) return true;
        if (typeof password !== 'string') return false;
        // Constant-time comparison: hash both to fixed length to avoid leaking password length
        const a = crypto.createHash('sha256').update(this.password).digest();
        const b = crypto.createHash('sha256').update(password).digest();
        return crypto.timingSafeEqual(a, b);
    }

    _createEmptyBoard() {
        return Array(GRID_SIZE)
            .fill(null)
            .map(() => Array(GRID_SIZE).fill(CellState.WATER));
    }

    addPlayer(playerId, playerName) {
        if (Object.keys(this.players).length >= 2) {
            return false;
        }

        this.players[playerId] = {
            id: playerId,
            name: playerName,
            board: this._createEmptyBoard(),
            ships: [],
            shipsPlaced: false,
            totalHitsReceived: 0,
            joinedAt: Date.now(),
        };

        return true;
    }

    isHost(playerId) {
        return this.hostId === playerId;
    }

    kickPlayer(kickerId, targetId) {
        if (!this.isHost(kickerId)) {
            return { success: false, error: 'Only the host can kick players' };
        }
        if (targetId === kickerId) {
            return { success: false, error: 'Cannot kick yourself' };
        }
        if (!this.players[targetId]) {
            return { success: false, error: 'Player not found' };
        }
        delete this.players[targetId];
        return { success: true };
    }

    startGameByHost(hostId) {
        if (!this.isHost(hostId)) {
            return { success: false, error: 'Only the host can start the game' };
        }
        if (this.state !== GameState.WAITING_FOR_PLAYERS) {
            return { success: false, error: 'Game already started or in invalid state' };
        }
        if (Object.keys(this.players).length !== 2) {
            return { success: false, error: 'Need 2 players to start' };
        }
        this._transitionState(GameState.PLACEMENT_PHASE);
        this.hostStartedGame = true;
        return { success: true };
    }

    // Reset the room back to WAITING_FOR_PLAYERS after one player leaves mid-game.
    // The remaining player stays in the room so a new opponent can join.
    resetToWaiting(remainingPlayerId) {
        if (!this._transitionState(GameState.WAITING_FOR_PLAYERS)) {
            console.error(`resetToWaiting: invalid transition from ${this.state}`);
            return;
        }
        this.winner = null;
        this.currentTurn = null;
        this.firstPlayer = null;
        this.playerTimeLeft = {};
        this.turnStartedAt = null;
        this.hostStartedGame = false;
        // Transfer host to remaining player if the old host left
        if (this.hostId !== remainingPlayerId) {
            this.hostId = remainingPlayerId;
        }
        this.playAgainVotes.clear();
        const p = this.players[remainingPlayerId];
        if (p) {
            p.board = this._createEmptyBoard();
            p.ships = [];
            p.shipsPlaced = false;
            p.totalHitsReceived = 0;
        }
    }

    validateShipPlacement(playerId, row, col, length, direction) {
        if (!Number.isInteger(row) || !Number.isInteger(col) || !Number.isInteger(length)) {
            return { valid: false, error: 'Invalid coordinates (must be integers)' };
        }

        if (!isValidCoordinate(row, col)) {
            return { valid: false, error: 'Invalid starting position' };
        }

        if (!SHIPS.includes(length)) {
            return { valid: false, error: 'Invalid ship length' };
        }

        if (!['horizontal', 'vertical'].includes(direction)) {
            return { valid: false, error: 'Invalid direction' };
        }

        if (!this.players[playerId]) {
            return { valid: false, error: 'Player not found' };
        }

        const board = this.players[playerId].board;

        if (direction === 'horizontal') {
            if (col + length > GRID_SIZE) {
                return { valid: false, error: 'Ship extends beyond boundary' };
            }
            for (let i = 0; i < length; i++) {
                if (board[row][col + i] !== CellState.WATER) {
                    return { valid: false, error: 'Cell occupied' };
                }
            }
        } else {
            if (row + length > GRID_SIZE) {
                return { valid: false, error: 'Ship extends beyond boundary' };
            }
            for (let i = 0; i < length; i++) {
                if (board[row + i][col] !== CellState.WATER) {
                    return { valid: false, error: 'Cell occupied' };
                }
            }
        }

        // Check 1-cell buffer around all ship cells (diagonals included)
        const shipCells = [];
        if (direction === 'horizontal') {
            for (let i = 0; i < length; i++) shipCells.push({ row, col: col + i });
        } else {
            for (let i = 0; i < length; i++) shipCells.push({ row: row + i, col });
        }
        const shipSet = new Set(shipCells.map(c => `${c.row},${c.col}`));
        for (const c of shipCells) {
            for (let dr = -1; dr <= 1; dr++) {
                for (let dc = -1; dc <= 1; dc++) {
                    if (dr === 0 && dc === 0) continue;
                    const nr = c.row + dr, nc = c.col + dc;
                    if (nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE) continue;
                    if (shipSet.has(`${nr},${nc}`)) continue;
                    if (board[nr][nc] !== CellState.WATER) {
                        return { valid: false, error: 'Ships must have 1-cell gap (including diagonals)' };
                    }
                }
            }
        }

        return { valid: true, error: null };
    }

    placeShip(playerId, row, col, length, direction, shipName) {
        if (!this.placementLimiter.isAllowed(playerId)) {
            return false;
        }

        const validation = this.validateShipPlacement(playerId, row, col, length, direction);
        if (!validation.valid) {
            return false;
        }

        const board = this.players[playerId].board;
        const shipCells = [];

        if (direction === 'horizontal') {
            for (let i = 0; i < length; i++) {
                board[row][col + i] = CellState.SHIP;
                shipCells.push({ row, col: col + i });
            }
        } else {
            for (let i = 0; i < length; i++) {
                board[row + i][col] = CellState.SHIP;
                shipCells.push({ row: row + i, col });
            }
        }

        this.players[playerId].ships.push({
            id: this.players[playerId].ships.length,
            name: shipName || SHIP_NAMES[this.players[playerId].ships.length] || 'Ship',
            length,
            cells: shipCells,
            hitsReceived: 0,
            sunk: false,
        });

        return true;
    }

    hasPlayerPlacedAllShips(playerId) {
        const placed = this.players[playerId].ships;
        if (placed.length !== SHIPS.length) return false;
        const placedLengths = placed.map(s => s.length).sort((a, b) => a - b);
        const expectedLengths = [...SHIPS].sort((a, b) => a - b);
        return placedLengths.every((len, i) => len === expectedLengths[i]);
    }

    markPlayerReady(playerId) {
        if (!this.hasPlayerPlacedAllShips(playerId)) {
            return false;
        }

        this.players[playerId].shipsPlaced = true;

        const playerIds = this.getPlayerIds();
        const allReady = playerIds.length === 2 &&
            playerIds.every(id => this.players[id].shipsPlaced);

        if (allReady) {
            this._transitionState(GameState.BATTLE_PHASE);
            // Swap first turn on rematch
            if (this.firstPlayer && playerIds.includes(this.firstPlayer)) {
                this.currentTurn = this.getOpponentId(this.firstPlayer);
            } else {
                this.currentTurn = playerIds[0];
            }
            this.firstPlayer = this.currentTurn;
            // Initialize per-player chess clocks
            const pids = this.getPlayerIds();
            this.playerTimeLeft = {};
            pids.forEach(pid => { this.playerTimeLeft[pid] = this.timeLimit; });
            this.turnStartedAt = Date.now();
        }

        return allReady;
    }

    processShot(shooterId, row, col) {
        if (this.state !== GameState.BATTLE_PHASE) {
            return { success: false, error: 'Not in battle phase', result: null };
        }

        if (this.currentTurn !== shooterId) {
            return { success: false, error: 'Not your turn', result: null };
        }

        if (!this.shotLimiter.isAllowed(shooterId)) {
            return { success: false, error: 'Shot rate limit exceeded', result: null };
        }

        if (!isValidCoordinate(row, col)) {
            return { success: false, error: 'Coordinates out of bounds', result: null };
        }

        const opponentId = this.getOpponentId(shooterId);
        if (!opponentId) return { success: false, error: 'Opponent not found', result: null };
        const opponentBoard = this.players[opponentId].board;

        const targetCell = opponentBoard[row][col];

        if (targetCell === CellState.HIT || targetCell === CellState.MISS) {
            return { success: false, error: 'Cell already targeted', result: null };
        }

        // Consume elapsed turn time from the shooter's clock
        if (this.timeLimit && this.turnStartedAt) {
            const elapsed = (Date.now() - this.turnStartedAt) / 1000;
            this.playerTimeLeft[shooterId] = Math.max(0,
                (this.playerTimeLeft[shooterId] ?? this.timeLimit) - elapsed
            );
            this.turnStartedAt = Date.now();
            if (this.playerTimeLeft[shooterId] <= 0) {
                const timeOutWinner = opponentId;
                this.winner = timeOutWinner;
                this._transitionState(GameState.GAME_OVER);
                return {
                    success: true, error: null,
                    result: {
                        row, col, isHit: false, shipSunk: false, gameWon: true, timeOut: true,
                        winner: this.winner,
                        playerTimeLeft: { ...this.playerTimeLeft },
                        turnStartedAt: this.turnStartedAt,
                    }
                };
            }
        }

        const result = {
            isHit: false,
            shipSunk: false,
            shipId: null,
            gameWon: false,
            row,
            col,
        };

        if (targetCell === CellState.SHIP) {
            opponentBoard[row][col] = CellState.HIT;
            result.isHit = true;
            this.players[opponentId].totalHitsReceived++;

            for (let ship of this.players[opponentId].ships) {
                const hit = ship.cells.some(c => c.row === row && c.col === col);
                if (hit) {
                    ship.hitsReceived++;
                    result.shipId = ship.id;
                    if (ship.hitsReceived === ship.length) {
                        ship.sunk = true;
                        result.shipSunk = true;
                        result.sunkShipCells = ship.cells;
                        result.sunkShipName = ship.name || 'Ship';
                    }
                    break;
                }
            }

            if (this.players[opponentId].totalHitsReceived === TOTAL_SEGMENTS) {
                result.gameWon = true;
                this.winner = shooterId;
                this._transitionState(GameState.GAME_OVER);
            }
        } else {
            opponentBoard[row][col] = CellState.MISS;
            result.isHit = false;
        }

        if (!result.gameWon && !result.isHit) {
            this.currentTurn = opponentId;
        }
        result.nextTurn = this.currentTurn;
        result.playerTimeLeft = { ...this.playerTimeLeft };
        result.turnStartedAt = this.turnStartedAt;

        return {
            success: true,
            error: null,
            result,
        };
    }

    getOpponentViewBoard(playerId) {
        const opponentId = this.getOpponentId(playerId);
        if (!opponentId || !this.players[opponentId]) return this._createEmptyBoard();
        const board = this.players[opponentId].board;

        return board.map(row =>
            row.map(cell => (cell === CellState.SHIP ? CellState.WATER : cell))
        );
    }

    getPlayerBoard(playerId) {
        if (!this.players[playerId]) return this._createEmptyBoard();
        return deepCloneBoard(this.players[playerId].board);
    }

    _transitionState(newState) {
        const validTransitions = {
            [GameState.WAITING_FOR_PLAYERS]: [GameState.PLACEMENT_PHASE, GameState.WAITING_FOR_PLAYERS],
            [GameState.PLACEMENT_PHASE]: [GameState.BATTLE_PHASE, GameState.WAITING_FOR_PLAYERS],
            [GameState.BATTLE_PHASE]: [GameState.GAME_OVER, GameState.WAITING_FOR_PLAYERS],
            [GameState.GAME_OVER]: [GameState.PLACEMENT_PHASE, GameState.WAITING_FOR_PLAYERS],
        };

        const allowed = validTransitions[this.state];
        if (!allowed || !allowed.includes(newState)) {
            console.error(`Invalid state transition: ${this.state} -> ${newState}`);
            return false;
        }

        this.state = newState;
        return true;
    }

    getPlayerIds() {
        return Object.keys(this.players);
    }

    getOpponentId(playerId) {
        return this.getPlayerIds().find(id => id !== playerId) || null;
    }

    isFull() {
        return Object.keys(this.players).length === 2;
    }

    getState() {
        return this.state;
    }

    getEffectiveTimeLeft(playerId) {
        const stored = this.playerTimeLeft[playerId] ?? this.timeLimit;
        if (stored == null) return null;
        if (this.currentTurn === playerId && this.turnStartedAt) {
            const elapsed = (Date.now() - this.turnStartedAt) / 1000;
            return Math.max(0, stored - elapsed);
        }
        return stored;
    }

    isCurrentPlayerTimeUp() {
        if (!this.timeLimit || !this.turnStartedAt || this.state !== GameState.BATTLE_PHASE || !this.currentTurn) return false;
        return this.getEffectiveTimeLeft(this.currentTurn) <= 0;
    }

    getSpectatorBoards() {
        const ids = this.getPlayerIds();
        return ids
            .filter(pid => this.players[pid])
            .map(pid => ({
                playerId: pid,
                playerName: this.players[pid].name,
                board: this.players[pid].board.map(row =>
                    row.map(cell => (cell === CellState.SHIP ? CellState.WATER : cell))
                ),
            }));
    }

    /** Return sunk ship cells per player — used to populate spectator overlay on join */
    getSunkShipData() {
        return this.getPlayerIds()
            .filter(pid => this.players[pid])
            .map(pid => ({
                playerId: pid,
                sunkShips: this.players[pid].ships
                    .filter(s => s.sunk)
                    .map(s => ({ cells: s.cells, name: s.name })),
            }));
    }

    addChatMessage(senderId, senderName, text, isImportant = false) {
        if (!this.chatLimiter.isAllowed(senderId)) return null;
        this._chatIdCounter++;
        const msg = {
            id: `${this.roomId}-${this._chatIdCounter}`,
            senderId,
            senderName,
            text: text.slice(0, MAX_CHAT_LENGTH),
            timestamp: Date.now(),
            ...(isImportant && { isImportant: true }),
        };
        this.chatMessages.push(msg);
        return msg;
    }
}

module.exports = Room;
