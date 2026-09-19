/**
 * ============================================================================
 * BATTLESHIPS GAME SERVER - MODULAR & PRODUCTION-GRADE
 * ============================================================================
 */

const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const compression = require('compression');
const helmet = require('helmet');
const mongoose = require('mongoose');
require('dotenv').config();

// Models & Utils
const Room = require('./src/models/Room');
const pkg = require('./package.json');
const { GameState, DEFAULT_GAME_TIME_SECONDS, MIN_GAME_TIME_SECONDS, MAX_GAME_TIME_SECONDS, SHIPS, SHIP_NAMES } = require('./src/constants');
const { sanitizeInput } = require('./src/utils/sanitizers');
const { TimestampTracker } = require('./src/utils/RateLimiter');

// Auth & API
const { initFirebase, verifyToken } = require('./src/auth/firebase');
const apiRoutes = require('./src/routes/api');
const { recordGameResult } = require('./src/utils/gameStats');

// ============================================================================
// CONFIGURATION
// ============================================================================

const ALLOWED_ORIGINS = [...new Set([
  ...(process.env.ALLOWED_ORIGINS || 'http://localhost:3000,http://localhost:3001').split(',').map(o => o.trim()).filter(Boolean),
  'https://abbattleships.web.app',
  'https://abbattleships.firebaseapp.com',
  'https://battleships-server-jtit.onrender.com',
])];

/**
 * CORS origin validator.
 * Allows:
 *  - any origin in the ALLOWED_ORIGINS list
 *  - null / undefined origin (Electron desktop app loaded via file://)
 */
function isOriginAllowed(origin, callback) {
  if (!origin || ALLOWED_ORIGINS.includes(origin)) {
    callback(null, true);
  } else {
    callback(new Error(`Origin '${origin}' not allowed by CORS`));
  }
}

const app = express();
app.set('trust proxy', 1);
app.use(compression());
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'"],
      styleSrc: ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
      fontSrc: ["'self'", 'https://fonts.gstatic.com'],
      connectSrc: ["'self'", ...ALLOWED_ORIGINS, ...ALLOWED_ORIGINS.map(o => o.replace(/^http/, 'ws'))],
      imgSrc: ["'self'", 'data:', 'blob:'],
      mediaSrc: ["'self'", 'data:', 'blob:'],
    },
  },
  crossOriginEmbedderPolicy: false,
}));
app.use(cors({
  origin: isOriginAllowed,
  methods: ['GET', 'POST', 'PUT', 'OPTIONS'],
  credentials: true,
}));
app.use(express.json({ limit: '16kb' }));

// Mount API routes
app.use('/api', apiRoutes);

const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: isOriginAllowed,
    methods: ['GET', 'POST', 'PUT', 'OPTIONS'],
    credentials: true,
  },
  pingTimeout: 60000,
  pingInterval: 25000,
  maxHttpBufferSize: 64e3,
  transports: ['websocket', 'polling'],
});

// ============================================================================
// GLOBAL STATE
// ============================================================================

const rooms = {};
const playerToRoom = {};
/** Per-room mutex to prevent concurrent shot processing (race condition guard) */
const roomLocks = {};

/** IP-based connection rate limiter — prevents mass connection spam */
const ipConnectionTracker = new TimestampTracker(15, 60_000);

/** PIN brute-force rate limiter — per IP+room, 5 attempts per minute */
const pinAttemptTracker = new TimestampTracker(5, 60_000);

/** HTTP /rooms listing rate limiter — per IP, 30 requests per minute */
const roomsListTracker = new TimestampTracker(30, 60_000);

/** joinGame event rate limiter — per socket, 10 joins per minute */
const joinEventTracker = new TimestampTracker(10, 60_000);

const ROOM_CLEANUP_INTERVAL_MS = 30000;
const ROOM_INACTIVE_TIMEOUT_MS = 5 * 60 * 1000;
const ROOM_GAMEOVER_TIMEOUT_MS = 2 * 60 * 1000;

/** Interval handles — stored so they can be cleared on shutdown */
let cleanupIntervalId = null;
let timerIntervalId = null;

/** Grace period before treating a disconnect as a real leave (allows reconnect) */
const DISCONNECT_GRACE_MS = 10_000;
/** Pending disconnect timers — key: "roomId:playerName", value: { timer, oldSocketId } */
const pendingDisconnects = new Map();

/** Validate room ID format — 4-10 alphanumeric characters */
function isValidRoomId(id) {
  return typeof id === 'string' && /^[A-Z0-9]{4,10}$/i.test(id);
}

// ============================================================================
// GAME HELPERS
// ============================================================================

/**
 * Simple per-room lock to serialise shot processing and prevent race conditions
 * where two near-simultaneous events both read stale state.
 */
function acquireRoomLock(roomId) {
  if (!roomLocks[roomId]) roomLocks[roomId] = { tail: Promise.resolve(), depth: 0 };
  const lock = roomLocks[roomId];
  let release;
  const next = new Promise(resolve => { release = resolve; });
  const prev = lock.tail;
  lock.depth++;
  lock.tail = prev.then(() => next);
  const wrappedRelease = () => {
    lock.depth--;
    // Reset the chain when no waiters remain — prevents unbounded promise chain growth
    if (lock.depth === 0) lock.tail = Promise.resolve();
    release();
  };
  return prev.then(() => wrappedRelease);
}

