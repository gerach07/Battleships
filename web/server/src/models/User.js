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



module.exports = mongoose.model('User', userSchema);
