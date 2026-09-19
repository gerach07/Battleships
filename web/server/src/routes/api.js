const express = require('express');
const { requireAuth, optionalAuth } = require('../auth/firebase');
const User = require('../models/User');
const { sanitizeInput } = require('../utils/sanitizers');

const router = express.Router();

// ─── POST /api/auth/login ─────────────────────────────────────────────────────
// Called after Google Sign-In on any client. Creates user in DB if new.
router.post('/auth/login', requireAuth, async (req, res) => {
  try {
    const { uid, email, name: firebaseName, picture } = req.user;

    let user = await User.findOne({ firebaseUid: uid });
    if (!user) {
      user = await User.create({
        firebaseUid: uid,
        email: email || '',
        name: sanitizeInput(firebaseName || 'Anonymous', 50),
        photoUrl: picture || null,
      });
    } else {
      // Update last login and photo
      user.lastLoginAt = new Date();
      if (picture && !user.photoUrl) user.photoUrl = picture;
      await user.save();
    }

    res.json({
      id: user._id,
      firebaseUid: user.firebaseUid,
      email: user.email,
      name: user.name,

      photoUrl: user.photoUrl,
      wins: user.wins,
      gamesPlayed: user.gamesPlayed,
    });
  } catch (err) {
    console.error('Auth login error:', err);
    res.status(500).json({ error: 'Login failed' });
  }
});

// ─── GET /api/profile ─────────────────────────────────────────────────────────
// Get the authenticated user's profile.
router.get('/profile', requireAuth, async (req, res) => {
  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });
    if (!user) return res.status(404).json({ error: 'User not found' });

    res.json({
      id: user._id,
      firebaseUid: user.firebaseUid,
      email: user.email,
      name: user.name,

      photoUrl: user.photoUrl,
      wins: user.wins,
      gamesPlayed: user.gamesPlayed,
    });
  } catch (err) {
    console.error('Profile get error:', err);
    res.status(500).json({ error: 'Failed to get profile' });
  }
});

// ─── PUT /api/profile ─────────────────────────────────────────────────────────
// Update name.
router.put('/profile', requireAuth, async (req, res) => {
  try {
    const user = await User.findOne({ firebaseUid: req.user.uid });
    if (!user) return res.status(404).json({ error: 'User not found' });

    const { name } = req.body;

    if (name !== undefined) {
      const sanitized = sanitizeInput(name, 50);
      if (!sanitized || sanitized.length < 1) {
        return res.status(400).json({ error: 'Name cannot be empty' });
      }
      // Block names already claimed by a guest player with wins to prevent
      // impersonation and leaderboard confusion.
      if (sanitized.toLowerCase() !== user.name.toLowerCase()) {
        const GuestPlayer = require('../models/GuestPlayer');
        const escapedName = sanitized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const guestClash = await GuestPlayer.findOne({
          name: { $regex: new RegExp(`^${escapedName}$`, 'i') },
          wins: { $gt: 0 },
        });
        if (guestClash) {
          return res.status(409).json({
            error: `The name "${sanitized}" is already used by a guest player on the leaderboard. Please choose a different name.`,
          });
        }
      }
      user.name = sanitized;
    }



    await user.save();

    res.json({
      id: user._id,
      firebaseUid: user.firebaseUid,
      email: user.email,
      name: user.name,

      photoUrl: user.photoUrl,
      wins: user.wins,
      gamesPlayed: user.gamesPlayed,
    });
  } catch (err) {
    console.error('Profile update error:', err);
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

// ─── GET /api/leaderboard ─────────────────────────────────────────────────────
// Public endpoint — returns top 50 players (auth + guest) by wins.
router.get('/leaderboard', async (req, res) => {
  try {
    const GuestPlayer = require('../models/GuestPlayer');

    const [authUsers, guestUsers] = await Promise.all([
      User.find({ wins: { $gte: 0 } })
        .select('name wins gamesPlayed photoUrl')
        .lean(),
      GuestPlayer.find({ wins: { $gte: 0 } })
        .select('name wins gamesPlayed')
        .lean(),
    ]);

    // Merge into unified list, marking guests with no photoUrl
    const merged = [
      ...authUsers.map(u => ({ name: u.name, wins: u.wins, gamesPlayed: u.gamesPlayed, photoUrl: u.photoUrl || null })),
      ...guestUsers.map(u => ({ name: u.name + ' 👤', wins: u.wins, gamesPlayed: u.gamesPlayed, photoUrl: null })),
    ];

    // Sort by wins desc, then gamesPlayed asc, limit to 50
    merged.sort((a, b) => b.wins - a.wins || a.gamesPlayed - b.gamesPlayed);
    const top50 = merged.slice(0, 50);

    res.json({
      leaderboard: top50.map((u, i) => ({
        rank: i + 1,
        name: u.name,
        wins: u.wins,
        gamesPlayed: u.gamesPlayed,
        photoUrl: u.photoUrl,
      })),
    });
  } catch (err) {
    console.error('Leaderboard error:', err);
    res.status(500).json({ error: 'Failed to load leaderboard' });
  }
});

module.exports = router;