/**
 * Unified handler for a player (or spectator) leaving a room, either
 * voluntarily or due to a disconnect.  Handles all room states correctly:
 *   BATTLE_PHASE   → opponent wins, room moves to GAME_OVER
 *   PLACEMENT/GAME_OVER → room resets to WAITING so remaining player can wait
 *   WAITING        → player removed; room deleted if now empty
 */
async function handlePlayerLeave(socketId, io) {
  const roomId = playerToRoom[socketId];
  if (!roomId) return;
  const room = rooms[roomId];
  if (!room) { delete playerToRoom[socketId]; return; }

  // --- Spectator leaving (no lock needed) ---
  if (room.spectators.has(socketId)) {
    room.spectators.delete(socketId);
    io.to(roomId).emit('spectatorUpdate', { count: room.spectators.size });
    const s = io.sockets.sockets.get(socketId);
    s?.leave(roomId);
    delete playerToRoom[socketId];
    return;
  }

  const release = await acquireRoomLock(roomId);
  try {
    // Re-check room still exists after lock
    if (!rooms[roomId] || !room.players[socketId]) {
      delete playerToRoom[socketId];
      return;
    }

    const playerName = room.players[socketId]?.name;
    const opponentId = room.getOpponentId(socketId);
    const prevState = room.state;

    // Handle state-specific consequences before removing the player
    if (prevState === GameState.BATTLE_PHASE && opponentId) {
      room.winner = opponentId;
      room._transitionState(GameState.GAME_OVER);
      io.to(roomId).emit('gameForfeited', {
        winner: opponentId,
        forfeiterId: socketId,
        forfeiterName: playerName,
      });
    }

    // Remove the leaving player
    delete room.players[socketId];
    room.shotLimiter.removePlayer(socketId);
    room.chatLimiter.removePlayer(socketId);
    room.placementLimiter.removePlayer(socketId);

    if (room.isEmpty()) {
      // Notify and clean up spectators before deleting the room
      room.spectators.forEach(sid => {
        io.to(sid).emit('playerLeft', { playerName: playerName || 'Host' });
        const specSocket = io.sockets.sockets.get(sid);
        specSocket?.leave(roomId);
        delete playerToRoom[sid];
      });
      delete rooms[roomId];
      delete roomLocks[roomId];
    } else if (opponentId) {
      if (prevState === GameState.GAME_OVER) {
        // Game is fully over — discard the room entirely and send winner home
        room.spectators.forEach(sid => {
          io.to(sid).emit('playerLeft', { playerName });
          const specSocket = io.sockets.sockets.get(sid);
          specSocket?.leave(roomId);
          delete playerToRoom[sid];
        });
        // Tell the remaining player (winner) the session is over
        io.to(opponentId).emit('gameSessionEnded', { reason: 'opponentLeft', playerName });
        // Clean up the winner's room membership too
        const winnerSocket = io.sockets.sockets.get(opponentId);
        winnerSocket?.leave(roomId);
        delete playerToRoom[opponentId];
        delete rooms[roomId];
        delete roomLocks[roomId];
      } else if (prevState === GameState.PLACEMENT_PHASE || prevState === GameState.BATTLE_PHASE || prevState === GameState.WAITING_FOR_PLAYERS) {
        room.resetToWaiting(opponentId);
        // Boot spectators — nothing to watch in WAITING
        room.spectators.forEach(sid => {
          io.to(sid).emit('playerLeft', { playerName });
          const specSocket = io.sockets.sockets.get(sid);
          specSocket?.leave(roomId);
          delete playerToRoom[sid];
        });
        room.spectators.clear();
        io.to(opponentId).emit('opponentLeft', { playerName, isHost: room.isHost(opponentId) });
        room.touch();
      }
      // For BATTLE_PHASE, gameForfeited was already emitted above
    }

    const s = io.sockets.sockets.get(socketId);
    s?.leave(roomId);
  } finally {
    delete playerToRoom[socketId];
    release();
  }
}

// ============================================================================
// SOCKET HANDLERS
// ============================================================================

