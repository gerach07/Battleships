const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// In production, use GOOGLE_APPLICATION_CREDENTIALS env var pointing to a service account JSON
// In development, you can set FIREBASE_SERVICE_ACCOUNT_JSON env var with the JSON string
let initialized = false;

function initFirebase() {
  if (initialized) return;
  
  try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp({
        credential: admin.credential.applicationDefault(),
      });
    } else {
      // Fallback: try default credentials (works on GCP/Firebase hosting)
      admin.initializeApp();
    }
    initialized = true;
    console.log('✅ Firebase Admin SDK initialized');
  } catch (err) {
    console.error('⚠️  Firebase Admin SDK initialization failed:', err.message);
    console.error('   Auth features will be disabled. Set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS.');
  }
}

/**
 * Verify a Firebase ID token and return the decoded user info.
 * Returns null if verification fails or Firebase is not initialized.
 */
async function verifyToken(idToken) {
  if (!initialized) return null;
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return decoded;
  } catch (err) {
    return null;
  }
}

/**
 * Express middleware that optionally authenticates requests.
 * If a valid Bearer token is present, req.user is set.
 * If no token or invalid token, req.user is null (guest mode).
 */
function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    req.user = null;
    return next();
  }
  const token = authHeader.split('Bearer ')[1];
  verifyToken(token).then(decoded => {
    req.user = decoded || null;
    next();
  }).catch(() => {
    req.user = null;
    next();
  });
}

/**
 * Express middleware that requires authentication.
 * Returns 401 if no valid token is present.
 */
function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }
  const token = authHeader.split('Bearer ')[1];
  verifyToken(token).then(decoded => {
    if (!decoded) return res.status(401).json({ error: 'Invalid or expired token' });
    req.user = decoded;
    next();
  }).catch(() => {
    res.status(401).json({ error: 'Authentication failed' });
  });
}

module.exports = { initFirebase, verifyToken, optionalAuth, requireAuth };
