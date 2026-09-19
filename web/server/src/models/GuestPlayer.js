const mongoose = require('mongoose');

/**
 * Tracks stats for guest (non-authenticated) players.
 * Guests are identified by their chosen display name.
 * If the same name is reused, wins/gamesPlayed accumulate on the same record.
 */
const guestPlayerSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    unique: true,
    maxlength: 50,
    index: true,
  },
  wins: {
    type: Number,
    default: 0,
    min: 0,
  },
  gamesPlayed: {
    type: Number,
    default: 0,
    min: 0,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
  lastSeenAt: {
    type: Date,
    default: Date.now,
  },
});

module.exports = mongoose.model('GuestPlayer', guestPlayerSchema);