io.on('connection', (socket) => {
  const clientIp = socket.handshake.headers['x-forwarded-for']?.split(',')[0]?.trim() || socket.handshake.address;
  console.log(`New connection: ${socket.id} (IP: ${clientIp})`);

  // Handle unexpected socket-level errors
  socket.on('error', (err) => {
    console.error(`Socket error [${socket.id}]:`, err.message || err);
  });

  // ── IP-based connection rate limiting ──
  if (!ipConnectionTracker.isAllowed(clientIp)) {
    console.warn(`IP rate limit exceeded: ${clientIp}`);
    socket.emit('error', { error: 'Too many connections. Please wait a moment.' });
    socket.disconnect(true);
    return;
  }

  socket.on('joinGame', async (payload) => {
    // Per-socket rate limit on join attempts
    if (!joinEventTracker.isAllowed(socket.id)) return socket.emit('error', { error: 'Too many join attempts. Wait a moment.' });
    // Defensive: tolerate malformed payloads gracefully
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return socket.emit('error', { error: 'Invalid request' });
    const { gameId, playerName, password, isCreating, isSpectating, timeLimit: hostTimeLimit, authToken } = payload;

    // Optionally verify Firebase token for authenticated players
    let firebaseUid = null;
    if (authToken) {
      const decoded = await verifyToken(authToken);
      if (decoded) firebaseUid = decoded.uid;
    }

    // Clean up any stale room membership from a previous game
    if (playerToRoom[socket.id]) {
      await handlePlayerLeave(socket.id, io);
    }

    const roomId = sanitizeInput(gameId, 50).toUpperCase();
    const name = sanitizeInput(playerName, 50) || 'Anonymous';
    const pwd = password ? sanitizeInput(password, 3) : null;

    if (!roomId || !isValidRoomId(roomId)) return socket.emit('error', { error: 'Invalid room ID (4-10 alphanumeric characters)' });

    // Acquire room lock — serialise join/create vs. concurrent events on the same room
    const release = await acquireRoomLock(roomId);
    try {
      let room = rooms[roomId];

      // Spectator mode
      if (isSpectating) {
        if (!room) return socket.emit('error', { error: 'Room does not exist' });
        if (room.state === GameState.WAITING_FOR_PLAYERS) return socket.emit('error', { error: 'Game hasn\'t started yet — try again later' });
        if (!room.checkPassword(pwd)) return socket.emit('error', { error: 'Incorrect password' });
        if (!room.addSpectator(socket.id)) return socket.emit('error', { error: 'Spectator slots full' });
        playerToRoom[socket.id] = roomId;
        socket.join(roomId);
        socket.emit('spectatorJoined', {
          roomId: room.roomId,
          players: room.getPlayerList(),
          state: room.getState(),
          timeLimit: room.timeLimit,
          playerTimeLeft: room.playerTimeLeft,
          turnStartedAt: room.turnStartedAt,
          currentTurn: room.currentTurn,
          boards: room.getSpectatorBoards(),
          sunkShipData: room.getSunkShipData(),
          chatHistory: room.chatMessages || [],
          serverNow: Date.now(),
        });
        io.to(roomId).emit('spectatorUpdate', { count: room.spectators.size });
        room.touch();
        return;
      }

      if (isCreating) {
        if (room) return socket.emit('error', { error: 'Room already exists' });
        const tl = parseInt(hostTimeLimit) || DEFAULT_GAME_TIME_SECONDS;
        room = new Room(roomId, pwd, name, Math.max(MIN_GAME_TIME_SECONDS, Math.min(MAX_GAME_TIME_SECONDS, tl)), socket.id);
        rooms[roomId] = room;
      } else {
        if (!room) return socket.emit('error', { error: 'Room does not exist' });
        if (room.isFull()) return socket.emit('error', { error: 'Room is full' });
        if (!room.checkPassword(pwd)) return socket.emit('error', { error: 'Incorrect password' });
      }

      if (room.addPlayer(socket.id, name, firebaseUid)) {
        playerToRoom[socket.id] = roomId;
        socket.join(roomId);

        socket.emit('gameJoined', {
          playerId: socket.id,
          roomId: room.roomId,
          password: isCreating ? pwd : undefined,
          players: room.getPlayerList(),
          board: room.getPlayerBoard(socket.id),
          state: room.getState(),
          timeLimit: room.timeLimit,
          hostId: room.hostId,
          isHost: room.isHost(socket.id),
          chatHistory: room.chatMessages || [],
        });

        socket.to(roomId).emit('playerJoined', {
          playerCount: room.playerCount(),
          players: room.getPlayerList(),
          state: room.getState(),
          hostId: room.hostId,
        });
        room.touch();
      }
    } finally {
      release();
    }
  });

  // ── Rejoin after brief disconnect ──
  socket.on('rejoinGame', async (payload) => {
    if (!payload || typeof payload !== 'object') return;
    const { gameId, playerName, password } = payload;
    if (typeof gameId !== 'string' || typeof playerName !== 'string') return;
    if (password !== undefined && password !== null && typeof password !== 'string') return;
    const roomId = sanitizeInput(gameId, 50).toUpperCase();
    const name = sanitizeInput(playerName, 50) || 'Anonymous';
    const pwd = password ? sanitizeInput(password, 3) : null;
    if (!roomId || !isValidRoomId(roomId)) return socket.emit('error', { error: 'Invalid room ID' });

    const room = rooms[roomId];
    if (!room) return socket.emit('rejoinFailed', { reason: 'Room no longer exists' });
    if (!room.checkPassword(pwd)) return socket.emit('rejoinFailed', { reason: 'Incorrect password' });

    // Check for a pending disconnect grace period for this player
    const key = `${roomId}:${name}`;
    const pending = pendingDisconnects.get(key);

    if (!pending) {
      // Check if this is a spectator trying to re-spectate (no grace period needed)
      if (payload.isSpectator && room.state !== GameState.WAITING_FOR_PLAYERS) {
        if (!room.checkPassword(pwd)) return socket.emit('rejoinFailed', { reason: 'Incorrect password' });
        if (!room.addSpectator(socket.id)) return socket.emit('rejoinFailed', { reason: 'Spectator slots full' });
        playerToRoom[socket.id] = roomId;
        socket.join(roomId);
        socket.emit('spectatorJoined', {
          roomId: room.roomId,
          players: room.getPlayerList(),
          state: room.getState(),
          timeLimit: room.timeLimit,
          playerTimeLeft: room.playerTimeLeft,
          turnStartedAt: room.turnStartedAt,
          currentTurn: room.currentTurn,
          boards: room.getSpectatorBoards(),
          sunkShipData: room.getSunkShipData(),
          chatHistory: room.chatMessages || [],
          serverNow: Date.now(),
        });
        io.to(roomId).emit('spectatorUpdate', { count: room.spectators.size });
        room.touch();
        return;
      }
      return socket.emit('rejoinFailed', { reason: 'Session expired' });
    }

    // Cancel the grace timer — player made it back in time
    clearTimeout(pending.timer);
    pendingDisconnects.delete(key);
    const oldSocketId = pending.oldSocketId;

    // Acquire room lock — rejoin mutates room state and must not race with shots/timers
    const release = await acquireRoomLock(roomId);
    try {
      // Re-check room still exists after lock
      const freshRoom = rooms[roomId];
      if (!freshRoom) return socket.emit('rejoinFailed', { reason: 'Room no longer exists' });

      // Swap the old socket ID for the new one in the room
      if (freshRoom.players[oldSocketId]) {
        freshRoom.players[socket.id] = { ...freshRoom.players[oldSocketId], id: socket.id };
        delete freshRoom.players[oldSocketId];

        // Update host reference if needed
        if (freshRoom.hostId === oldSocketId) freshRoom.hostId = socket.id;

        // Swap in time-left map
        if (freshRoom.playerTimeLeft[oldSocketId] !== undefined) {
          freshRoom.playerTimeLeft[socket.id] = freshRoom.playerTimeLeft[oldSocketId];
          delete freshRoom.playerTimeLeft[oldSocketId];
        }

        // Swap current turn reference
        if (freshRoom.currentTurn === oldSocketId) freshRoom.currentTurn = socket.id;
        if (freshRoom.winner === oldSocketId) freshRoom.winner = socket.id;
        if (freshRoom.firstPlayer === oldSocketId) freshRoom.firstPlayer = socket.id;

        // Update play-again votes
        if (freshRoom.playAgainVotes.has(oldSocketId)) {
          freshRoom.playAgainVotes.delete(oldSocketId);
          freshRoom.playAgainVotes.add(socket.id);
        }

        // Swap rate limiters
        freshRoom.shotLimiter.removePlayer(oldSocketId);
        freshRoom.chatLimiter.removePlayer(oldSocketId);
        freshRoom.placementLimiter.removePlayer(oldSocketId);
      }

      // Update global tracking
      delete playerToRoom[oldSocketId];
      playerToRoom[socket.id] = roomId;
      socket.join(roomId);

      // Send full state restore to the reconnected client
      const state = freshRoom.getState();
      const opponentId = freshRoom.getOpponentId(socket.id);
      const opponent = opponentId ? freshRoom.players[opponentId] : null;

      socket.emit('rejoinSuccess', {
        playerId: socket.id,
        roomId: freshRoom.roomId,
        players: freshRoom.getPlayerList(),
        board: freshRoom.getPlayerBoard(socket.id),
        opponentBoard: opponentId ? freshRoom.getPlayerBoard(opponentId)?.map(row =>
          row.map(cell => (cell === 'S' ? 'W' : cell))
        ) : null,
        state,
        timeLimit: freshRoom.timeLimit,
        isHost: freshRoom.isHost(socket.id),
        currentTurn: freshRoom.currentTurn,
        playerTimeLeft: freshRoom.playerTimeLeft,
        turnStartedAt: freshRoom.turnStartedAt,
        winner: freshRoom.winner,
        chatHistory: freshRoom.chatMessages || [],
        opponentName: opponent?.name || null,
        shipsPlaced: freshRoom.players[socket.id]?.shipsPlaced || false,
        serverNow: Date.now(),
      });

      // Notify opponent the player is back
      if (opponentId) {
        io.to(opponentId).emit('opponentReconnected', { 
          playerName: name,
          playerId: socket.id,
        });
      }

      freshRoom.touch();
      console.log(`Rejoin successful: ${name} (${oldSocketId} -> ${socket.id}) in room ${roomId}`);
    } finally {
      release();
    }
  });

  socket.on('finishPlacement', async (payload) => {
    if (!payload || typeof payload !== 'object') return;
    const { ships } = payload;
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
    const room = rooms[roomId];
    if (!room || room.state !== GameState.PLACEMENT_PHASE) return;
    if (!room.players[socket.id]) return;
    // Prevent re-submission if already ready
    if (room.players[socket.id].shipsPlaced) return;

    // ── Anti-cheat: strict fleet composition check ──
    if (!Array.isArray(ships) || ships.length !== SHIPS.length) return;

    // Validate that submitted ship lengths match the expected fleet exactly
    const expectedLengths = [...SHIPS].sort((a, b) => a - b);
    const submittedLengths = ships
      .filter(s => s && typeof s.length === 'number' && Number.isInteger(s.length))
      .map(s => s.length)
      .sort((a, b) => a - b);
    if (submittedLengths.length !== expectedLengths.length ||
        !submittedLengths.every((len, i) => len === expectedLengths[i])) {
      return socket.emit('error', { error: 'Invalid fleet composition' });
    }

    room.players[socket.id].ships = [];
    room.players[socket.id].board = room._createEmptyBoard();

    for (const ship of ships) {
      if (!ship || typeof ship.row !== 'number' || typeof ship.col !== 'number' ||
        typeof ship.length !== 'number' || typeof ship.direction !== 'string') {
        // Reset on any malformed ship — prevents partial placement exploits
        room.players[socket.id].ships = [];
        room.players[socket.id].board = room._createEmptyBoard();
        return socket.emit('error', { error: 'Invalid ship data' });
      }
      // Enforce integer coordinates (prevent float exploits)
      if (!Number.isInteger(ship.row) || !Number.isInteger(ship.col) || !Number.isInteger(ship.length)) {
        room.players[socket.id].ships = [];
        room.players[socket.id].board = room._createEmptyBoard();
        return socket.emit('error', { error: 'Coordinates must be integers' });
      }
      // Use predefined ship name from constants — don't accept user-supplied names
      const shipIndex = room.players[socket.id].ships.length;
      const shipName = SHIP_NAMES[shipIndex] || 'Ship';
      const placed = room.placeShip(socket.id, ship.row, ship.col, ship.length, ship.direction, shipName);
      if (!placed) {
        // Atomic: if any ship fails, reset all — prevents partial fleet exploits
        room.players[socket.id].ships = [];
        room.players[socket.id].board = room._createEmptyBoard();
        return socket.emit('error', { error: 'Invalid ship placement — all ships reset' });
      }
    }

    if (!room.hasPlayerPlacedAllShips(socket.id)) return;

    if (room.markPlayerReady(socket.id)) {
      const p1 = room.getPlayerIds()[0];
      const p2 = room.getPlayerIds()[1];
      const battleData = { currentTurn: room.currentTurn, playerTimeLeft: room.playerTimeLeft, turnStartedAt: room.turnStartedAt, timeLimit: room.timeLimit, serverNow: Date.now() };
      io.to(p1).emit('battleStarted', { ...battleData, playerBoard: room.getPlayerBoard(p1) });
      io.to(p2).emit('battleStarted', { ...battleData, playerBoard: room.getPlayerBoard(p2) });
      // Notify spectators
      room.spectators.forEach(sid => {
        io.to(sid).emit('spectatorBattleStarted', { ...battleData, boards: room.getSpectatorBoards() });
      });
    } else {
      socket.emit('placementFinished');
      socket.to(roomId).emit('playerReady', { playerId: socket.id });
    }
    room.touch();
    } finally {
      release();
    }
  });

  socket.on('unreadyPlacement', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId) return;
    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room || room.state !== GameState.PLACEMENT_PHASE) return;
      if (!room.players[socket.id]) return;
      room.players[socket.id].shipsPlaced = false;
      socket.emit('placementUnreadied');
      socket.to(roomId).emit('playerUnreadied', { playerId: socket.id });
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('shoot', async (payload) => {
    // Defensive: ensure payload is valid
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return;
    const row = payload.row;
    const col = payload.col;

    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    // Serialise shots per room to prevent race conditions
    const release = await acquireRoomLock(roomId);
    try {
      // Fresh read after acquiring lock — room may have been deleted or state changed
      const room = rooms[roomId];
      if (!room || room.state !== GameState.BATTLE_PHASE) return;

      // Validate coordinates inside lock — after rate limit in processShot
      if (!Number.isInteger(row) || !Number.isInteger(col)) return;
      if (row < 0 || row >= 10 || col < 0 || col >= 10) return;

      const shot = room.processShot(socket.id, row, col);
      if (!shot.success) return socket.emit('error', { error: shot.error });

      // Record win in database (works for both authenticated and guest players)
      if (shot.result.gameWon && room.winner) {
        const winnerUid = room.players[room.winner]?.firebaseUid || null;
        const loserId = room.getOpponentId(room.winner);
        const loserUid = room.players[loserId]?.firebaseUid || null;
        const winnerName = winnerUid ? null : (room.players[room.winner]?.name || null);
        const loserName = loserUid ? null : (room.players[loserId]?.name || null);
        recordGameResult(winnerUid, loserUid, winnerName, loserName);
      }

      const opponentId = room.getOpponentId(socket.id);
      const result = shot.result;
      const serverNow = Date.now();

      io.to(socket.id).emit('shotResult', {
        ...result,
        winner: result.gameWon ? room.winner : undefined,
        currentTurn: room.currentTurn,
        shooterId: socket.id,
        playerBoard: room.getPlayerBoard(socket.id),
        opponentBoard: room.getOpponentViewBoard(socket.id),
        serverNow,
      });

      if (opponentId) {
        io.to(opponentId).emit('shotResult', {
          ...result,
          winner: result.gameWon ? room.winner : undefined,
          currentTurn: room.currentTurn,
          shooterId: socket.id,
          playerBoard: room.getPlayerBoard(opponentId),
          opponentBoard: room.getOpponentViewBoard(opponentId),
          serverNow,
        });
      }
      // Notify spectators
      room.spectators.forEach(sid => {
        io.to(sid).emit('spectatorShotResult', {
          ...result,
          currentTurn: room.currentTurn,
          winner: result.gameWon ? room.winner : undefined,
          shooterId: socket.id,
          boards: room.getSpectatorBoards(),
          serverNow,
        });
      });
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('requestPlayAgain', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
    const room = rooms[roomId];
    if (!room || room.state !== GameState.GAME_OVER) return;
    if (!room.players[socket.id]) return;

    // If opponent already left, reset this player to waiting instead of hanging
    if (room.getPlayerIds().length < 2) {
      room.playAgainVotes.clear();
      room.resetToWaiting(socket.id);
      socket.emit('opponentLeft', { isHost: room.isHost(socket.id) });
      room.touch();
      return;
    }

    room.playAgainVotes.add(socket.id);
    socket.to(roomId).emit('playAgainRequested', { requesterName: room.players[socket.id]?.name });

    if (room.playAgainVotes.size === 2 && room.getPlayerIds().length === 2) {
      room._transitionState(GameState.PLACEMENT_PHASE);
      room.winner = null;
      room.currentTurn = null;
      room.playAgainVotes.clear();
      room.playerTimeLeft = {};
      room.turnStartedAt = null;
      // Reset rate limiters and player state for fresh game
      room.getPlayerIds().forEach(pid => {
        room.shotLimiter.removePlayer(pid);
        room.placementLimiter.removePlayer(pid);
        room.chatLimiter.removePlayer(pid);
        room.players[pid].board = room._createEmptyBoard();
        room.players[pid].ships = [];
        room.players[pid].shipsPlaced = false;
        room.players[pid].totalHitsReceived = 0;
      });
      io.to(roomId).emit('gameReset', { timeLimit: room.timeLimit });
    }
    room.touch();
    } finally {
      release();
    }
  });

  socket.on('declinePlayAgain', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room || room.state !== GameState.GAME_OVER) return;
      if (!room.players[socket.id]) return; // spectators can't decline
      socket.to(roomId).emit('playAgainDeclined', { declinerName: room.players[socket.id]?.name });
      room.playAgainVotes.delete(socket.id);
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('forfeit', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
      // Fresh read after acquiring lock — room may have been deleted
      const room = rooms[roomId];
      if (!room || room.state !== GameState.BATTLE_PHASE) return;
      if (!room.players[socket.id]) return;
      const opponentId = room.getOpponentId(socket.id);
      room.winner = opponentId;
      room._transitionState(GameState.GAME_OVER);
      // Record forfeit result for authenticated and guest players
      const winnerUid = room.players[opponentId]?.firebaseUid || null;
      const loserUid = room.players[socket.id]?.firebaseUid || null;
      const winnerName = winnerUid ? null : (room.players[opponentId]?.name || null);
      const loserName = loserUid ? null : (room.players[socket.id]?.name || null);
      recordGameResult(winnerUid, loserUid, winnerName, loserName);
      io.to(roomId).emit('gameForfeited', {
        winner: opponentId,
        forfeiterId: socket.id,
        forfeiterName: room.players[socket.id]?.name
      });
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('leaveRoom', async () => {
    await handlePlayerLeave(socket.id, io);
    socket.emit('leftRoom');
  });

  socket.on('hostStartGame', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room) return;
      
      const result = room.startGameByHost(socket.id);
      if (!result.success) {
        return socket.emit('error', { error: result.error });
      }
      
      // Notify both players that the game has started
      io.to(roomId).emit('gameStartedByHost', {
        state: room.getState(),
      });
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('kickPlayer', async (payload) => {
    if (!payload || typeof payload !== 'object') return;
    const { targetId } = payload;
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room) return;

      // Only allow kicks during waiting phase
      if (room.state !== GameState.WAITING_FOR_PLAYERS) {
        return socket.emit('error', { error: 'Can only kick players in the waiting room' });
      }

      const result = room.kickPlayer(socket.id, targetId);
      if (!result.success) {
        return socket.emit('error', { error: result.error });
      }

      // Clean up rate limiters for the kicked player
      room.shotLimiter.removePlayer(targetId);
      room.chatLimiter.removePlayer(targetId);
      room.placementLimiter.removePlayer(targetId);

      // Notify the kicked player
      io.to(targetId).emit('kicked', { message: 'You have been kicked from the room' });
      
      // Remove the player's room membership
      delete playerToRoom[targetId];
      
      // Cancel any pending disconnect grace timer for the kicked player
      for (const [key, entry] of pendingDisconnects) {
        if (entry.oldSocketId === targetId && entry.roomId === roomId) {
          clearTimeout(entry.timer);
          pendingDisconnects.delete(key);
        }
      }
      
      // Make them leave the socket room
      const kickedSocket = io.sockets.sockets.get(targetId);
      if (kickedSocket) {
        kickedSocket.leave(roomId);
      }
      
      // Notify remaining players
      socket.emit('playerKicked', { targetId });
      room.touch();
    } finally {
      release();
    }
  });

  socket.on('sendChat', async (payload) => {
    if (!payload || typeof payload !== 'object') return;
    const { message, isImportant } = payload;
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) return;

    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room) return;
      const isSpec = room.spectators.has(socket.id);
      // Spectators cannot send important (broadcast) messages
      const senderName = isSpec ? 'Spectator' : (room.players[socket.id]?.name || 'Anonymous');
      const sanitized = sanitizeInput(typeof message === 'string' ? message : '', 200);
      if (!sanitized) return;
      const msg = room.addChatMessage(socket.id, senderName, sanitized, isSpec ? false : !!isImportant);
      if (!msg) return;
      io.to(roomId).emit('chatMessage', msg);
    } finally {
      release();
    }
  });

  socket.on('disconnect', async () => {
    const roomId = playerToRoom[socket.id];
    if (!roomId || !rooms[roomId]) {
      // No room or room gone — leave immediately
      await handlePlayerLeave(socket.id, io);
      console.log(`Disconnected: ${socket.id}`);
      return;
    }

    // Acquire lock to safely read room state and set up grace period
    const release = await acquireRoomLock(roomId);
    try {
      const room = rooms[roomId];
      if (!room || !room.players[socket.id] || room.spectators.has(socket.id)) {
        // Spectator or already removed — leave immediately (inside lock for consistency)
        await handlePlayerLeave(socket.id, io);
        console.log(`Disconnected: ${socket.id}`);
        return;
      }

      const playerName = room.players[socket.id].name;
      const key = `${roomId}:${playerName}`;
      console.log(`Disconnect (grace period ${DISCONNECT_GRACE_MS}ms): ${socket.id} from room ${roomId}`);

      // Cancel any existing grace timer for the same player (prevents stale timer leak)
      const existingPending = pendingDisconnects.get(key);
      if (existingPending) {
        clearTimeout(existingPending.timer);
        pendingDisconnects.delete(key);
      }

      // Notify the opponent that this player is reconnecting
      const opponentId = room.getOpponentId(socket.id);
      if (opponentId) {
        io.to(opponentId).emit('opponentReconnecting', { playerName });
      }

      const oldSocketId = socket.id;
      const timer = setTimeout(async () => {
        pendingDisconnects.delete(key);
        // Notify opponent grace period expired — re-read room in case it changed
        const currentRoom = rooms[roomId];
        if (currentRoom && currentRoom.players[oldSocketId]) {
          const opp = currentRoom.getOpponentId(oldSocketId);
          if (opp) io.to(opp).emit('opponentReconnectFailed', { playerName });
        }
        await handlePlayerLeave(oldSocketId, io);
        console.log(`Grace period expired — removed ${oldSocketId} from room ${roomId}`);
      }, DISCONNECT_GRACE_MS);

      pendingDisconnects.set(key, { timer, oldSocketId, roomId });

      // Cap pendingDisconnects to prevent unbounded growth
      if (pendingDisconnects.size > 1000) {
        const oldest = pendingDisconnects.keys().next().value;
        const entry = pendingDisconnects.get(oldest);
        if (entry) { clearTimeout(entry.timer); pendingDisconnects.delete(oldest); }
      }
    } finally {
      release();
    }
    console.log(`Disconnected: ${socket.id}`);
  });
});

