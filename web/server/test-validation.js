/**
 * ============================================================================
 * BATTLESHIPS GAME - COMPREHENSIVE TEST SUITE & VALIDATION
 * ============================================================================
 * 
 * PHASE 5: FINAL VALIDATION
 * Tests all critical game logic, security fixes, and edge cases
 * 
 * Run with: node test-validation.js (in server directory)
 * ============================================================================
 */

const Room = require('./src/models/Room');
const RateLimiter = require('./src/utils/RateLimiter');
const { sanitizeInput } = require('./src/utils/sanitizers');

// ============================================================================
// TEST FRAMEWORK SETUP
// ============================================================================

let testsPassed = 0;
let testsFailed = 0;
const failures = [];

function assert(condition, testName) {
  if (condition) {
    testsPassed++;
    console.log(`✅ ${testName}`);
  } else {
    testsFailed++;
    failures.push(testName);
    console.error(`❌ ${testName}`);
  }
}

function suite(name) {
  console.log(`\n${'='.repeat(70)}`);
  console.log(`${name}`);
  console.log(`${'='.repeat(70)}`);
}

/**
 * Helper: Place all 5 standard ships for a player without overlap
 * Layout A: ships along left columns (horizontal)
 * Layout B: ships along right columns (horizontal)
 */
function placeAllShipsLayoutA(room, playerId) {
  room.placeShip(playerId, 0, 0, 5, 'horizontal'); // (0,0)-(0,4)
  room.placeShip(playerId, 2, 0, 4, 'horizontal'); // (2,0)-(2,3)
  room.placeShip(playerId, 4, 0, 3, 'horizontal'); // (4,0)-(4,2)
  room.placeShip(playerId, 6, 0, 3, 'horizontal'); // (6,0)-(6,2)
  room.placeShip(playerId, 8, 0, 2, 'horizontal'); // (8,0)-(8,1)
}

function placeAllShipsLayoutB(room, playerId) {
  room.placeShip(playerId, 0, 5, 5, 'horizontal'); // (0,5)-(0,9)
  room.placeShip(playerId, 2, 5, 4, 'horizontal'); // (2,5)-(2,8)
  room.placeShip(playerId, 4, 5, 3, 'horizontal'); // (4,5)-(4,7)
  room.placeShip(playerId, 6, 5, 3, 'horizontal'); // (6,5)-(6,7)
  room.placeShip(playerId, 8, 5, 2, 'horizontal'); // (8,5)-(8,6)
}

// All cells occupied by Layout B ships (for targeted win simulation)
const LAYOUT_B_CELLS = [
  [0,5],[0,6],[0,7],[0,8],[0,9],  // 5-ship
  [2,5],[2,6],[2,7],[2,8],        // 4-ship
  [4,5],[4,6],[4,7],              // 3-ship
  [6,5],[6,6],[6,7],              // 3-ship
  [8,5],[8,6],                    // 2-ship
];

// ============================================================================
// PHASE 5: VALIDATION TESTS
// ============================================================================

suite('PHASE 5.1: CORE GAME LOGIC VALIDATION');

// Test 1: Board initialization
const room1 = new Room('test-room-1', null, 'Alice', undefined, 'player1');
room1.addPlayer('player1', 'Alice');
room1.addPlayer('player2', 'Bob');
room1.startGameByHost('player1');

assert(
  room1.getPlayerIds().length === 2,
  'Both players added to room'
);

assert(
  room1.players['player1'].board.length === 10 &&
  room1.players['player1'].board[0].length === 10,
  'Board is 10x10'
);

assert(
  room1.players['player1'].board.every(row =>
    row.every(cell => cell === 'W')
  ),
  'Board initialized with all water'
);

// Test 2: Ship placement validation
suite('PHASE 5.2: SHIP PLACEMENT LOGIC');

const validation1 = room1.validateShipPlacement('player1', 0, 0, 5, 'horizontal');
assert(validation1.valid, 'Valid horizontal placement accepted');

room1.placeShip('player1', 0, 0, 5, 'horizontal');

