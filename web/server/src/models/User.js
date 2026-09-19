const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  firebaseUid: {
    type: String,
    required: true,
    unique: true,
    index: true,
  },
  email: {
    type: String,
    required: true,
  },
  name: {
    type: String,
    required: true,
    default: 'Anonymous',
    maxlength: 50,
  },
  playerId: {
    type: String,
    unique: true,
    sparse: true,
    maxlength: 30,
  },
  photoUrl: {
    type: String,
    default: null,
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
  lastLoginAt: {
    type: Date,
    default: Date.now,
  },
});

// Generate a default playerId from the first part of firebaseUid if not set
userSchema.pre('save', function (next) {
  if (!this.playerId) {
    this.playerId = 'player_' + this.firebaseUid.slice(0, 8);
  }
  next();
});

module.exports = mongoose.model('User', userSchema);
