import { GRID_SIZE, CELL, SHIPS } from '../constants';

export function createEmptyBoard() {
    return Array(GRID_SIZE).fill(null).map(() => Array(GRID_SIZE).fill(CELL.WATER));
}

/** Pure — checks 1-cell buffer including diagonals */
export function canPlaceShipOnBoard(board, row, col, length, dir) {
    const cells = [];
    if (dir === 'horizontal') {
        if (col + length > GRID_SIZE) return { valid: false, cells: [] };
        for (let i = 0; i < length; i++) {
            if (board[row][col + i] !== CELL.WATER) return { valid: false, cells: [] };
            cells.push({ row, col: col + i });
        }
    } else {
        if (row + length > GRID_SIZE) return { valid: false, cells: [] };
        for (let i = 0; i < length; i++) {
            if (board[row + i][col] !== CELL.WATER) return { valid: false, cells: [] };
            cells.push({ row: row + i, col });
        }
    }
    const cellSet = new Set(cells.map(c => `${c.row},${c.col}`));
    for (const c of cells) {
        for (let dr = -1; dr <= 1; dr++) {
            for (let dc = -1; dc <= 1; dc++) {
                const nr = c.row + dr, nc = c.col + dc;
                if (nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE) continue;
                if (cellSet.has(`${nr},${nc}`)) continue;
                if (board[nr][nc] === CELL.SHIP) return { valid: false, cells: [] };
            }
        }
    }
    return { valid: true, cells };
}

/** Generates a fully valid random layout for all ships */
export function generateRandomPlacement() {
    for (let attempt = 0; attempt < 500; attempt++) {
        const board = createEmptyBoard();
        const placements = [];
        let failed = false;
        for (const ship of SHIPS) {
            let placed = false;
            for (let t = 0; t < 150; t++) {
                const dir = Math.random() < 0.5 ? 'horizontal' : 'vertical';
                const row = Math.floor(Math.random() * GRID_SIZE);
                const col = Math.floor(Math.random() * GRID_SIZE);
                const { valid, cells } = canPlaceShipOnBoard(board, row, col, ship.length, dir);
                if (valid) {
                    cells.forEach(c => { board[c.row][c.col] = CELL.SHIP; });
                    placements.push({ row, col, length: ship.length, direction: dir });
                    placed = true;
                    break;
                }
            }
            if (!placed) { failed = true; break; }
        }
        if (!failed) return { board, placements };
    }
    return null;
}

/** Returns the Set of "row,col" keys surrounding the given sunk ship cells */
export function getSurroundingKeys(shipCells) {
    const shipSet = new Set(shipCells.map(c => `${c.row},${c.col}`));
    const ring = new Set();
    for (const c of shipCells) {
        for (let dr = -1; dr <= 1; dr++) {
            for (let dc = -1; dc <= 1; dc++) {
                const nr = c.row + dr, nc = c.col + dc;
                if (nr < 0 || nr >= GRID_SIZE || nc < 0 || nc >= GRID_SIZE) continue;
                const key = `${nr},${nc}`;
                if (!shipSet.has(key)) ring.add(key);
            }
        }
    }
    return ring;
}

/** Get room code and password from URL path (e.g., /ABC123 or /ABC123/123) */
export function getRoomFromURL() {
    const path = window.location.pathname.replace(/^\/+|\/+$/g, '');
    if (!path) return { roomCode: null, password: null };

    const parts = path.split('/');
    const roomCode = parts[0];
    const rawPassword = parts[1] || null;

    // Password must be exactly 3 digits
    let password = null;
    if (rawPassword && /^\d{3}$/.test(rawPassword)) {
        password = rawPassword;
    }

    if (roomCode && /^[A-Z0-9]{4,10}$/i.test(roomCode)) {
        return { roomCode: roomCode.toUpperCase(), password };
    }
    return { roomCode: null, password: null };
}

/** Update URL to reflect current room and optionally password */
export function setURLRoom(roomCode, password = null) {
    if (roomCode) {
        const path = password ? `/${roomCode}/${password}` : `/${roomCode}`;
        window.history.replaceState(null, '', path);
    } else {
        window.history.replaceState(null, '', '/');
    }
}
