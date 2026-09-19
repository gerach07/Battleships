const User = require('../models/User');

/**
 * Record a game result for authenticated players.
 * Called when a game ends (shotResult with gameWon, forfeit, or timeUp).
 *
 * @param {string|null} winnerFirebaseUid - Firebase UID of the winner (null if guest)
 * @param {string|null} loserFirebaseUid  - Firebase UID of the loser (null if guest)
 */
async function recordGameResult(winnerFirebaseUid, loserFirebaseUid) {
  try {
    const ops = [];
    if (winnerFirebaseUid) {
      ops.push(
        User.updateOne(
          { firebaseUid: winnerFirebaseUid },
          { $inc: { wins: 1, gamesPlayed: 1 } }
        )
      );
    }
    if (loserFirebaseUid) {
      ops.push(
        User.updateOne(
          { firebaseUid: loserFirebaseUid },
          { $inc: { gamesPlayed: 1 } }
        )
      );
    }
    if (ops.length > 0) await Promise.all(ops);
  } catch (err) {
    console.error('Failed to record game result:', err.message);
  }
}

module.exports = { recordGameResult };
