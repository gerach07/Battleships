const { GRID_SIZE } = require('../constants');

function sanitizeInput(input, maxLength = 100) {
    if (typeof input !== 'string') return '';
    return input
        .trim()
        .slice(0, maxLength)
        .replace(/[<>"'`&]/g, '')
        .replace(/[\x00-\x1F\x7F]/g, '');
}

function deepCloneBoard(board) {
    return board.map(row => [...row]);
}

function isValidCoordinate(row, col) {
    return Number.isInteger(row) && Number.isInteger(col) &&
        row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;
}

module.exports = {
    sanitizeInput,
    deepCloneBoard,
    isValidCoordinate,
};