const validation3 = room1.validateShipPlacement('player1', 0, 0, 4, 'vertical');
assert(!validation3.valid, 'Overlapping ship rejected');

const validation4 = room1.validateShipPlacement('player1', 0, 6, 5, 'horizontal');
assert(!validation4.valid, 'Out-of-bounds placement rejected');

const validation5 = room1.validateShipPlacement('player1', 2, 0, 4, 'horizontal');
assert(validation5.valid, 'Non-overlapping placement accepted');

// Complete Player 1 placement (Layout A — first ship already placed)
room1.placeShip('player1', 2, 0, 4, 'horizontal');
room1.placeShip('player1', 4, 0, 3, 'horizontal');
room1.placeShip('player1', 6, 0, 3, 'horizontal');
room1.placeShip('player1', 8, 0, 2, 'horizontal');

assert(
  room1.hasPlayerPlacedAllShips('player1'),
  'All 5 ships detected as placed'
);

// Player 2 placements (Layout B — right side, no overlaps)
placeAllShipsLayoutB(room1, 'player2');

assert(
  room1.hasPlayerPlacedAllShips('player2'),
  'Player 2: All 5 ships placed'
);

// Test 3: Battle phase transition
suite('PHASE 5.3: BATTLE PHASE TRANSITION');

const ready1 = room1.markPlayerReady('player1');
assert(!ready1, 'First player cannot start battle alone');

const ready2 = room1.markPlayerReady('player2');
assert(ready2, 'Battle starts when both players ready');

assert(
  room1.getState() === 'BATTLE_PHASE',
  'Game transitioned to BATTLE_PHASE'
);

assert(
  room1.currentTurn !== null,
  'Current turn assigned'
);

// Test 4: HIT/MISS logic
suite('PHASE 5.4: HIT/MISS PROCESSING');

const currentShooter = room1.currentTurn; // player1
const otherPlayer = room1.getOpponentId(currentShooter); // player2

// Player 1 fires at Player 2's ship (Layout B first ship at (0,5))
const shot1 = room1.processShot(currentShooter, 0, 5);
assert(shot1.success, 'Valid shot accepted');
assert(shot1.result.isHit, 'Hit on ship detected');
assert(!shot1.result.shipSunk, 'Ship not sunk (5-cell ship, only 1 hit)');
assert(shot1.result.nextTurn === currentShooter, 'Hit keeps shooter turn');

// Since Player 1 hit, they shoot again - miss to give Player 2 a turn
const shot1b = room1.processShot(currentShooter, 9, 9); // water on P2's board
assert(shot1b.success, 'Follow-up shot after hit accepted');
assert(!shot1b.result.isHit, 'Miss on water detected');
assert(shot1b.result.nextTurn === otherPlayer, 'Miss switches turn to opponent');

// Now Player 2 fires at water (row 9 has no P1 ships in Layout A)
const shot2 = room1.processShot(otherPlayer, 9, 9);
assert(shot2.success, 'Second player shot accepted');
assert(!shot2.result.isHit, 'Miss on water detected');

// Player 1 tries to re-target same cell (0,5) — already HIT on P2's board
const shot3 = room1.processShot(shot2.result.nextTurn, 0, 5);
assert(!shot3.success, 'Re-targeting same cell rejected');

// Test 5: Win condition
suite('PHASE 5.5: WIN CONDITION LOGIC');

// Player 1 sinks all of Player 2's ships (Layout B)
// (0,5) already hit in Test 4 — 16 remaining cells
// With "hits keep turn" rule, Player 1 can keep shooting until they miss or win

// Disable rate limiting for rapid-fire win condition test
room1.shotLimiter = new RateLimiter(1000, 1000);

// Filter out cells already targeted in Test 4
const alreadyTargeted = [[0, 5], [9, 9]]; // hit and miss from Test 4
const remainingP2Cells = LAYOUT_B_CELLS.filter(
  ([r, c]) => !alreadyTargeted.some(([tr, tc]) => tr === r && tc === c)
);