// ============================================================================
// HTTP API
// ============================================================================

app.get('/health', (req, res) => res.json({ status: 'ok', uptime: process.uptime() }));

app.get('/version', (req, res) => {
  res.json({
    name: pkg.name,
    version: pkg.version,
    node: process.version,
    uptime: Math.floor(process.uptime()),
    platform: process.platform,
    activeRooms: Object.keys(rooms).length,
    connectedSockets: io.engine?.clientsCount || 0,
    memoryMB: Math.round(process.memoryUsage().heapUsed / 1024 / 1024 * 10) / 10,
    author: 'Adrians Bergmanis',
  });
});

app.get('/rooms', (req, res) => {
  // Rate-limit room list queries per IP
  const clientIp = req.ip || req.connection?.remoteAddress || 'unknown';
  if (!roomsListTracker.isAllowed(clientIp)) {
    return res.status(429).json({ error: 'Too many requests. Please wait.' });
  }

  const activeRooms = Object.values(rooms)
    .filter(r => {
      const s = r.getState();
      return (s === GameState.WAITING_FOR_PLAYERS && !r.isFull()) ||
        s === GameState.PLACEMENT_PHASE ||
        s === GameState.BATTLE_PHASE;
    })
    .map(r => ({
      roomId: r.roomId,
      hostName: r.hostName,
      hasPassword: r.hasPassword(),
      createdAt: r.createdAt,
      state: r.getState(),
      playerCount: r.playerCount(),
      spectatorCount: r.spectators.size,
      timeLimit: r.timeLimit,
    }))
    .sort((a, b) => b.createdAt - a.createdAt)
    .slice(0, 50);
  res.json({ rooms: activeRooms });
});

