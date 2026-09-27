const mongoose = require('mongoose');

const reconnectSessionSchema = new mongoose.Schema({
  roomId: {
    type: String,
    required: true,
    uppercase: true,
    trim: true,
    maxlength: 10,
    index: true,
  },
  playerName: {
    type: String,
    required: true,
    trim: true,
    maxlength: 50,
    index: true,
  },
  sessionToken: {
    type: String,
    required: true,
    unique: true,
    index: true,
  },
  socketId: {
    type: String,
    required: true,
    index: true,
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true,
  },
  createdAt: {
    type: Date,
    default: Date.now,
    index: true,
  },
}, { timestamps: true });

module.exports = mongoose.model('ReconnectSession', reconnectSessionSchema);