let gameWon = false;
let shotCount = 0;
const maxShots = 100; // safety limit

// Keep shooting until game is won or we hit max shots
while (!gameWon && shotCount < maxShots) {
  const shooter = room1.currentTurn;
  
  // Find an unhit cell to target
  let targetRow, targetCol, found = false;
  
  // If it's player1's turn, target P2's ship cells
  if (shooter === 'player1' && remainingP2Cells.length > 0) {
    [targetRow, targetCol] = remainingP2Cells.shift();
    found = true;
  } else if (shooter === 'player2') {
    // Player 2 misses intentionally at row 1 (no ships in Layout A)
    targetRow = 1;
    targetCol = shotCount % 10;
    found = true;
  }
  
  if (!found) break;
  
  const result = room1.processShot(shooter, targetRow, targetCol);
  shotCount++;
  
  if (!result.success) continue; // skip invalid shots
  
  if (result.result.gameWon) {
    gameWon = true;
  }
}

assert(
  gameWon && room1.getState() === 'GAME_OVER',
  'Game ends when all segments hit (WIN CONDITION)'
);

assert(
  room1.winner === 'player1',
  'Correct winner assigned'
);

// Test 6: Security - Input Sanitization
suite('PHASE 5.6: SECURITY - INPUT VALIDATION');

const sanitized1 = sanitizeInput('<script>alert("xss")</script>', 100);
assert(
  !sanitized1.includes('<') && !sanitized1.includes('>'),
  'XSS script tags sanitized'
);

const sanitized2 = sanitizeInput('Alice&Bob', 100);
assert(sanitized2 === 'AliceBob', 'Ampersands stripped for security');

const sanitized2b = sanitizeInput("O'Brien", 100);
assert(sanitized2b === 'OBrien', 'Apostrophes stripped for security');

const sanitized2c = sanitizeInput('A<B>C', 100);
assert(sanitized2c === 'ABC', 'Angle brackets stripped');

const sanitized3 = sanitizeInput('a'.repeat(200), 50);
assert(sanitized3.length === 50, 'Input length truncated to max');

// Test 7: Rate Limiting
suite('PHASE 5.7: SECURITY - RATE LIMITING');

const limiter = new RateLimiter(3, 1000);

let limiterPassed = 0;
for (let i = 0; i < 5; i++) {
  if (limiter.isAllowed('player1')) {
    limiterPassed++;
  }
}

assert(
  limiterPassed === 3,
  'Rate limiter enforces 3 requests per second'
);

assert(
  !limiter.isAllowed('player1'),
  'Further requests blocked after limit'
);

// Test 8: Board Privacy (Critical Security)
suite('PHASE 5.8: SECURITY - BOARD PRIVACY');

const room2 = new Room('test-room-2');
room2.addPlayer('player1', 'Alice');
room2.addPlayer('player2', 'Bob');

room2.placeShip('player1', 0, 0, 5, 'horizontal');

const player1Board = room2.getPlayerBoard('player1');
assert(
  player1Board[0][0] === 'S',
  'Own board correctly shows ships'
);

room2.placeShip('player2', 5, 5, 3, 'vertical');

const opponentViewOfPlayer1 = room2.getOpponentViewBoard('player1');
const hasShipInOpponentView = opponentViewOfPlayer1.some(row =>
  row.some(cell => cell === 'S')
);

assert(
  !hasShipInOpponentView,
  'Opponent view NEVER exposes ship positions (CRITICAL SECURITY)'
);

// Test 9: Immutable Board Copies
suite('PHASE 5.9: SECURITY - IMMUTABLE STATE');

const room3 = new Room('test-room-3');
room3.addPlayer('player1', 'Alice');
room3.addPlayer('player2', 'Bob');
room3.placeShip('player1', 0, 0, 3, 'horizontal');

const board1 = room3.getPlayerBoard('player1');
const board2 = room3.getPlayerBoard('player1');

board1[0][0] = 'HACKED';