app.get('/rooms/:id', (req, res) => {
  if (!isValidRoomId(req.params.id)) return res.json({ exists: false });
  const room = rooms[req.params.id.toUpperCase()];
  if (!room) return res.json({ exists: false });
  res.json({
    exists: true,
    state: room.getState(),
    hasPassword: room.hasPassword(),
    playerCount: room.playerCount(),
  });
});

app.post('/rooms/:id/check-password', (req, res) => {
  if (!isValidRoomId(req.params.id)) return res.status(404).json({ error: 'Room not found' });
  // Rate-limit PIN attempts per IP+room
  const clientIp = req.ip || req.connection?.remoteAddress || 'unknown';
  const rateLimitKey = `${clientIp}:${req.params.id.toUpperCase()}`;
  if (!pinAttemptTracker.isAllowed(rateLimitKey)) {
    return res.status(429).json({ error: 'Too many PIN attempts. Try again later.' });
  }

  const room = rooms[req.params.id.toUpperCase()];
  if (!room) return res.status(404).json({ error: 'Room not found' });
  const { password } = req.body;
  if (room.checkPassword(sanitizeInput(password, 3))) {
    res.json({ valid: true });
  } else {
    res.status(401).json({ valid: false, error: 'Incorrect PIN' });
  }
});

// ============================================================================
// CLEANUP & STARTUP
// ============================================================================

