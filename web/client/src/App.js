import React, { useState, useEffect, useCallback, useMemo, useRef, lazy, Suspense } from 'react';
import { createPortal } from 'react-dom';
import './App.css';

// Constants & Utils
import { CELL, SHIPS } from './constants';
import {
  createEmptyBoard,
  getRoomFromURL,
  setURLRoom,
  getSurroundingKeys
} from './utils/gameHelpers';
import { playSound, setSoundEnabled as setSfxEnabled, disposeSounds } from './utils/sounds';
import { playPhaseMusic, stopAllMusic, setMusicEnabled, pauseMusic, resumeMusic, onTrackChange, getCurrentTrackName } from './utils/music';

// i18n
import { useI18n } from './i18n/I18nContext';

// Eagerly loaded — on the critical render path
import LoginView from './components/LoginView';
import WaitingRoom from './components/WaitingRoom';
import ConnectionOverlay from './components/ConnectionOverlay';

// Hooks
import useSocket from './hooks/useSocket';

// Lazy loaded — not needed until later game phases
const ShipPlacement = lazy(() => import('./components/ShipPlacement'));
const BattleField = lazy(() => import('./components/BattleField'));
const GameOver = lazy(() => import('./components/GameOver'));
const ChatBox = lazy(() => import('./components/ChatBox'));
const ProfileModal = lazy(() => import('./components/ProfileModal'));
const Leaderboard = lazy(() => import('./components/Leaderboard'));

/* ── Floating bubble background (hoisted to module scope — never re-computed) ── */
const BUBBLE_COUNT = 14;
const BUBBLE_PALETTE = [
    'rgba(59,130,246,0.12)',
    'rgba(139,92,246,0.12)',
    'rgba(16,185,129,0.10)',
    'rgba(236,72,153,0.10)',
    'rgba(251,191,36,0.08)',
];
const BUBBLE_DATA = Array.from({ length: BUBBLE_COUNT }, (_, i) => ({
    key: i,
    size: 18 + Math.random() * 60,
    left: `${Math.random() * 100}%`,
    duration: `${12 + Math.random() * 18}s`,
    delay: `${-Math.random() * 20}s`,
    color: BUBBLE_PALETTE[i % BUBBLE_PALETTE.length],
}));

const FloatingBubbles = React.memo(function FloatingBubbles() {
    return (
        <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
            {BUBBLE_DATA.map(b => (
                <div
                    key={b.key}
                    style={{
                        position: 'absolute',
                        bottom: '-20%',
                        left: b.left,
                        width: b.size,
                        height: b.size,
                        background: b.color,
                        borderRadius: '50%',
                        animationName: 'float',
                        animationTimingFunction: 'ease-in-out',
                        animationIterationCount: 'infinite',
                        animationDuration: b.duration,
                        animationDelay: b.delay,
                    }}
                />
            ))}
        </div>
    );
});

/* ── Background ships sailing across screen ── */
const SHIP_EMOJIS = ['🚢', '⛵', '🛥️', '🚤', '🛳️', '⛴️'];
const BG_SHIP_COUNT = 8;
const BG_SHIP_DATA = Array.from({ length: BG_SHIP_COUNT }, (_, i) => {
  const goRight = Math.random() > 0.5;
  const duration = 18 + Math.random() * 24;
  const top = 8 + Math.random() * 80;
  const size = 16 + Math.random() * 20;
  return {
    key: i,
    emoji: SHIP_EMOJIS[i % SHIP_EMOJIS.length],
    top: `${top}%`,
    size,
    duration: `${duration}s`,
    delay: `${-Math.random() * duration}s`,
    goRight,
  };
});

const BackgroundShips = React.memo(function BackgroundShips() {
  return (
    <div className="fixed inset-0 overflow-hidden pointer-events-none z-0" aria-hidden="true">
      {BG_SHIP_DATA.map(s => (
        <div
          key={s.key}
          style={{
            position: 'absolute',
            top: s.top,
            left: 0,
            fontSize: s.size,
            opacity: 0.07,
            animationName: s.goRight ? 'sailRight' : 'sailLeft',
            animationTimingFunction: 'linear',
            animationIterationCount: 'infinite',
            animationDuration: s.duration,
            animationDelay: s.delay,
            transform: s.goRight ? 'scaleX(1)' : 'scaleX(-1)',
          }}
        >
          {s.emoji}
        </div>
      ))}
    </div>
  );
});

/* ── Module-level constants ── */
const NOOP = () => {};
const ACTIVE_ROOM_SESSION_KEY = 'battleships-active-room-session';
const SOCKET_EVENTS = [
  'gameJoined', 'playerJoined', 'error', 'placementFinished', 'playerReady',
  'placementUnreadied', 'playerUnreadied', 'battleStarted', 'shotResult',
  'gameReset', 'playerLeft', 'opponentLeft', 'leftRoom', 'gameForfeited',
  'playAgainRequested', 'playAgainDeclined', 'chatMessage', 'gameSessionEnded',
  'spectatorJoined', 'spectatorShotResult', 'spectatorBattleStarted',
  'spectatorUpdate', 'timeUp', 'gameStartedByHost', 'kicked', 'playerKicked',
  'connect', 'rejoinSuccess', 'rejoinFailed',
  'opponentReconnecting', 'opponentReconnected', 'opponentReconnectFailed', 'roomClosed',
  'bombResult', 'spectatorBombResult',
];

function readActiveRoomSession(roomCode) {
  if (!roomCode) return null;
  try {
    const session = JSON.parse(sessionStorage.getItem(ACTIVE_ROOM_SESSION_KEY) || 'null');
    if (session?.roomId?.toUpperCase() !== roomCode.toUpperCase() || !session.sessionToken) return null;
    return session;
  } catch {
    return null;
  }
}

function saveActiveRoomSession(session) {
  try {
    sessionStorage.setItem(ACTIVE_ROOM_SESSION_KEY, JSON.stringify(session));
  } catch {
    // Storage may be unavailable in private browsing; transient reconnect remains supported.
  }
}

function clearActiveRoomSession() {
  try {
    sessionStorage.removeItem(ACTIVE_ROOM_SESSION_KEY);
  } catch {
    // Ignore storage access failures.
  }
}