assert(
  board2[0][0] !== 'HACKED',
  'Board copies are deep - client mutations don\'t corrupt server'
);

// Test 10: Invalid Transitions
suite('PHASE 5.10: STATE MACHINE VALIDATION');

const room4 = new Room('test-room-4', null, 'Alice', undefined, 'player1');
room4.addPlayer('player1', 'Alice');
room4.addPlayer('player2', 'Bob');
room4.startGameByHost('player1');
// State is now PLACEMENT_PHASE (host started the game)

// Try invalid transition (PLACEMENT_PHASE -> GAME_OVER is not allowed)
const invalidTransition = room4._transitionState('GAME_OVER');
assert(!invalidTransition, 'Invalid state transitions rejected');

// Valid transition (PLACEMENT_PHASE -> BATTLE_PHASE)
const validTransition = room4._transitionState('BATTLE_PHASE');
assert(validTransition, 'Valid state transitions allowed');

// Test 11: Turn-order cannot be manipulated
suite('PHASE 5.11: SECURITY - TURN ORDER');

const room5 = new Room('test-room-5', null, 'Alice', undefined, 'player1');
room5.addPlayer('player1', 'Alice');
room5.addPlayer('player2', 'Bob');
room5.startGameByHost('player1');

// Must place ALL 5 ships per player for markPlayerReady to work
placeAllShipsLayoutA(room5, 'player1');
room5.markPlayerReady('player1');

placeAllShipsLayoutB(room5, 'player2');
room5.markPlayerReady('player2');

assert(
  room5.getState() === 'BATTLE_PHASE',
  'Room5 entered BATTLE_PHASE for turn-order test'
);

const initialTurn = room5.currentTurn;
const notInitialTurn = room5.getOpponentId(initialTurn);

// Player not on turn tries to shoot
const unauthedShot = room5.processShot(notInitialTurn, 0, 0);
assert(
  !unauthedShot.success,
  'Turn enforcement prevents out-of-turn shots'
);

// Test 12: Reset Game State Validation
suite('PHASE 5.12: RESET GAME STATE VALIDATION');

const room6r = new Room('test-room-reset', null, 'Alice', undefined, 'p1');
room6r.addPlayer('p1', 'Alice');
room6r.addPlayer('p2', 'Bob');
room6r.startGameByHost('p1');
placeAllShipsLayoutA(room6r, 'p1');
placeAllShipsLayoutB(room6r, 'p2');
room6r.markPlayerReady('p1');
room6r.markPlayerReady('p2');

assert(
  room6r.getState() === 'BATTLE_PHASE',
  'Reset test: Room in BATTLE_PHASE'
);

// Try resetting during battle — should FAIL
const midBattleReset = room6r._transitionState('PLACEMENT_PHASE');
assert(
  !midBattleReset,
  'Cannot reset during BATTLE_PHASE (CRITICAL SECURITY)'
);
assert(
  room6r.getState() === 'BATTLE_PHASE',
  'State unchanged after invalid reset attempt'
);

// Verify ships are still intact (not wiped)
assert(
  room6r.players['p1'].ships.length === 5,
  'Player ships preserved after invalid reset'
);

// Test 13: Turn Doesn't Switch After Win
suite('PHASE 5.13: TURN PRESERVED ON WIN');

const room6w = new Room('test-room-win-turn', null, 'Alice', undefined, 'p1');
room6w.addPlayer('p1', 'Alice');
room6w.addPlayer('p2', 'Bob');
room6w.startGameByHost('p1');
placeAllShipsLayoutA(room6w, 'p1');
placeAllShipsLayoutB(room6w, 'p2');
room6w.markPlayerReady('p1');
room6w.markPlayerReady('p2');
room6w.shotLimiter = new RateLimiter(1000, 1000);

// P1 shoots all P2 ships - with "hit keeps turn", P1 can keep shooting
// Make a copy of cells to target
const targetCells = [...LAYOUT_B_CELLS];
let winnerTurn = null;
let p2MissCol = 0;