cleanupIntervalId = setInterval(() => {
  // Cleanup stale rooms — acquire lock per room to avoid racing with shots/rejoin
  Object.keys(rooms).forEach(id => {
    const room = rooms[id];
    const timeout = room.getState() === GameState.GAME_OVER ? ROOM_GAMEOVER_TIMEOUT_MS : ROOM_INACTIVE_TIMEOUT_MS;
    if (room.isInactive(timeout) || room.isEmpty()) {
      (async () => {
        const release = await acquireRoomLock(id);
        try {
          // Re-check after lock — room may have been touched or deleted
          const freshRoom = rooms[id];
          if (!freshRoom) return;
          const t = freshRoom.getState() === GameState.GAME_OVER ? ROOM_GAMEOVER_TIMEOUT_MS : ROOM_INACTIVE_TIMEOUT_MS;
          if (!freshRoom.isInactive(t) && !freshRoom.isEmpty()) return;

          // Broadcast room closure to all sockets in the room at once
          io.to(id).emit('roomClosed', { reason: 'Room closed due to inactivity' });
          // Remove all player/spectator sockets from the socket.io room
          freshRoom.getPlayerIds().forEach(pid => {
            const sock = io.sockets.sockets.get(pid);
            sock?.leave(id);
            delete playerToRoom[pid];
          });
          freshRoom.spectators.forEach(sid => {
            const sock = io.sockets.sockets.get(sid);
            sock?.leave(id);
            delete playerToRoom[sid];
          });
          console.log(`Cleaning up stale room: ${id}`);
          // Cancel any pending disconnect grace timers for this room
          for (const [key, entry] of pendingDisconnects) {
            if (entry.roomId === id) {
              clearTimeout(entry.timer);
              pendingDisconnects.delete(key);
            }
          }
          delete rooms[id];
          delete roomLocks[id];
        } catch (err) {
          console.error(`Cleanup error in room ${id}:`, err);
        } finally {
          release();
        }
      })().catch(err => console.error(`Cleanup lock error for room ${id}:`, err));
    }
  });
  // Cleanup expired rate-limit tracker entries
  ipConnectionTracker.cleanup();
  pinAttemptTracker.cleanup();
  roomsListTracker.cleanup();
  joinEventTracker.cleanup();
  // Cleanup orphaned room locks for rooms that no longer exist
  for (const lockId of Object.keys(roomLocks)) {
    if (!rooms[lockId]) delete roomLocks[lockId];
  }
  // Cleanup stale rate limiter entries inside active rooms
  for (const room of Object.values(rooms)) {
    room.shotLimiter.cleanup();
    room.chatLimiter.cleanup();
    room.placementLimiter.cleanup();
  }
}, ROOM_CLEANUP_INTERVAL_MS);

