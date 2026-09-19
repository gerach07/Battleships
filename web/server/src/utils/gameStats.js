const User = require('../models/User');
const GuestPlayer = require('../models/GuestPlayer');

/**
 * Record a game result for any players (authenticated or guest).
 *
 * @param {string|null} winnerFirebaseUid  - Firebase UID of the winner (null if guest)
 * @param {string|null} loserFirebaseUid   - Firebase UID of the loser (null if guest)
 * @param {string|null} winnerGuestName    - Display name of winner if guest (null if auth'd)
 * @param {string|null} loserGuestName     - Display name of loser if guest (null if auth'd)
 */
async function recordGameResult(winnerFirebaseUid, loserFirebaseUid, winnerGuestName = null, loserGuestName = null) {
  try {
    const ops = [];

    // ── Authenticated winner ──────────────────────────────────────
    if (winnerFirebaseUid) {
      ops.push(
        User.updateOne(
          { firebaseUid: winnerFirebaseUid },
          { $inc: { wins: 1, gamesPlayed: 1 } }
        )
      );
    }
    // ── Guest winner ──────────────────────────────────────────────
    else if (winnerGuestName) {
      const name = winnerGuestName.trim().slice(0, 50);
      ops.push(
        GuestPlayer.findOneAndUpdate(
          { name },
          { $inc: { wins: 1, gamesPlayed: 1 }, $set: { lastSeenAt: new Date() } },
          { upsert: true, new: true }
        )
      );
    }

    // ── Authenticated loser ───────────────────────────────────────
    if (loserFirebaseUid) {
      ops.push(
        User.updateOne(
          { firebaseUid: loserFirebaseUid },
          { $inc: { gamesPlayed: 1 } }
        )
      );
    }
    // ── Guest loser ───────────────────────────────────────────────
    else if (loserGuestName) {
      const name = loserGuestName.trim().slice(0, 50);
      ops.push(
        GuestPlayer.findOneAndUpdate(
          { name },
          { $inc: { gamesPlayed: 1 }, $set: { lastSeenAt: new Date() } },
          { upsert: true, new: true }
        )
      );
    }

    if (ops.length > 0) await Promise.all(ops);
  } catch (err) {
    console.error('Failed to record game result:', err.message);
  }
}

module.exports = { recordGameResult };
