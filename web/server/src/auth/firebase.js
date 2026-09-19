const { initializeApp, cert, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');

let initialized = false;

function initFirebase() {
  if (initialized) return;
  
  try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      initializeApp({
        credential: cert(serviceAccount),
      });
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      initializeApp({
        credential: applicationDefault(),
      });
    } else {
      initializeApp();
    }
    initialized = true;
    console.log('✅ Firebase Admin SDK initialized');
  } catch (err) {
    console.error('⚠️  Firebase Admin SDK initialization failed:', err.message);
    console.error('   Auth features will be disabled. Set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS.');
  }
}

async function verifyToken(idToken) {
  if (!initialized) return null;
  try {
    const decoded = await getAuth().verifyIdToken(idToken);
    return decoded;
  } catch (err) {
    return null;
  }
}

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