// Timer check — end games where the active player's clock runs out
timerIntervalId = setInterval(() => {
  for (const roomId of Object.keys(rooms)) {
    const room = rooms[roomId];
    if (!room || room.state !== GameState.BATTLE_PHASE || !room.isCurrentPlayerTimeUp()) continue;

    // Fire-and-forget per room; errors are caught individually
    (async () => {
      const release = await acquireRoomLock(roomId);
      try {
        // Fresh read after lock — room may have been deleted or state changed
        const freshRoom = rooms[roomId];
        if (!freshRoom || freshRoom.state !== GameState.BATTLE_PHASE || !freshRoom.isCurrentPlayerTimeUp()) return;

        const loser = freshRoom.currentTurn;
        const winner = freshRoom.getOpponentId(loser);
        freshRoom.winner = winner;
        freshRoom._transitionState(GameState.GAME_OVER);
        // Record timeout result for authenticated and guest players
        const winnerUid = winner ? freshRoom.players[winner]?.firebaseUid || null : null;
        const loserUid = loser ? freshRoom.players[loser]?.firebaseUid || null : null;
        const winnerName = winnerUid ? null : (winner ? freshRoom.players[winner]?.name || null : null);
        const loserName = loserUid ? null : (loser ? freshRoom.players[loser]?.name || null : null);
        recordGameResult(winnerUid, loserUid, winnerName, loserName);
        const data = { winner, loser, winnerName: winner ? freshRoom.players[winner]?.name : null };
        io.to(roomId).emit('timeUp', data);
      } catch (err) {
        console.error(`Timer error in room ${roomId}:`, err);
      } finally {
        release();
      }
    })();
  }
}, 1000);