while (room6w.getState() === 'BATTLE_PHASE' && targetCells.length >= 0) {
  const shooter = room6w.currentTurn;
  
  if (shooter === 'p1' && targetCells.length > 0) {
    // P1 fires at P2's ships
    const [r, c] = targetCells.shift();
    const res = room6w.processShot('p1', r, c);
    if (res.result && res.result.gameWon) {
      winnerTurn = room6w.currentTurn;
      break;
    }
  } else if (shooter === 'p2') {
    // P2 fires a miss at row 1 (no ships for P1 in Layout A)
    room6w.processShot('p2', 1, p2MissCol % 10);
    p2MissCol++;
  } else {
    break; // safety
  }
}

assert(
  winnerTurn === 'p1',
  'Winner is p1 (the shooter)'
);
assert(
  room6w.currentTurn === 'p1',
  'Turn stays on winner after winning shot (not switched)'
);

// Test 14: Complete Game Flow Simulation
suite('PHASE 5.14: END-TO-END GAME SIMULATION');

const room6 = new Room('test-room-6', null, 'Alice', undefined, 'p1');
room6.addPlayer('p1', 'Alice');
room6.addPlayer('p2', 'Bob');
room6.startGameByHost('p1');

// Placement phase — use offset layouts to avoid overlap between ships
const shipDefs = [
  { row: 0, col: 0, length: 5, dir: 'horizontal' },
  { row: 2, col: 0, length: 4, dir: 'horizontal' },
  { row: 4, col: 0, length: 3, dir: 'horizontal' },
  { row: 6, col: 0, length: 3, dir: 'horizontal' },
  { row: 8, col: 0, length: 2, dir: 'horizontal' },
];

shipDefs.forEach(ship => {
  room6.placeShip('p1', ship.row, ship.col, ship.length, ship.dir);
  room6.placeShip('p2', ship.row, ship.col + 5, ship.length, ship.dir);
});

assert(
  room6.hasPlayerPlacedAllShips('p1') && room6.hasPlayerPlacedAllShips('p2'),
  'End-to-end: All ships placed for both players'
);

room6.markPlayerReady('p1');
room6.markPlayerReady('p2');

assert(
  room6.getState() === 'BATTLE_PHASE',
  'End-to-end: Game entered BATTLE_PHASE'
);

// Battle simulation — random valid shots until game over
let totalShots = 0;
const maxTotalShots = 200;

while (room6.getState() === 'BATTLE_PHASE' && totalShots < maxTotalShots) {
  const currentTurnPlayer = room6.currentTurn;
  const opponent = room6.getOpponentId(currentTurnPlayer);
  const board = room6.players[opponent].board;

  // Find a valid untargeted cell
  let row, col, cell;
  do {
    row = Math.floor(Math.random() * 10);
    col = Math.floor(Math.random() * 10);
    cell = board[row][col];
  } while (cell === 'H' || cell === 'M');

  const result = room6.processShot(currentTurnPlayer, row, col);
  if (result.success) {
    totalShots++;
  }
}

assert(
  room6.getState() === 'GAME_OVER',
  'Complete game simulation ends properly'
);

assert(
  room6.winner !== null,
  'Winner assigned after game (WIN/LOSS CONDITION)'
);

// ============================================================================
// TEST RESULTS
// ============================================================================

console.log(`\n${'='.repeat(70)}`);
console.log(`TEST RESULTS`);
console.log(`${'='.repeat(70)}`);
console.log(`✅ Passed: ${testsPassed}`);
console.log(`❌ Failed: ${testsFailed}`);
console.log(`📊 Total:  ${testsPassed + testsFailed}`);
console.log(`📈 Success Rate: ${((testsPassed / (testsPassed + testsFailed)) * 100).toFixed(1)}%`);

if (testsFailed > 0) {
  console.log('\n❌ FAILED TESTS:');
  failures.forEach(f => console.log(`  - ${f}`));
  process.exit(1);
} else {
  console.log('\n🎉 ALL TESTS PASSED! SYSTEM IS PRODUCTION-READY.');
  process.exit(0);
}
