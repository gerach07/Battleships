export const GRID_SIZE = 10;

export const SHIPS = [
    { id: 0, length: 5, name: 'Carrier', emoji: '🛥️' },
    { id: 1, length: 4, name: 'Battleship', emoji: '⛵' },
    { id: 2, length: 3, name: 'Destroyer', emoji: '🚢' },
    { id: 3, length: 3, name: 'Submarine', emoji: '🤿' },
    { id: 4, length: 2, name: 'Patrol', emoji: '🚤' },
];

export const CELL = {
    WATER: 'W',
    SHIP: 'S',
    HIT: 'H',
    MISS: 'M',
    SUNK: 'X',  // client-only: ship fully sunk
    SAFE: 'Z',  // client-only: safe zone around sunk ship
};

export const LOCAL_SERVER_URL = 'http://localhost:3001';
export const PUBLIC_SERVER_URL = 'https://battleships-server-jtit.onrender.com';

// If REACT_APP_SERVER_URL is explicitly set at build time, use only that.
// Otherwise the app will try LOCAL first, then fall back to PUBLIC at runtime (see useSocket.js).
export const SOCKET_URL = process.env.REACT_APP_SERVER_URL || null;