function formatUptime(seconds) {
  if (!seconds && seconds !== 0) return '—';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m ${s}s`;
}

function App() {
  const { socket, isConnected, serverUrl } = useSocket();
  const { t, lang, setLang } = useI18n();
  const urlInfo = useMemo(() => getRoomFromURL(), []);
  const restoredSession = useMemo(() => readActiveRoomSession(urlInfo.roomCode), [urlInfo.roomCode]);

  // State
  const [gameId, setGameId] = useState(urlInfo.roomCode || restoredSession?.roomId || '');
  const [roomPassword, setRoomPassword] = useState(urlInfo.password || restoredSession?.password || '');
  const [playerName, setPlayerName] = useState(restoredSession?.playerName || '');
  const [phase, setPhase] = useState('login');
  const [loginView, setLoginView] = useState(urlInfo.roomCode ? 'join' : 'menu');
  const [playerBoard, setPlayerBoard] = useState(createEmptyBoard);
  const [opponentBoard, setOpponentBoard] = useState(createEmptyBoard);
  const [currentTurn, setCurrentTurn] = useState(null);
  const [playerId, setPlayerId] = useState(null);
  const [opponentName, setOpponentName] = useState('');
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [winner, setWinner] = useState(null);
  const [shipsPlaced, setShipsPlaced] = useState(0);
  const [clientPlacements, setClientPlacements] = useState([]);
  const [isReady, setIsReady] = useState(false);
  const [opponentReady, setOpponentReady] = useState(false);
  const [playAgainPending, setPlayAgainPending] = useState(false);
  const [opponentWantsPlayAgain, setOpponentWantsPlayAgain] = useState(false);
  const [placementKey, setPlacementKey] = useState(0);
  const [availableRooms, setAvailableRooms] = useState([]);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [roomNotFound, setRoomNotFound] = useState(false);
  const [roomIsFull, setRoomIsFull] = useState(false);
  const [roomHasPassword, setRoomHasPassword] = useState(false);
  const [createPassword, setCreatePassword] = useState('');
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [joinRoomCode, setJoinRoomCode] = useState('');
  const [joinRoomPin, setJoinRoomPin] = useState('');
  const [pendingJoin, setPendingJoin] = useState(null);
  const [explosionCells, setExplosionCells] = useState([]);
  /** Incremented whenever playerSunk or opponentSunk refs change, to invalidate display memos */
  const [sunkVersion, setSunkVersion] = useState(0);
  /** Count of ships sunk per side (incremented on each shipSunk event) */
  const [myShipsSunk, setMyShipsSunk] = useState(0);
  const [theirShipsSunk, setTheirShipsSunk] = useState(0);

  // Auth & New feature state
  const [user, setUser] = useState(null);
  const [firebaseAuthToken, setFirebaseAuthToken] = useState(null);
  const [showLeaderboard, setShowLeaderboard] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [bombUsed, setBombUsed] = useState(false);
  const [bombMode, setBombMode] = useState(false);
  
  // Listen for Firebase Auth state changes
  useEffect(() => {
    let unsubscribe = NOOP;
    import('./firebase').then(({ auth }) => {
      unsubscribe = auth.onAuthStateChanged(async (firebaseUser) => {
        if (firebaseUser) {
          const token = await firebaseUser.getIdToken();
          setFirebaseAuthToken(token);

          const apiBase = (serverUrl.startsWith('http') ? serverUrl : `http://${serverUrl}`);

          // Fetch backend profile data. A 401/403 means the token is invalid or the
          // server has not been bootstrapped yet; a 404 means the user record is
          // missing and must be created via the login endpoint.
          try {
            const profileUrl = `${apiBase}/api/profile`;
            const res = await fetch(profileUrl, {
              headers: { 'Authorization': `Bearer ${token}` }
            });

            if (res.ok) {
              const profile = await res.json();
              setUser(profile);
              return;
            }

            if (res.status === 401 || res.status === 403 || res.status === 404) {
              const loginRes = await fetch(`${apiBase}/api/auth/login`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
              });

              if (loginRes.ok) {
                setUser(await loginRes.json());
                return;
              }

              const loginText = await loginRes.text();
              console.warn('Auth bootstrap failed:', loginRes.status, loginText);
              return;
            }

            console.error('Failed to load user profile:', res.status, await res.text());
          } catch (err) {
            console.error('Failed to load user profile:', err);
          }
        } else {
          setUser(null);
          setFirebaseAuthToken(null);
        }
      });
    });
    return () => unsubscribe();
  }, [serverUrl]);

  const [theme, setTheme] = useState(() => localStorage.getItem('battleships-theme') || 'dark');
  const [soundEnabled, setSoundEnabled] = useState(() => {
    const val = localStorage.getItem('battleships-sound');
    if (val === null) return false; // Default: disabled until user enables
    return val === 'on';
  });
  const [musicEnabled, setMusicPref] = useState(() => {
    const val = localStorage.getItem('battleships-music');
    if (val === null) return false; // Default: disabled until user enables
    return val === 'on';
  });
  const [chatMessages, setChatMessages] = useState([]);
  const [chatOpen, setChatOpen] = useState(false);
  const [chatUnread, setChatUnread] = useState(0);
  const [isSpectator, setIsSpectator] = useState(false);
  const [currentTrackName, setCurrentTrackName] = useState(() => getCurrentTrackName());

  // Auto-clear messages after a timeout
  const messageTimerRef = useRef(null);
  const setMessageWithTimeout = useCallback((msg, type = 'info', duration = 3000) => {
    setMessage(msg);
    setMessageType(type);
    if (messageTimerRef.current) clearTimeout(messageTimerRef.current);
    if (msg && duration > 0) {
      messageTimerRef.current = setTimeout(() => { setMessage(''); setMessageType('info'); }, duration);
    }
  }, []);

  const fetchAbortRef = useRef(null);
  const [spectatorCount, setSpectatorCount] = useState(0);
  const [gameTimeLimit, setGameTimeLimit] = useState(300);
  const [playerTimeLeft, setPlayerTimeLeft] = useState({});
  const [turnStartedAt, setTurnStartedAt] = useState(null);
  const [lastShot, setLastShot] = useState(null);
  const [spectatorBoards, setSpectatorBoards] = useState([]);
  const [serverInfo, setServerInfo] = useState(null);
  const [headerPopover, setHeaderPopover] = useState(null);
  const [headerPopoverPosition, setHeaderPopoverPosition] = useState({ top: 48, right: 12 });
  const serverInfoBtnRef = useRef(null);
  const settingsBtnRef = useRef(null);
  const [isHost, setIsHost] = useState(false);
  const [opponentSocketId, setOpponentSocketId] = useState(null);

  // Refs for socket handlers to avoid stale closures
  const playerSunk = useRef(new Set());
  const opponentSunk = useRef(new Set());
  const hasConnectedOnceRef = useRef(false);
  const playerIdRef = useRef(null);
  const sessionTokenRef = useRef(null);
  const soundRef = useRef(soundEnabled);
  const chatOpenRef = useRef(false);
  const phaseRef = useRef('login');
  const winnerRef = useRef(null);
  const tRef = useRef(t);
  const playerLeftTimerRef = useRef(null);
  const explosionTimersRef = useRef([]);
  const shootPendingRef = useRef(false);
  const shootTimeoutRef = useRef(null);

  // Clear shoot timeout on unmount to prevent setting state on unmounted component
  useEffect(() => {
    return () => {
      if (shootTimeoutRef.current) {
        clearTimeout(shootTimeoutRef.current);
      }
    };
  }, []);
  const lastShotTimerRef = useRef(null);
  /** Per-player sunk-cell sets for spectator overlay (💀 + red bg) */
  const spectatorSunkMap = useRef(new Map());
  const joiningGameRef = useRef(false);
  const gameIdRef = useRef('');
  const roomPasswordRef = useRef('');
  const playerNameRef = useRef('');
  const opponentNameRef = useRef('');
  const isSpectatorRef = useRef(false);
  const savedSessionRejoinAttemptedRef = useRef(false);
  const [isJoining, setIsJoining] = useState(false);

  const rejoinSavedSession = useCallback(() => {
    if (!restoredSession || !socket?.connected || savedSessionRejoinAttemptedRef.current) return false;
    savedSessionRejoinAttemptedRef.current = true;
    joiningGameRef.current = true;
    setIsJoining(true);
    setGameId(restoredSession.roomId);
    setRoomPassword(restoredSession.password || '');
    setPlayerName(restoredSession.playerName);
    const payload = {
      gameId: restoredSession.roomId,
      playerName: restoredSession.playerName,
      password: restoredSession.password || null,
      sessionToken: restoredSession.sessionToken,
    };
    console.log('[client] emitting saved-session rejoin', payload);
    socket.emit('rejoinGame', payload);
    return true;
  }, [restoredSession, socket]);

  useEffect(() => {
    if (!headerPopover) return;
    const updatePosition = () => {
      const anchor = headerPopover === 'settings' ? settingsBtnRef.current : serverInfoBtnRef.current;
      if (!anchor) return;
      const rect = anchor.getBoundingClientRect();
      const panelWidth = 288;
      setHeaderPopoverPosition({
        top: rect.bottom + 8,
        right: Math.min(Math.max(window.innerWidth - rect.right, 12), window.innerWidth - panelWidth - 12),
      });
    };
    const closeOnEscape = event => {
      if (event.key === 'Escape') setHeaderPopover(null);
    };
    updatePosition();
    window.addEventListener('resize', updatePosition);
    window.addEventListener('scroll', updatePosition, true);
    window.addEventListener('keydown', closeOnEscape);
    return () => {
      window.removeEventListener('resize', updatePosition);
      window.removeEventListener('scroll', updatePosition, true);
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [headerPopover]);

  // Keep tRef always pointing to current translation function
  useEffect(() => { tRef.current = t; }, [t]);

  // Consolidated ref sync — keep refs in sync with state for use inside socket handlers
  useEffect(() => {
    playerIdRef.current = playerId;
    soundRef.current = soundEnabled;
    chatOpenRef.current = chatOpen;
    phaseRef.current = phase;
    winnerRef.current = winner;
    gameIdRef.current = gameId;
    roomPasswordRef.current = roomPassword;
    playerNameRef.current = playerName;
    opponentNameRef.current = opponentName;
    isSpectatorRef.current = isSpectator;
  }, [playerId, soundEnabled, chatOpen, phase, winner, gameId, roomPassword, playerName, opponentName, isSpectator]);

  // Persistence
  useEffect(() => {
    if (playerName) localStorage.setItem('battleships-name', playerName);
  }, [playerName]);

  useEffect(() => {
    localStorage.setItem('battleships-theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('battleships-sound', soundEnabled ? 'on' : 'off');
    setSfxEnabled(soundEnabled);
  }, [soundEnabled]);

  // Music persistence & phase-specific music
  useEffect(() => {
    localStorage.setItem('battleships-music', musicEnabled ? 'on' : 'off');
    setMusicEnabled(musicEnabled);
  }, [musicEnabled]);

  // Subscribe to track name changes
  useEffect(() => {
    onTrackChange(setCurrentTrackName);
  }, []);

  // Initialize sound & music modules on mount to match stored preferences
  useEffect(() => {
    setSfxEnabled(soundEnabled);
    setMusicEnabled(musicEnabled);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Play phase-appropriate background music (mirrors Android MusicManager)
  useEffect(() => {
    if (phase === 'gameOver') {
      // Determine victory vs defeat music
      playPhaseMusic(winnerRef.current === playerIdRef.current ? 'victory' : 'defeat');
    } else {
      // Normalize login and waiting to 'menu' to prevent restart on transition
      const musicPhase = (phase === 'login' || phase === 'waiting') ? 'menu' : phase;
      playPhaseMusic(musicPhase);
    }
  }, [phase]);

  // Prefetch explosion image when entering battle so it's cached before first sunk ship
  useEffect(() => {
    if (phase === 'battle') {
      const img = new Image();
      img.src = '/assets/ship-sink-explosion.webp';
    }
  }, [phase]);

  // Tab visibility: pause/resume music when switching tabs (mirrors Android onPause/onResume)
  useEffect(() => {
    const handleVisibility = () => {
      if (document.hidden) {
        pauseMusic();
      } else {
        resumeMusic();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, []);

  // Warn before closing tab during active game
  useEffect(() => {
    const active = phase === 'placement' || phase === 'battle';
    if (!active) return;
    const handler = (e) => { e.preventDefault(); };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [phase]);

  useEffect(() => {
    if (!restoredSession || !socket?.connected || savedSessionRejoinAttemptedRef.current) return;
    rejoinSavedSession();
  }, [restoredSession, socket, rejoinSavedSession]);

  // Load saved name on mount
  useEffect(() => {
    const saved = localStorage.getItem('battleships-name');
    if (saved && !playerName) setPlayerName(saved);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Fetch server info on mount
  useEffect(() => {
    if (!serverUrl) return;
    const ac = new AbortController();
    fetch(`${serverUrl}/version`, { signal: ac.signal })
      .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
      .then(data => setServerInfo(data))
      .catch(() => {});
    return () => ac.abort();
  }, [serverUrl]);

  // If opened via a URL room link, check up-front whether the room exists (and validate PIN if included)
  useEffect(() => {
    if (!urlInfo.roomCode || !serverUrl) return;
    const ac = new AbortController();
    fetch(`${serverUrl}/rooms/${urlInfo.roomCode}`, { signal: ac.signal })
      .then(r => r.json().catch(() => ({ exists: false })))
      .then(async data => {
        if (!data.exists) {
          setRoomNotFound(true);
        } else {
          setRoomHasPassword(data.hasPassword || false);

          // If the URL included a PIN, validate it immediately
          if (data.hasPassword && urlInfo.password) {
            try {
              const res = await fetch(`${serverUrl}/rooms/${urlInfo.roomCode}/check-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password: urlInfo.password }),
                signal: ac.signal,
              });
              const pinData = await res.json();
              if (!pinData.valid) {
                // Wrong PIN in URL — clear it so the input field appears
                setRoomPassword('');
                setMessageWithTimeout('❌ Incorrect PIN in link — please enter the correct PIN', 'error', 5000);
              }
              // If valid, roomPassword is already set from urlInfo, nothing to do
            } catch {
              // On network error just leave the PIN as-is
            }
          }

          if (restoredSession) {
            setRoomIsFull(false);
            return;
          }

          if (data.playerCount >= 2) {
            setRoomIsFull(true);
            // If room is full and no password is required, automatically join as spectator
            if (!data.hasPassword) {
              // Use pendingJoin so the socket-ready effect handles the emit when socket connects
              setPendingJoin({ roomId: urlInfo.roomCode, password: null, isSpectating: true, autoJoin: true });
            }
          }
        }
      })
      .catch((err) => { if (err.name !== 'AbortError') setRoomNotFound(true); });
    return () => ac.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [serverUrl]);

  // Auto-emit joinGame for spectators once socket is ready (avoids race condition on page load)
  useEffect(() => {
    if (!pendingJoin?.autoJoin || !socket || joiningGameRef.current) return;
    joiningGameRef.current = true;
    setIsJoining(true);
    setGameId(pendingJoin.roomId);
    socket.emit('joinGame', {
      gameId: pendingJoin.roomId,
      playerName: 'Spectator',
      password: pendingJoin.password || null,
      isCreating: false,
      isSpectating: true,
      timeLimit: gameTimeLimit,
      authToken: firebaseAuthToken,
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingJoin, socket]);

  const resetSunk = useCallback(() => {
    playerSunk.current = new Set();
    opponentSunk.current = new Set();
    setSunkVersion(v => v + 1);
    setMyShipsSunk(0);
    setTheirShipsSunk(0);
  }, []);

  const overlay = useCallback((rawBoard, sunkSet) => {
    if (sunkSet.size === 0) return rawBoard;
    return rawBoard.map((row, ri) =>
      row.map((cell, ci) => {
        const key = `${ri},${ci}`;
        if (sunkSet.has(key)) return CELL.SUNK;
        if (sunkSet.has(key + '_safe') && cell === CELL.WATER) return CELL.SAFE;
        return cell;
      })
    );
  }, []);

  const handleBackToMenu = useCallback(() => {
    if (playerLeftTimerRef.current) { clearTimeout(playerLeftTimerRef.current); playerLeftTimerRef.current = null; }
    joiningGameRef.current = false;
    setIsJoining(false);
    shootPendingRef.current = false;
    sessionTokenRef.current = null; // Clear stale session token
    clearActiveRoomSession();
    savedSessionRejoinAttemptedRef.current = false;
    setBombUsed(false); setBombMode(false);
    // Clear pending explosion timers to prevent stale state updates
    explosionTimersRef.current.forEach(clearTimeout);
    explosionTimersRef.current = [];
    setExplosionCells([]);
    socket?.emit('leaveRoom');
    stopAllMusic();
    setPhase('login'); setGameId(''); resetSunk();
    setLoginView('menu'); setRoomPassword(''); setCreatePassword('');
    setPendingJoin(null); // Clear stale pending join state
    setURLRoom(null);
    setOpponentName(''); setOpponentSocketId(null); setIsHost(false);
    setIsSpectator(false); setSpectatorCount(0); setSpectatorBoards([]); spectatorSunkMap.current.clear();
    setChatMessages([]); setChatOpen(false); setChatUnread(0);
    setTurnStartedAt(null); setPlayerTimeLeft({});
    setIsReady(false); setOpponentReady(false); // Prevent stale ready state on next join
    setPlayAgainPending(false); setOpponentWantsPlayAgain(false);
  }, [socket, resetSunk]);

  // Socket Handlers
  useEffect(() => {
    if (!socket) return;

    socket.on('gameJoined', (data) => {
      joiningGameRef.current = false;
      setIsJoining(false);
      setPendingJoin(null); // clear stale pendingJoin
      setPlayerId(data.playerId);
      playerIdRef.current = data.playerId;
      sessionTokenRef.current = data.sessionToken || null;
      saveActiveRoomSession({
        roomId: data.roomId || gameIdRef.current || '',
        password: data.password || roomPasswordRef.current || '',
        playerName: playerNameRef.current || playerName,
        sessionToken: sessionTokenRef.current,
      });
      setIsHost(data.isHost || false);
      setBombUsed(data.bombUsed || false); setBombMode(false);
      // Cancel any pending playerLeft timer from a previous session
      if (playerLeftTimerRef.current) { clearTimeout(playerLeftTimerRef.current); playerLeftTimerRef.current = null; }
      const roomCode = data.roomId || '';
      const roomPwd = data.password || '';
      setGameId(roomCode);
      setRoomPassword(roomPwd);
      setURLRoom(roomCode, roomPwd || null);
      setShipsPlaced(0);
      setClientPlacements([]);
      resetSunk();
      setIsReady(false);
      setOpponentReady(false);
      setPlayerBoard(data.board || createEmptyBoard());
      if (data.timeLimit) setGameTimeLimit(data.timeLimit);
      // Restore chat history from server (don't clear existing chat)
      if (data.chatHistory && data.chatHistory.length > 0) {
        setChatMessages(data.chatHistory.slice(-100).map(msg => ({
          ...msg,
          isMine: msg.senderId === data.playerId,
        })));
      }
      setChatUnread(0);
      setIsSpectator(false);
      if (data.players.length === 2) {
        const opp = data.players.find(p => p.id !== data.playerId);
        setOpponentName(opp?.name || '');
        setOpponentSocketId(opp?.id || null);
        // If server is already in placement (e.g. reconnect), go to placement
        // Otherwise go to waiting and let the host start
        if (data.state === 'PLACEMENT_PHASE') {
          setPhase('placement');
          setMessageWithTimeout(tRef.current('msg.bothIn'), 'success');
        } else {
          setPhase('waiting');
          setMessageWithTimeout(tRef.current('msg.opJoined', opp?.name || ''), 'success');
        }
      } else {
        setOpponentName('');
        setOpponentSocketId(null);
        setPhase('waiting');
        setMessage('');
      }
    });

    socket.on('playerJoined', (data) => {
      const myId = playerIdRef.current;
      const opp = data.players?.find(p => p.id !== myId);
      if (opp) {
        setOpponentName(opp.name || '');
        setOpponentSocketId(opp.id || null);
        setMessageWithTimeout(tRef.current('msg.opJoined', opp.name || ''), 'success');
      }
    });

    socket.on('error', (data) => {
      joiningGameRef.current = false;
      setIsJoining(false);
      shootPendingRef.current = false;
      const errorMsg = data.error || tRef.current('app.unknownError');
      setMessageWithTimeout(`❌ ${errorMsg}`, 'error', 6000);
      if (errorMsg.includes('does not exist') || errorMsg.includes('Incorrect password') || errorMsg.includes('not found') || errorMsg.includes('is full')) {
        setLoginView('join');
        setGameId('');
        setRoomPassword('');
        setPendingJoin(null);
        setURLRoom(null);
      }
    });

    socket.on('placementFinished', () => {
      setIsReady(true);
      setMessageWithTimeout(tRef.current('msg.waitingPlace', opponentNameRef.current), 'info');
    });

    socket.on('playerReady', (data) => {
      if (data.playerId !== playerIdRef.current) setOpponentReady(true);
    });

    socket.on('placementUnreadied', () => {
      setIsReady(false);
      setMessageWithTimeout('', 'info', 0);
    });

    socket.on('playerUnreadied', (data) => {
      if (data.playerId !== playerIdRef.current) setOpponentReady(false);
    });

    socket.on('battleStarted', (data) => {
      setPhase('battle');
      setCurrentTurn(data.currentTurn);
      if (data.playerBoard) setPlayerBoard(data.playerBoard);
      setOpponentBoard(createEmptyBoard());
      resetSunk();
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      if (data.timeLimit) setGameTimeLimit(data.timeLimit);
      // Play turn notification if it's our turn first
      if (soundRef.current && data.currentTurn === playerIdRef.current) playSound('turn');
    });

    socket.on('shotResult', (data) => {
      shootPendingRef.current = false; // allow next shot
      if (shootTimeoutRef.current) { clearTimeout(shootTimeoutRef.current); shootTimeoutRef.current = null; }
      setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      const iShot = data.shooterId === playerIdRef.current;

      // Last shot animation — cancel previous timer to prevent early clear on rapid shots
      const shotBoard = iShot ? 'opponent' : 'player';
      if (lastShotTimerRef.current) clearTimeout(lastShotTimerRef.current);
      setLastShot({ row: data.row, col: data.col, board: shotBoard });
      lastShotTimerRef.current = setTimeout(() => { setLastShot(null); lastShotTimerRef.current = null; }, 500);

      // Sound effects
      if (soundRef.current) {
        if (data.shipSunk) playSound('sunk');
        else if (data.isHit) playSound('hit');
        else playSound('miss');
        // Play turn notification when it becomes our turn (opponent missed)
        if (!iShot && !data.isHit && data.currentTurn === playerIdRef.current) {
          setTimeout(() => playSound('turn'), 400);
        }
      }

      // Haptics
      if (data.isHit && !iShot) navigator.vibrate?.(200);
      if (data.shipSunk && !iShot) navigator.vibrate?.([100, 50, 200]);

      if (data.shipSunk && data.sunkShipCells?.length > 0) {
        const ref = iShot ? opponentSunk : playerSunk;
        data.sunkShipCells.forEach(c => ref.current.add(`${c.row},${c.col}`));
        getSurroundingKeys(data.sunkShipCells).forEach(k => ref.current.add(k + '_safe'));
        // Bump version to force display memo recalculation
        setSunkVersion(v => v + 1);
        // Increment ship-level counters
        if (iShot) setMyShipsSunk(c => c + 1);
        else setTheirShipsSunk(c => c + 1);

        const boardType = iShot ? 'opponent' : 'player';
        const newExplosions = data.sunkShipCells.map(c => ({
          row: c.row,
          col: c.col,
          board: boardType,
          id: `${boardType}-${c.row}-${c.col}-${Date.now()}`
        }));
        setExplosionCells(prev => [...prev, ...newExplosions]);
        const explosionTimer = setTimeout(() => {
          setExplosionCells(prev => prev.filter(e => !newExplosions.some(n => n.id === e.id)));
        }, 1500);
        explosionTimersRef.current.push(explosionTimer);
      }

      const raw_p = data.playerBoard || createEmptyBoard();
      const raw_o = data.opponentBoard || createEmptyBoard();
      setPlayerBoard(overlay(raw_p, playerSunk.current));
      setOpponentBoard(overlay(raw_o, opponentSunk.current));

      const shipName = data.sunkShipName || tRef.current('app.ship');
      const oppName = opponentNameRef.current;
      let msg = data.shipSunk
        ? (iShot ? tRef.current('msg.sunkTheir', shipName) : tRef.current('msg.yourSunk', shipName, oppName))
        : data.isHit
          ? (iShot ? tRef.current('msg.hit') : tRef.current('msg.theyHit'))
          : (iShot ? tRef.current('msg.miss', oppName) : tRef.current('msg.theyMissed'));
      setMessageWithTimeout(msg, data.isHit ? (iShot ? 'success' : 'error') : (iShot ? 'info' : 'success'), 5000);
      if (data.gameWon) {
        setBombMode(false);
        setPhase('gameOver');
        setWinner(data.winner);
        if (soundRef.current) playSound(data.winner === playerIdRef.current ? 'victory' : 'defeat');
      }
    });

    socket.on('gameReset', (data) => {
      shootPendingRef.current = false;
      setBombUsed(false); setBombMode(false);
      // Clear pending explosion timers to prevent stale updates  
      explosionTimersRef.current.forEach(clearTimeout);
      explosionTimersRef.current = [];
      setExplosionCells([]);
      setPhase('placement');
      setPlayerBoard(createEmptyBoard());
      setOpponentBoard(createEmptyBoard());
      setCurrentTurn(null);
      setWinner(null);
      setShipsPlaced(0);
      setClientPlacements([]);
      resetSunk();
      spectatorSunkMap.current.clear();
      setIsReady(false);
      setOpponentReady(false);
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      setPlacementKey(k => k + 1);
      setTurnStartedAt(null);
      setPlayerTimeLeft({});
      if (data?.timeLimit) setGameTimeLimit(data.timeLimit);
      setMessageWithTimeout(tRef.current('msg.newGame'), 'info');
    });

    socket.on('playerLeft', (data) => {
      setMessageWithTimeout(tRef.current('msg.playerLeft', data.playerName || opponentNameRef.current), 'info', 4000);
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      if (playerLeftTimerRef.current) clearTimeout(playerLeftTimerRef.current);
      playerLeftTimerRef.current = setTimeout(() => {
        setPhase('login'); setGameId(''); resetSunk();
        setURLRoom(null);
        playerLeftTimerRef.current = null;
      }, 2000);
    });

    socket.on('opponentLeft', (data) => {
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      setIsReady(false);
      setOpponentReady(false);
      setClientPlacements([]);
      setShipsPlaced(0);
      setPlacementKey(k => k + 1);
      setOpponentName('');
      setOpponentSocketId(null);
      if (data.isHost !== undefined) setIsHost(data.isHost);

      // Only reset to waiting if NOT in game over — game over is handled by gameSessionEnded
      setOpponentBoard(createEmptyBoard());
      setPlayerBoard(createEmptyBoard());
      resetSunk();
      setWinner(null);
      setTurnStartedAt(null);
      setPlayerTimeLeft({});
      setMessageWithTimeout(tRef.current('msg.opLeft', data.playerName || opponentNameRef.current), 'info', 4000);
      setPhase('waiting');
    });

    // Fired when opponent leaves after the game is already over — room is discarded
    socket.on('gameSessionEnded', (data) => {
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      setOpponentName('');
      setOpponentSocketId(null);
      setOpponentBoard(createEmptyBoard());
      setPlayerBoard(createEmptyBoard());
      resetSunk();
      setWinner(null);
      handleBackToMenu();
      setMessageWithTimeout(
        tRef.current('msg.opLeft', data.playerName || opponentNameRef.current),
        'info', 4000
      );
    });


    socket.on('leftRoom', () => {
      clearActiveRoomSession();
      savedSessionRejoinAttemptedRef.current = false;
      setChatMessages([]);
      setPhase('login'); setGameId(''); resetSunk();
      setURLRoom(null);
      setMessageWithTimeout('', 'info', 0);
    });

    socket.on('gameForfeited', (data) => {
      setPhase('gameOver');
      setWinner(data.winner);
      if (soundRef.current) playSound(data.winner === playerIdRef.current ? 'victory' : 'defeat');
      const iForfeited = data.forfeiterId === playerIdRef.current;
      setMessageWithTimeout(
        iForfeited ? tRef.current('msg.youSurrendered') : tRef.current('msg.opSurrendered', data.forfeiterName),
        iForfeited ? 'info' : 'success', 5000
      );
    });

    socket.on('playAgainRequested', (data) => {
      if (isSpectatorRef.current) {
        setMessageWithTimeout(tRef.current('gameover.rematchOffer', data?.requesterName || opponentNameRef.current), 'info', 6000);
      } else {
        setOpponentWantsPlayAgain(true);
      }
    });

    socket.on('playAgainDeclined', (data) => {
      if (isSpectatorRef.current) {
        setMessageWithTimeout(tRef.current('msg.declinedRematch', data?.declinerName || opponentNameRef.current), 'info', 5000);
      } else {
        setPlayAgainPending(false);
        setOpponentWantsPlayAgain(false);
        setMessageWithTimeout(tRef.current('msg.declinedRematch', opponentNameRef.current), 'error', 5000);
      }
    });

    socket.on('chatMessage', (msg) => {
      const isMine = msg.senderId === playerIdRef.current;
      setChatMessages(prev => [...prev, { ...msg, isMine }].slice(-100));
      if (msg.isImportant) {
        setMessageWithTimeout(`📢 ${msg.senderName}: ${msg.text}`, 'info', 5000);
      }
      if (!isMine && !chatOpenRef.current) {
        setChatUnread(prev => prev + 1);
        if (soundRef.current) playSound('chat');
      }
    });

    socket.on('spectatorJoined', (data) => {
      joiningGameRef.current = false;
      setIsJoining(false);
      setPendingJoin(null);
      const roomCode = data.roomId || '';
      setGameId(roomCode);
      setURLRoom(roomCode, null); // Spectator links usually shouldn't include password
      setIsSpectator(true);
      setSpectatorBoards(data.boards || []);
      setGameTimeLimit(data.timeLimit || 300);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      setCurrentTurn(data.currentTurn);
      // Populate sunk overlay for ships already sunk before spectator joined
      spectatorSunkMap.current.clear();
      if (data.sunkShipData) {
        data.sunkShipData.forEach(pd => {
          const sunkSet = new Set();
          pd.sunkShips.forEach(ship => {
            ship.cells.forEach(c => sunkSet.add(`${c.row},${c.col}`));
            getSurroundingKeys(ship.cells).forEach(k => sunkSet.add(k + '_safe'));
          });
          if (sunkSet.size > 0) spectatorSunkMap.current.set(pd.playerId, sunkSet);
        });
        setSunkVersion(v => v + 1);
      }
      if (data.state === 'BATTLE_PHASE') setPhase('battle');
      else if (data.state === 'PLACEMENT_PHASE') setPhase('placement');
      else if (data.state === 'GAME_OVER') setPhase('gameOver');
      if (data.chatHistory && data.chatHistory.length > 0) {
        setChatMessages(data.chatHistory.slice(-100).map(msg => ({
          ...msg,
          isMine: false, // Spectators can't send messages as themselves in the same way (or if they do, senderId won't match)
        })));
      }
      const names = (data.players || []).map(p => p.name).join(' vs ');
      setMessageWithTimeout(tRef.current('msg.spectating', names), 'info', 5000);
    });

    socket.on('spectatorShotResult', (data) => {
      setSpectatorBoards(data.boards || []);
      setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);

      // Last shot animation for spectators
      const boards = data.boards || [];
      const targetBoardIdx = boards.findIndex(b => b.playerId !== data.shooterId);
      const shotBoard = targetBoardIdx === 0 ? 'player' : 'opponent';
      if (lastShotTimerRef.current) clearTimeout(lastShotTimerRef.current);
      setLastShot({ row: data.row, col: data.col, board: shotBoard });
      lastShotTimerRef.current = setTimeout(() => { setLastShot(null); lastShotTimerRef.current = null; }, 500);

      // Sunk ship overlay + explosion for spectators (💀 red bg instead of just 🔥)
      if (data.shipSunk && data.sunkShipCells?.length > 0) {
        const targetPlayerId = boards.find(b => b.playerId !== data.shooterId)?.playerId;
        if (targetPlayerId) {
          if (!spectatorSunkMap.current.has(targetPlayerId)) {
            spectatorSunkMap.current.set(targetPlayerId, new Set());
          }
          const sunkSet = spectatorSunkMap.current.get(targetPlayerId);
          data.sunkShipCells.forEach(c => sunkSet.add(`${c.row},${c.col}`));
          getSurroundingKeys(data.sunkShipCells).forEach(k => sunkSet.add(k + '_safe'));
          setSunkVersion(v => v + 1);
        }

        const boardType = targetBoardIdx === 0 ? 'player' : 'opponent';
        const newExplosions = data.sunkShipCells.map(c => ({
          row: c.row, col: c.col, board: boardType,
          id: `${boardType}-${c.row}-${c.col}-${Date.now()}`
        }));
        setExplosionCells(prev => [...prev, ...newExplosions]);
        const explosionTimer = setTimeout(() => {
          setExplosionCells(prev => prev.filter(e => !newExplosions.some(n => n.id === e.id)));
        }, 1500);
        explosionTimersRef.current.push(explosionTimer);
      }

      if (data.gameWon) { setPhase('gameOver'); setWinner(data.winner); }
    });

    // ── Bomb result handlers ──
    socket.on('bombResult', (data) => {
      shootPendingRef.current = false;
      if (shootTimeoutRef.current) { clearTimeout(shootTimeoutRef.current); shootTimeoutRef.current = null; }
      setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      const iShot = data.shooterId === playerIdRef.current;
      if (iShot) { setBombUsed(true); setBombMode(false); }

      // Sound effects
      if (soundRef.current) {
        const anyHit = data.cells?.some(c => c.isHit);
        const anySunk = data.cells?.some(c => c.shipSunk);
        if (anySunk) playSound('sunk');
        else if (anyHit) playSound('hit');
        else playSound('miss');
        if (!iShot && data.currentTurn === playerIdRef.current) {
          setTimeout(() => playSound('turn'), 400);
        }
      }

      // Haptics for opponent's bomb
      if (!iShot) {
        const anyHit = data.cells?.some(c => c.isHit);
        if (anyHit) navigator.vibrate?.([100, 50, 100, 50, 200]);
      }

      // Process each cell for sunk ship overlays and explosions
      if (data.cells) {
        for (const cell of data.cells) {
          if (cell.shipSunk && cell.sunkShipCells?.length > 0) {
            const ref = iShot ? opponentSunk : playerSunk;
            cell.sunkShipCells.forEach(c => ref.current.add(`${c.row},${c.col}`));
            getSurroundingKeys(cell.sunkShipCells).forEach(k => ref.current.add(k + '_safe'));
            setSunkVersion(v => v + 1);
            if (iShot) setMyShipsSunk(c => c + 1);
            else setTheirShipsSunk(c => c + 1);

            const boardType = iShot ? 'opponent' : 'player';
            const newExplosions = cell.sunkShipCells.map(c => ({
              row: c.row, col: c.col, board: boardType,
              id: `${boardType}-${c.row}-${c.col}-${Date.now()}`
            }));
            setExplosionCells(prev => [...prev, ...newExplosions]);
            const explosionTimer = setTimeout(() => {
              setExplosionCells(prev => prev.filter(e => !newExplosions.some(n => n.id === e.id)));
            }, 1500);
            explosionTimersRef.current.push(explosionTimer);
          }
        }
      }

      const raw_p = data.playerBoard || createEmptyBoard();
      const raw_o = data.opponentBoard || createEmptyBoard();
      setPlayerBoard(overlay(raw_p, playerSunk.current));
      setOpponentBoard(overlay(raw_o, opponentSunk.current));

      // Message
      const hitCount = data.cells?.filter(c => c.isHit).length || 0;
      const sunkCount = data.cells?.filter(c => c.shipSunk).length || 0;
      let msg;
      if (iShot) {
        msg = sunkCount > 0 ? `💣 Bomb hit! ${hitCount} hits, ${sunkCount} ship(s) sunk!` : hitCount > 0 ? `💣 Bomb hit! ${hitCount} cell(s) hit!` : '💣 Bomb missed everything!';
      } else {
        msg = sunkCount > 0 ? `💣 ${opponentNameRef.current} used a bomb! ${hitCount} hits, ${sunkCount} ship(s) sunk!` : hitCount > 0 ? `💣 ${opponentNameRef.current} used a bomb! ${hitCount} cell(s) hit!` : `💣 ${opponentNameRef.current} used a bomb, but missed!`;
      }
      setMessageWithTimeout(msg, hitCount > 0 ? (iShot ? 'success' : 'error') : 'info', 5000);

      if (data.gameWon) {
        setBombMode(false);
        setPhase('gameOver');
        setWinner(data.winner);
        if (soundRef.current) playSound(data.winner === playerIdRef.current ? 'victory' : 'defeat');
      }
    });

    socket.on('spectatorBombResult', (data) => {
      setSpectatorBoards(data.boards || []);
      setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);

      if (data.cells) {
        const boards = data.boards || [];
        for (const cell of data.cells) {
          if (cell.shipSunk && cell.sunkShipCells?.length > 0) {
            const targetPlayerId = boards.find(b => b.playerId !== data.shooterId)?.playerId;
            if (targetPlayerId) {
              if (!spectatorSunkMap.current.has(targetPlayerId)) {
                spectatorSunkMap.current.set(targetPlayerId, new Set());
              }
              const sunkSet = spectatorSunkMap.current.get(targetPlayerId);
              cell.sunkShipCells.forEach(c => sunkSet.add(`${c.row},${c.col}`));
              getSurroundingKeys(cell.sunkShipCells).forEach(k => sunkSet.add(k + '_safe'));
              setSunkVersion(v => v + 1);
            }
          }
        }
      }

      if (data.gameWon) { setPhase('gameOver'); setWinner(data.winner); }
    });

    socket.on('spectatorBattleStarted', (data) => {
      spectatorSunkMap.current.clear();
      setPhase('battle');
      setSpectatorBoards(data.boards || []);
      setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      if (data.timeLimit) setGameTimeLimit(data.timeLimit);
    });

    socket.on('spectatorUpdate', (data) => {
      setSpectatorCount(data.count || 0);
    });

    socket.on('timeUp', (data) => {
      setPhase('gameOver');
      setWinner(data.winner);
      const myId = playerIdRef.current;
      const iLost = data.loser === myId;
      const msg = iLost ? tRef.current('msg.yourTimeUp') : tRef.current('msg.opTimeUp', opponentNameRef.current);
      setMessageWithTimeout(msg, iLost ? 'error' : 'success', 5000);
      if (soundRef.current) playSound(data.winner === myId ? 'victory' : 'defeat');
    });

    socket.on('gameStartedByHost', (data) => {
      setPhase('placement');
      setIsReady(false);
      setOpponentReady(false);
      setMessageWithTimeout(tRef.current('msg.gameStarted'), 'success');
    });

    socket.on('kicked', (data) => {
      clearActiveRoomSession();
      savedSessionRejoinAttemptedRef.current = false;
      setMessageWithTimeout(`❌ ${data.message || 'You have been kicked from the room'}`, 'error', 5000);
      setChatMessages([]);
      setPhase('login');
      setLoginView('menu');
      setGameId('');
      setRoomPassword('');
      setURLRoom(null);
    });

    socket.on('playerKicked', (data) => {
      if (data.targetId) {
        setOpponentName('');
        setOpponentSocketId(null);
        setMessageWithTimeout(tRef.current('msg.playerKicked'), 'info');
      }
    });

    // ── Auto-rejoin after brief network interruption ──
    // Track whether this is the first connect (skip) or a reconnection (rejoin)
    if (!hasConnectedOnceRef.current && socket.connected) hasConnectedOnceRef.current = true;
    socket.on('connect', () => {
      if (!hasConnectedOnceRef.current) {
        hasConnectedOnceRef.current = true;
        if (restoredSession && !savedSessionRejoinAttemptedRef.current) {
          rejoinSavedSession();
        }
        return; // first connection — no need to rejoin
      }
      if (restoredSession && !savedSessionRejoinAttemptedRef.current) {
        rejoinSavedSession();
        return;
      }
      // Only auto-rejoin if we were in a game (not on login screen)
      const phase = phaseRef.current;
      const gId = gameIdRef.current;
      const pName = playerNameRef.current;
      if (phase !== 'login' && gId && pName) {
        setMessageWithTimeout('🔄 Reconnecting...', 'info', 0);
        socket.emit('rejoinGame', {
          gameId: gId,
          playerName: pName,
          password: roomPasswordRef.current || null,
          sessionToken: sessionTokenRef.current || null,
        });
      }
    });

    socket.on('rejoinSuccess', (data) => {
      joiningGameRef.current = false;
      setIsJoining(false);
      shootPendingRef.current = false;
      setPlayerId(data.playerId);
      playerIdRef.current = data.playerId;
      sessionTokenRef.current = data.sessionToken || sessionTokenRef.current || null;
      saveActiveRoomSession({
        roomId: data.roomId || gameIdRef.current || '',
        password: roomPasswordRef.current || '',
        playerName: playerNameRef.current || playerName,
        sessionToken: sessionTokenRef.current,
      });
      setIsHost(data.isHost || false);
      setBombUsed(data.bombUsed || false); setBombMode(false);
      setGameId(data.roomId);
      if (data.board) setPlayerBoard(data.board);
      if (data.opponentBoard) setOpponentBoard(data.opponentBoard);
      if (data.currentTurn) setCurrentTurn(data.currentTurn);
      if (data.playerTimeLeft) setPlayerTimeLeft(data.playerTimeLeft);
      if (data.turnStartedAt) setTurnStartedAt(data.serverNow ? Date.now() - (data.serverNow - data.turnStartedAt) : data.turnStartedAt);
      if (data.timeLimit) setGameTimeLimit(data.timeLimit);
      setWinner(data.winner || null);
      setOpponentName(data.opponentName || '');
      setOpponentSocketId(data.opponentSocketId || null);
      if (data.shipsPlaced) setIsReady(true);
      if (data.chatHistory?.length > 0) {
        setChatMessages(data.chatHistory.slice(-100).map(msg => ({
          ...msg,
          isMine: msg.senderId === data.playerId,
        })));
      }
      // Clear play-again state on rejoin
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      // Restore correct phase from server state
      const stateStr = data.state;
      if (stateStr === 'BATTLE_PHASE') setPhase('battle');
      else if (stateStr === 'PLACEMENT_PHASE') setPhase('placement');
      else if (stateStr === 'GAME_OVER') setPhase('gameOver');
      else if (stateStr === 'WAITING_FOR_PLAYERS') setPhase('waiting');

      setMessageWithTimeout('✅ Reconnected!', 'success', 3000);
    });

    socket.on('rejoinFailed', (data) => {
      console.log('[client] rejoinFailed', {
        reason: data?.reason,
        restoredSession,
        gameId: gameIdRef.current,
        playerName: playerNameRef.current,
        roomPassword: roomPasswordRef.current,
        sessionToken: sessionTokenRef.current,
      });
      clearActiveRoomSession();
      savedSessionRejoinAttemptedRef.current = false;
      setMessageWithTimeout(`❌ ${data?.reason || 'Could not rejoin'}`, 'error', 5000);
      setPhase('login');
      setGameId('');
      setRoomPassword('');
      setURLRoom(null);
    });

    socket.on('opponentReconnecting', (data) => {
      setMessageWithTimeout(`⏳ ${data?.playerName || tRef.current('app.opponent')} lost connection, waiting...`, 'info', 0);
    });

    socket.on('opponentReconnected', (data) => {
      if (data?.playerId) setOpponentSocketId(data.playerId);
      setMessageWithTimeout(`✅ ${data?.playerName || tRef.current('app.opponent')} reconnected!`, 'success', 3000);
    });

    socket.on('opponentReconnectFailed', (data) => {
      setPlayAgainPending(false);
      setOpponentWantsPlayAgain(false);
      setMessageWithTimeout(`❌ ${data?.playerName || tRef.current('app.opponent')} disconnected`, 'error', 5000);
    });

    socket.on('roomClosed', (data) => {
      setMessageWithTimeout(`⚠️ ${data?.reason || 'Room was closed'}`, 'error', 5000);
      handleBackToMenu();
    });

    return () => {
      if (playerLeftTimerRef.current) { clearTimeout(playerLeftTimerRef.current); playerLeftTimerRef.current = null; }
      if (messageTimerRef.current) { clearTimeout(messageTimerRef.current); messageTimerRef.current = null; }
      if (lastShotTimerRef.current) { clearTimeout(lastShotTimerRef.current); lastShotTimerRef.current = null; }
      explosionTimersRef.current.forEach(clearTimeout);
      explosionTimersRef.current = [];
      SOCKET_EVENTS.forEach(evt => socket.off(evt));
    };
  // setMessageWithTimeout is a stable useCallback — safe to omit
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [socket, overlay, resetSunk]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (messageTimerRef.current) clearTimeout(messageTimerRef.current);
      fetchAbortRef.current?.abort();
      disposeSounds();
      stopAllMusic();
    };
  }, []);

  // sunkVersion is an intentional cache-buster for ref-based sunk Sets
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const dispPlayer = useMemo(() => overlay(playerBoard, playerSunk.current), [playerBoard, overlay, sunkVersion]);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const dispOpponent = useMemo(() => overlay(opponentBoard, opponentSunk.current), [opponentBoard, overlay, sunkVersion]);

  /** Spectator boards with sunk ship overlay applied (💀 + safe zones) */
  const dispSpectatorBoards = useMemo(() => {
    if (spectatorBoards.length === 0) return spectatorBoards;
    return spectatorBoards.map(sb => {
      const sunkSet = spectatorSunkMap.current.get(sb.playerId);
      if (!sunkSet || sunkSet.size === 0) return sb;
      return { ...sb, board: overlay(sb.board, sunkSet) };
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [spectatorBoards, overlay, sunkVersion]);

  const fetchRooms = useCallback(async () => {
    fetchAbortRef.current?.abort();
    const ac = new AbortController();
    fetchAbortRef.current = ac;
    setLoadingRooms(true);
    try {
      const res = await fetch(`${serverUrl}/rooms`, { signal: ac.signal });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setAvailableRooms(data.rooms || []);
    } catch (err) {
      if (err.name === 'AbortError') return; // aborted — don't update state
      setAvailableRooms([]);
      setMessageWithTimeout(tRef.current('msg.failedRooms'), 'error', 5000);
    } finally {
      if (!ac.signal.aborted) setLoadingRooms(false);
    }
  }, [serverUrl, setMessageWithTimeout]);

  useEffect(() => {
    if (phase === 'login' && loginView === 'join') fetchRooms();
  }, [phase, loginView, fetchRooms]);

  const handleJoinGame = useCallback(async (e, opts = {}) => {
    if (e?.preventDefault) e.preventDefault();
    const id = (opts.roomId || joinRoomCode).trim();
    const isSpectating = opts.isSpectating || false;
    if (!id) { setMessageWithTimeout(tRef.current('msg.roomRequired'), 'error', 4000); return; }

    try {
      const res = await fetch(`${serverUrl}/rooms/${id}`);
      if (!res.ok) { setMessageWithTimeout(tRef.current('login.roomNotFound'), 'error', 4000); return; }
      const data = await res.json();
      if (!data.exists) { setMessageWithTimeout(tRef.current('login.roomNotFound'), 'error', 4000); return; }

      if (data.hasPassword) {
        setPendingJoin({ roomId: id, needsPassword: true, isSpectating });
        setJoinRoomPin('');
        setLoginView('enterPin');
      } else if (isSpectating) {
        // Spectators don't need a name — join immediately
        joiningGameRef.current = true;
        setIsJoining(true);
        setGameId(id);
        socket?.emit('joinGame', {
          gameId: id,
          playerName: 'Spectator',
          password: null,
          isCreating: false,
          isSpectating: true,
          timeLimit: gameTimeLimit,
          authToken: firebaseAuthToken,
        });
      } else {
        setPendingJoin({ roomId: id, password: null, isSpectating });
        setLoginView('enterName');
      }
    } catch {
      setMessageWithTimeout(tRef.current('login.failedValidate'), 'error', 4000);
    }
  }, [firebaseAuthToken, gameTimeLimit, joinRoomCode, serverUrl, setMessageWithTimeout, socket]);

  const handleSetLoginView = useCallback((view, join) => {
    if (view === 'enterName' && firebaseAuthToken) {
      // Logged-in: use profile name directly (or 'Player' if still loading) and join immediately
      const resolvedJoin = join || pendingJoin;
      if (!resolvedJoin) return;
      joiningGameRef.current = true;
      setIsJoining(true);
      setGameId(resolvedJoin.roomId);
      setRoomPassword(resolvedJoin.password || '');
      socket?.emit('joinGame', {
        gameId: resolvedJoin.roomId,
        playerName: user?.name || 'Player',
        password: resolvedJoin.password,
        isCreating: resolvedJoin.isCreating,
        isSpectating: resolvedJoin.isSpectating || false,
        timeLimit: resolvedJoin.timeLimit || gameTimeLimit,
        authToken: firebaseAuthToken,
      });
    } else {
      if (join) setPendingJoin(join);
      setLoginView(view);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, pendingJoin, socket, gameTimeLimit, firebaseAuthToken]);

  // Immediately join as anonymous spectator — no name required
  const handleJoinAsSpectator = useCallback((roomId, password = null) => {
    if (joiningGameRef.current) return;
    joiningGameRef.current = true;
    setIsJoining(true);
    setGameId(roomId);
    if (!socket) {
      // Socket not ready yet — store as pendingJoin so it fires on connect
      setPendingJoin({ roomId, password: password || null, isSpectating: true, autoJoin: true });
      joiningGameRef.current = false;
      setIsJoining(false);
      return;
    }
    socket.emit('joinGame', {
      gameId: roomId,
      playerName: 'Spectator',
      password: password || null,
      isCreating: false,
      isSpectating: true,
      timeLimit: gameTimeLimit,
      authToken: firebaseAuthToken,
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [socket, gameTimeLimit, firebaseAuthToken]);

  const handleFinalJoin = useCallback(() => {
    // For logged-in users, use their profile name (or 'Player' if still loading); for guests, use typed name
    const isSpectating = pendingJoin?.isSpectating || false;
    const rawName = isSpectating ? 'Spectator' : (firebaseAuthToken ? (user?.name || 'Player') : playerName);
    const name = rawName.trim();
    if (!isSpectating && !name) { setMessageWithTimeout(tRef.current('msg.nameRequired'), 'error', 4000); return; }
    if (!pendingJoin) return;
    if (joiningGameRef.current) return; // prevent double-submission
    joiningGameRef.current = true;
    setIsJoining(true);
    setGameId(pendingJoin.roomId);
    setRoomPassword(pendingJoin.password || '');
    socket?.emit('joinGame', {
      gameId: pendingJoin.roomId,
      playerName: name,
      password: pendingJoin.password,
      isCreating: pendingJoin.isCreating,
      isSpectating: pendingJoin.isSpectating || false,
      timeLimit: pendingJoin.timeLimit || gameTimeLimit,
      authToken: firebaseAuthToken,
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, playerName, pendingJoin, socket, gameTimeLimit, firebaseAuthToken]);

  const handleShipPlaced = useCallback((placements) => {
    setClientPlacements(placements);
    setShipsPlaced(placements.length);
  }, []);

  const handleFinishPlace = useCallback(() => {
    const ships = clientPlacements.map((p, i) => ({
      row: p.row, col: p.col, length: p.length, direction: p.direction,
      name: SHIPS[p.shipId]?.name || `Ship ${i + 1}`,
    }));
    socket?.emit('finishPlacement', { ships });
  }, [socket, clientPlacements]);

  const handleUnready = useCallback(() => socket?.emit('unreadyPlacement'), [socket]);

  const handleShoot = useCallback((r, c) => {
    if (shootPendingRef.current) return; // prevent double-fire
    if (socket && currentTurn === playerId) {
      shootPendingRef.current = true;
      socket.emit('shoot', { row: r, col: c });
      // Auto-reset if server never responds (e.g. connection lost mid-shot)
      if (shootTimeoutRef.current) clearTimeout(shootTimeoutRef.current);
      shootTimeoutRef.current = setTimeout(() => { shootPendingRef.current = false; }, 10000);
    }
  }, [socket, currentTurn, playerId]);

  const handleUseBomb = useCallback((r, c) => {
    if (shootPendingRef.current || bombUsed) return;
    if (socket && currentTurn === playerId) {
      shootPendingRef.current = true;
      setBombMode(false);
      socket.emit('useBomb', { row: r, col: c });
      if (shootTimeoutRef.current) clearTimeout(shootTimeoutRef.current);
      shootTimeoutRef.current = setTimeout(() => { shootPendingRef.current = false; }, 10000);
    }
  }, [socket, currentTurn, playerId, bombUsed]);

  const toggleBombMode = useCallback(() => {
    if (!bombUsed && currentTurn === playerId) {
      setBombMode(prev => !prev);
    }
  }, [bombUsed, currentTurn, playerId]);

  const handlePlayAgain = useCallback(() => {
    setPlayAgainPending(true);
    socket?.emit('requestPlayAgain');
  }, [socket]);

  const handleForfeit = useCallback(() => {
    if (window.confirm(t('battle.confirmSurrender'))) {
      socket?.emit('forfeit');
    }
  }, [socket, t]);

  const handleStartGame = useCallback(() => {
    if (!isHost) return;
    socket?.emit('hostStartGame');
  }, [socket, isHost]);

  const handleKickPlayer = useCallback(() => {
    if (!isHost || !opponentSocketId) return;
    if (window.confirm(t('waiting.confirmKick'))) {
      socket?.emit('kickPlayer', { targetId: opponentSocketId });
    }
  }, [socket, isHost, opponentSocketId, t]);

  const handleReturnToWaiting = useCallback(() => {
    setOpponentBoard(createEmptyBoard());
    setPlayerBoard(createEmptyBoard());
    resetSunk();
    setWinner(null);
    setTurnStartedAt(null);
    setPlayerTimeLeft({});
    setPhase('waiting');
  }, [resetSunk]);



  const sendChat = useCallback((text, isImportant = false) => {
    if (!text.trim() || !socket) return;
    socket.emit('sendChat', { message: text.trim(), isImportant });
  }, [socket]);

  const handleChatToggle = useCallback(() => {
    setChatOpen(prev => {
      if (!prev) setChatUnread(0);
      return !prev;
    });
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme(t => t === 'dark' ? 'light' : 'dark');
  }, []);

  const toggleSound = useCallback(() => {
    setSoundEnabled(s => !s);
  }, []);

  const toggleMusic = useCallback(() => {
    setMusicPref(m => !m);
  }, []);

  const handleDeclinePlayAgain = useCallback(() => {
    socket?.emit('declinePlayAgain');
    setPlayAgainPending(false);
    setOpponentWantsPlayAgain(false);
  }, [socket]);

  // Use module-level NOOP to avoid re-creating on every render

  const isMyTurn = currentTurn === playerId;
  const msgClass = useMemo(() => ({
    info: 'from-blue-500/20 to-blue-600/10 border-blue-500/40 text-blue-200 shadow-blue-900/20',
    success: 'from-emerald-500/20 to-emerald-600/10 border-emerald-500/40 text-emerald-200 shadow-emerald-900/20',
    error: 'from-red-500/20 to-red-600/10 border-red-500/40 text-red-200 shadow-red-900/20'
  }[messageType] || ''), [messageType]);

  const handleGoogleLogin = useCallback(async () => {
    try {
      const { signInWithGoogle } = await import('./firebase');
      await signInWithGoogle();
      // Auth state listener handles the rest
    } catch (err) {
      console.error(err);
      setMessageWithTimeout('Google Sign-In failed', 'error');
    }
  }, [setMessageWithTimeout]);

  const centeredPhases = ['login', 'waiting', 'gameOver'];
  const mainLayoutClass = centeredPhases.includes(phase) || (phase === 'placement' && isSpectator)
    ? 'flex-1 flex flex-col justify-center'
    : '';

  return (
    <div className="min-h-screen theme-bg text-white flex flex-col" data-theme={theme}>
      {/* Skip to main content — accessible keyboard shortcut */}
      <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:z-[200] focus:top-2 focus:left-2 focus:px-4 focus:py-2 focus:bg-blue-600 focus:text-white focus:rounded-lg focus:text-sm focus:font-bold">
        Skip to content
      </a>
      <ConnectionOverlay isConnected={isConnected} />

      {headerPopover && createPortal(
        <>
          <button
            type="button"
            className="fixed inset-0 z-[9998] cursor-default"
            aria-label={t('app.closePanel')}
            onClick={() => setHeaderPopover(null)}
          />
          <section
            className="header-popover fixed z-[9999] w-[min(18rem,calc(100vw-1.5rem))] rounded-2xl border border-slate-500/20"
            style={{ top: headerPopoverPosition.top, right: headerPopoverPosition.right }}
            aria-label={headerPopover === 'settings' ? t('app.settings') : t('serverInfo.title')}
          >
            {headerPopover === 'settings' ? (
              <div className="p-4 text-sm text-slate-200">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="font-bold">{t('app.settings')}</h2>
                  <span className="max-w-36 truncate text-xs text-slate-500">{musicEnabled && currentTrackName ? `♪ ${currentTrackName}` : ''}</span>
                </div>
                <label className="flex items-center justify-between gap-4 border-b border-white/10 py-3 text-sm">
                  <span>{t('app.language')}</span>
                  <select
                    value={lang}
                    onChange={e => setLang(e.target.value)}
                    className="rounded-lg border border-white/10 bg-slate-800 px-2 py-1.5 text-sm text-white"
                    aria-label={t('app.language')}
                  >
                    <option value="en">🇬🇧 English</option>
                    <option value="lv">🇱🇻 Latviešu</option>
                    <option value="ru">🇷🇺 Русский</option>
                  </select>
                </label>
                <button type="button" onClick={toggleSound} aria-pressed={soundEnabled} className="flex min-h-11 w-full items-center justify-between border-b border-white/10 py-3 text-left">
                  <span>{soundEnabled ? t('app.muteSound') : t('app.unmuteSound')}</span><span aria-hidden="true">{soundEnabled ? '🔊' : '🔇'}</span>
                </button>
                <button type="button" onClick={toggleMusic} aria-pressed={musicEnabled} className="flex min-h-11 w-full items-center justify-between border-b border-white/10 py-3 text-left">
                  <span>{musicEnabled ? t('app.muteMusic') : t('app.unmuteMusic')}</span><span aria-hidden="true">{musicEnabled ? '🎵' : '🔕'}</span>
                </button>
                <button type="button" onClick={toggleTheme} className="flex min-h-11 w-full items-center justify-between py-3 text-left">
                  <span>{t('app.toggleTheme')}</span><span aria-hidden="true">{theme === 'dark' ? '☀️' : '🌙'}</span>
                </button>
              </div>
            ) : (
              <>
                <div className={`flex items-center gap-2.5 px-4 py-3 ${isConnected ? 'bg-emerald-500/10' : 'bg-red-500/10'}`}>
                  <span className={`relative h-2.5 w-2.5 rounded-full ${isConnected ? 'bg-emerald-400' : 'bg-red-400'}`}>
                    {isConnected && <span className="absolute inset-0 animate-ping rounded-full bg-emerald-400 opacity-50" />}
                  </span>
                  <span className={`text-sm font-bold ${isConnected ? 'text-emerald-300' : 'text-red-300'}`}>
                    {isConnected ? t('app.online') : t('app.offline')}
                  </span>
                  <span className="ml-auto font-mono text-xs text-slate-500">{serverInfo ? `v${serverInfo.version}` : ''}</span>
                </div>
                {serverInfo && (
                  <div className="space-y-3 p-4 text-sm">
                    <div className="grid grid-cols-2 gap-2">
                      <div className="rounded-xl border border-slate-700/30 bg-slate-800/50 p-3 text-center">
                        <p className="text-2xl font-black text-cyan-300">{serverInfo.activeRooms}</p>
                        <p className="mt-1 text-xs text-slate-400">{t('serverInfo.rooms')}</p>
                      </div>
                      <div className="rounded-xl border border-slate-700/30 bg-slate-800/50 p-3 text-center">
                        <p className="text-2xl font-black text-emerald-300">{serverInfo.connectedSockets}</p>
                        <p className="mt-1 text-xs text-slate-400">{t('serverInfo.players')}</p>
                      </div>
                    </div>
                    <div className="h-px bg-white/10" />
                    <dl className="space-y-2 text-xs">
                      <div className="flex justify-between gap-3"><dt className="text-slate-400">{t('serverInfo.uptime')}</dt><dd className="font-mono text-slate-200">{formatUptime(serverInfo.uptime)}</dd></div>
                      <div className="flex justify-between gap-3"><dt className="text-slate-400">{t('serverInfo.node')}</dt><dd className="font-mono text-slate-200">{serverInfo.node}</dd></div>
                      <div className="flex justify-between gap-3"><dt className="text-slate-400">{t('serverInfo.memory')}</dt><dd className="font-mono text-slate-200">{serverInfo.memoryMB}MB</dd></div>
                    </dl>
                  </div>
                )}
              </>
            )}
          </section>
        </>,
        document.body
      )}

      <header className="theme-header relative z-20 border-b border-slate-700/60 bg-slate-900/90 shadow-lg backdrop-blur-xl" role="banner">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-2 px-3 py-2 sm:px-6">
          <div className="flex min-w-0 flex-1 items-center gap-2 border-r border-white/10 pr-3">
            <span className="text-xl drop-shadow-lg sm:text-2xl" aria-hidden="true">⚔️</span>
            <h1 className="min-w-0 truncate text-base font-black leading-none text-white sm:text-lg">{t('app.title')}</h1>
            {phase !== 'login' && gameId && (
              <span className="room-code-badge max-w-20 truncate rounded-md border border-yellow-400/20 bg-yellow-500/10 px-2 py-1 font-mono text-xs font-bold text-yellow-200 sm:max-w-none sm:text-sm" title={`${t('waiting.roomCode')}: ${gameId}`} aria-label={`${t('waiting.roomCode')}: ${gameId}`}>
                {gameId}
              </span>
            )}
          </div>
          <nav className="flex shrink-0 items-center gap-1 sm:gap-1.5" aria-label={t('app.controls')}>
            <button
              type="button"
              onClick={() => setShowLeaderboard(true)}
              className="header-icon-button"
              title={t('leaderboard.title')}
              aria-label={t('leaderboard.title')}
            >🏆</button>
            {user && (
              <button
                type="button"
                onClick={() => setShowProfile(true)}
                className="header-icon-button border-emerald-500/25 text-emerald-300 hover:bg-emerald-500/15"
                title={t('profile.title')}
                aria-label={t('profile.title')}
              >
                {user.photoUrl ? <img src={user.photoUrl} alt="" className="h-5 w-5 rounded-full" /> : '👤'}
              </button>
            )}
            <button
              type="button"
              ref={settingsBtnRef}
              onClick={() => setHeaderPopover(current => current === 'settings' ? null : 'settings')}
              className="header-icon-button"
              title={t('app.settings')}
              aria-label={t('app.settings')}
              aria-expanded={headerPopover === 'settings'}
            >⚙️</button>
            <button
              type="button"
              ref={serverInfoBtnRef}
              onClick={() => setHeaderPopover(current => current === 'server' ? null : 'server')}
              className={`header-icon-button ${isConnected ? 'border-cyan-400/25 text-cyan-300' : 'border-red-400/25 text-red-300'}`}
              title={t('serverInfo.title')}
              aria-label={`${t('serverInfo.title')}: ${isConnected ? t('app.online') : t('app.offline')}`}
              aria-expanded={headerPopover === 'server'}
            ><span className={`h-2 w-2 rounded-full ${isConnected ? 'bg-cyan-300 animate-pulse' : 'bg-red-400'}`} aria-hidden="true" /></button>
          </nav>
        </div>
      </header>

      <FloatingBubbles />
      {phase !== 'battle' && phase !== 'placement' && <BackgroundShips />}

      {/* Toast notification — bottom-center for less intrusion */}
      <div
        className={`fixed left-1/2 z-50 w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 transition-all duration-400 ${
          chatOpen ? 'bottom-[26rem]' : phase !== 'login' ? 'bottom-20' : 'bottom-6'
        } ${
          message ? 'opacity-100 translate-y-0 pointer-events-auto' : 'opacity-0 translate-y-4 pointer-events-none'
        }`}
        style={{ transitionTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)' }}
        aria-live="polite"
        aria-atomic="true"
      >
        <div className={`glass-card bg-slate-950/80 bg-gradient-to-r ${msgClass} rounded-2xl px-4 py-3 text-sm font-semibold flex items-center gap-3`}>
          <span className="text-base shrink-0">
            {messageType === 'success' ? '✅' : messageType === 'error' ? '⚠️' : 'ℹ️'}
          </span>
          <span className="min-w-0 flex-1 leading-snug">{message}</span>
          {message && (
            <button
              onClick={() => { if (messageTimerRef.current) clearTimeout(messageTimerRef.current); setMessage(''); setMessageType('info'); }}
              className="shrink-0 w-6 h-6 flex items-center justify-center rounded-full hover:bg-white/10 text-current opacity-50 hover:opacity-100 transition text-xs"
              aria-label="Dismiss"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      <main id="main-content" className={`w-full max-w-5xl mx-auto px-3 sm:px-6 py-4 sm:py-5 space-y-3 sm:space-y-4 relative z-10 ${mainLayoutClass}`} role="main">

        {phase === 'login' && (
          <LoginView
            loginView={loginView} gameId={gameId} roomPassword={roomPassword} playerName={playerName}
            setLoginView={handleSetLoginView} setGameId={setGameId} setRoomPassword={setRoomPassword} setPlayerName={setPlayerName}
            createPassword={createPassword} setCreatePassword={setCreatePassword}
            joinRoomCode={joinRoomCode} setJoinRoomCode={setJoinRoomCode}
            joinRoomPin={joinRoomPin} setJoinRoomPin={setJoinRoomPin}
            availableRooms={availableRooms} loadingRooms={loadingRooms} fetchRooms={fetchRooms}
            selectedRoom={selectedRoom} setSelectedRoom={setSelectedRoom}
            handleJoinGame={handleJoinGame} handleFinalJoin={handleFinalJoin}
            handleJoinAsSpectator={handleJoinAsSpectator}
            pendingJoin={pendingJoin} setPendingJoin={setPendingJoin}
            setMessageWithTimeout={setMessageWithTimeout}
            SOCKET_URL={serverUrl} setURLRoom={setURLRoom}
            gameTimeLimit={gameTimeLimit} setGameTimeLimit={setGameTimeLimit}
            roomNotFound={roomNotFound}
            roomHasPassword={roomHasPassword}
            roomIsFull={roomIsFull}
            isJoining={isJoining}
            handleGoogleLogin={handleGoogleLogin}
            user={user}
          />
        )}

        {phase === 'waiting' && <WaitingRoom gameId={gameId} roomPassword={roomPassword} handleBackToMenu={handleBackToMenu} timeLimit={gameTimeLimit} isHost={isHost} opponentName={opponentName} handleStartGame={handleStartGame} handleKickPlayer={handleKickPlayer} />}

        {phase === 'placement' && isSpectator && (
          <div className="max-w-md mx-auto pt-8 px-2 text-center space-y-6 animate-fade-in">
            <div className="glass-card p-8 space-y-5">
              <div className="text-5xl">🚢</div>
              <h2 className="text-lg font-bold text-slate-200">{t('spectator.placementTitle')}</h2>
              <p className="text-slate-400 text-sm leading-relaxed">{t('spectator.placementHint')}</p>
              <div className="flex justify-center gap-1.5 pt-2">
                {[0, 150, 300].map(d => (
                  <span key={d} className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: `${d}ms`, animationDuration: '1s' }} />
                ))}
              </div>
            </div>
            <button onClick={handleBackToMenu} className="px-6 py-2.5 text-red-400/80 hover:text-red-300 hover:bg-red-500/10 text-sm font-semibold transition-all rounded-lg">
              ← {t('waiting.leaveRoom')}
            </button>
          </div>
        )}

        {phase === 'placement' && !isSpectator && (
          <Suspense fallback={<div className="text-center py-12 text-slate-500">Loading…</div>}>
          <div className="space-y-3 animate-fade-in">
            <div className="glass-card p-3 sm:p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div className="min-w-0">
                <h2 className="text-base sm:text-lg font-black flex items-center gap-2 truncate">{t('placement.title')}</h2>
                <p className="text-slate-400 text-[0.65rem] mt-0.5">{t('placement.hint')}</p>
              </div>
              <div className="flex w-full sm:w-auto items-stretch sm:items-center gap-2">
                <button
                  onClick={isReady ? handleUnready : handleFinishPlace}
                  disabled={!isReady && shipsPlaced < 5}
                  className={`min-w-0 flex-1 sm:flex-none px-3 sm:px-4 py-2 rounded-xl font-bold text-sm text-center whitespace-normal transition-all ${isReady && !opponentReady
                    ? 'bg-yellow-500 hover:bg-yellow-400 text-white hover:scale-105 shadow-md'
                    : isReady && opponentReady
                      ? 'bg-green-700 text-green-200 cursor-not-allowed'
                      : shipsPlaced >= 5
                        ? 'bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 text-white hover:scale-105 shadow-md'
                        : 'bg-slate-700 text-slate-500 cursor-not-allowed'
                    }`}
                >
                  {isReady
                    ? (opponentReady ? t('placement.lockedIn') : `↩️ ${t('placement.unready')}`)
                    : `✅ ${t('placement.ready', shipsPlaced)}`}
                </button>
                <button onClick={handleBackToMenu} className="header-icon-button text-red-300" title={t('waiting.leaveRoom')} aria-label={t('waiting.leaveRoom')}>🚪</button>
              </div>
            </div>
            {/* Always render with fixed height to prevent layout shift */}
              <div className={`text-center text-xs py-1.5 rounded-lg min-h-[2rem] flex items-center justify-center transition-all duration-200 ${
                !opponentName ? 'invisible' : opponentReady ? 'bg-emerald-900/30 text-emerald-300 border border-emerald-500/20' : 'bg-slate-800/40 text-slate-500 border border-slate-700/30'
              }`}>
                {opponentReady ? `✓ ${t('placement.opReady', opponentName || 'Opponent')}` : t('placement.opWaiting', opponentName || 'Opponent')}
              </div>
            <ShipPlacement key={placementKey} onShipPlaced={handleShipPlaced} locked={isReady} />
          </div>
          </Suspense>
        )}

        {phase === 'battle' && (
          <Suspense fallback={<div className="text-center py-12 text-slate-500">Loading…</div>}>
          <BattleField
            isMyTurn={isMyTurn} opponentName={opponentName} handleForfeit={isSpectator ? NOOP : handleForfeit}
            dispPlayer={isSpectator ? (dispSpectatorBoards[0]?.board || dispPlayer) : dispPlayer}
            dispOpponent={isSpectator ? (dispSpectatorBoards[1]?.board || dispOpponent) : dispOpponent}
            spectatorPlayerNames={isSpectator ? [dispSpectatorBoards[0]?.playerName, dispSpectatorBoards[1]?.playerName] : []}
            spectatorBoards={isSpectator ? dispSpectatorBoards : []}
            playerName={playerName}
            handleShoot={isSpectator ? NOOP : handleShoot}
            explosionCells={explosionCells}
            playerTimeLeft={playerTimeLeft} turnStartedAt={turnStartedAt}
            myId={isSpectator ? null : playerId} currentTurn={currentTurn}
            spectatorCount={spectatorCount} isSpectator={isSpectator}
            lastShot={lastShot} handleLeave={handleBackToMenu}
            mySunkCount={myShipsSunk} theirSunkCount={theirShipsSunk}
            bombUsed={bombUsed} bombMode={bombMode}
            toggleBombMode={toggleBombMode} handleUseBomb={handleUseBomb}
          />
          </Suspense>
        )}

        {phase === 'gameOver' && (
          <Suspense fallback={<div className="text-center py-12 text-slate-500">Loading…</div>}>
          <GameOver
            winner={winner} playerId={playerId} opponentName={opponentName}
            opponentWantsPlayAgain={opponentWantsPlayAgain} playAgainPending={playAgainPending}
            handlePlayAgain={handlePlayAgain} handleBackToMenu={handleBackToMenu}
            handleDeclinePlayAgain={handleDeclinePlayAgain}
            handleReturnToWaiting={handleReturnToWaiting}
            isSpectator={isSpectator}
          />
          </Suspense>
        )}
      </main>

      {phase !== 'login' && (
        <Suspense fallback={null}>
          <ChatBox
            messages={chatMessages}
            onSend={sendChat}
            isOpen={chatOpen}
            onToggle={handleChatToggle}
            unread={chatUnread}
          />
        </Suspense>
      )}

      <Suspense fallback={null}>
        {showLeaderboard && (
          <Leaderboard onClose={() => setShowLeaderboard(false)} SOCKET_URL={serverUrl} />
        )}
        {showProfile && user && (
          <ProfileModal 
            onClose={() => setShowProfile(false)} 
            user={user} 
            setUser={setUser} 
            SOCKET_URL={serverUrl}
            firebaseAuthToken={firebaseAuthToken}
          />
        )}
      </Suspense>

      <footer className="max-w-5xl mx-auto px-3 sm:px-6 py-4 text-center mt-auto relative z-10" role="contentinfo">
        <p className="text-[0.6rem] text-slate-500 font-semibold mb-2">⚓ Battleships &mdash; Created by Adrians Bergmanis</p>
        <details className="mx-auto mb-2 w-fit text-left">
          <summary className="cursor-pointer list-none rounded-full border border-white/10 px-3 py-1 text-xs font-semibold text-slate-400 hover:border-cyan-400/30 hover:text-cyan-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-cyan-300">
            {t('footer.musicCredits')}
          </summary>
          <div className="mt-2 grid gap-1 rounded-xl border border-white/10 bg-slate-950/70 p-3 text-left sm:grid-cols-2">
          {[
            ['The Price of Freedom',  'Menu Music',           'Royalty-Free Music'],
            ['Beyond New Horizons',   'Ship Placement Music', 'Royalty-Free Music'],
            ['Honor and Sword',       'Battle Music',         'No-Copyright Music'],
            ['Victory',               'Victory Sound',        'Free Sound Effect'],
            ['Waves Crash',           'Defeat Sound',         'Free Sound Effect'],
          ].map(([name, role, source]) => (
            <span key={name} className="text-xs text-slate-400" title={`${role} — ${source}`}>
              <span className="text-cyan-300">♪</span> &quot;{name}&quot;
            </span>
          ))}
        </div>
        </details>
        <p className="text-[0.5rem] text-slate-700">&copy; Adrians Bergmanis. All rights reserved. Music &amp; sounds are royalty-free / no-copyright.</p>
      </footer>
    </div>
  );
}

export default App;