// Initialize Firebase Admin SDK
initFirebase();

// Connect to MongoDB (optional — server works without it, auth features disabled)
const MONGO_URI = process.env.MONGODB_URI || process.env.MONGO_URI;
if (MONGO_URI) {
  mongoose.connect(MONGO_URI)
    .then(() => console.log('✅ MongoDB connected'))
    .catch(err => console.error('⚠️  MongoDB connection failed:', err.message, '— auth features disabled'));
} else {
  console.log('ℹ️  No MONGODB_URI set — running without database (guest-only mode)');
}

const PORT = process.env.PORT || 3001;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n🚢 Battleships Server is battle-ready!`);
  console.log(`📡 Listening on 0.0.0.0:${PORT}`);
  console.log(`🌍 Allowed Origins: ${ALLOWED_ORIGINS.join(', ')}`);
});

// ============================================================================
// GRACEFUL SHUTDOWN
// ============================================================================

function gracefulShutdown(signal) {
  console.log(`\n${signal} received — shutting down gracefully…`);
  // Stop background intervals immediately
  if (cleanupIntervalId) clearInterval(cleanupIntervalId);
  if (timerIntervalId) clearInterval(timerIntervalId);
  // Cancel all pending disconnect grace timers
  for (const [key, entry] of pendingDisconnects) {
    clearTimeout(entry.timer);
    pendingDisconnects.delete(key);
  }
  // Notify all connected clients
  io.emit('error', { error: 'Server is restarting. Please reconnect shortly.' });
  server.close(() => {
    console.log('HTTP server closed.');
    process.exit(0);
  });
  // Force exit after 10s if connections don't close
  setTimeout(() => { console.error('Forced exit after timeout'); process.exit(1); }, 10000);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

// Catch unhandled errors to prevent silent crashes
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Promise Rejection:', reason);
});
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
  // Give time for logs, then exit — the process manager (Railway/PM2) will restart
  setTimeout(() => process.exit(1), 1000);
});
