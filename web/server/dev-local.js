const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const GuestPlayer = require('./src/models/GuestPlayer');

const samplePlayers = [
  { name: 'Northstar', wins: 18, gamesPlayed: 29 },
  { name: 'Kestrel', wins: 14, gamesPlayed: 24 },
  { name: 'Bluefin', wins: 11, gamesPlayed: 21 },
  { name: 'Mariner', wins: 8, gamesPlayed: 16 },
  { name: 'Wayfinder', wins: 5, gamesPlayed: 12 },
  { name: 'Tidewatch', wins: 2, gamesPlayed: 8 },
];

let mongoServer;
let serverProcess;
let shuttingDown = false;

async function stop(signal) {
  if (shuttingDown) return;
  shuttingDown = true;
  if (serverProcess && serverProcess.exitCode === null) {
    serverProcess.kill(signal);
  } else if (mongoServer) {
    await mongoServer.stop();
  }
}

async function main() {
  const databasePath = path.join(__dirname, '.local-mongo');
  fs.mkdirSync(databasePath, { recursive: true });
  mongoServer = await MongoMemoryServer.create({
    instance: {
      dbName: 'battleships_local',
      dbPath: databasePath,
      storageEngine: 'wiredTiger',
    },
  });

  const mongoUri = mongoServer.getUri('battleships_local');
  await mongoose.connect(mongoUri);
  await GuestPlayer.bulkWrite(samplePlayers.map(player => ({
    updateOne: {
      filter: { name: player.name },
      update: {
        $setOnInsert: {
          ...player,
          createdAt: new Date(),
          lastSeenAt: new Date(),
        },
      },
      upsert: true,
    },
  })), { ordered: false });
  await mongoose.disconnect();

  console.log('Local MongoDB ready at .local-mongo');
  console.log(`Seeded sample guest leaderboard players: ${samplePlayers.length} (existing records preserved)`);

  serverProcess = spawn(process.execPath, ['server.js'], {
    cwd: __dirname,
    env: {
      ...process.env,
      NODE_ENV: 'development',
      MONGODB_URI: mongoUri,
    },
    stdio: 'inherit',
  });

  serverProcess.on('error', async error => {
    console.error('Failed to launch local Battleships server:', error);
    await mongoServer.stop();
    process.exitCode = 1;
  });

  serverProcess.on('exit', async code => {
    await mongoServer.stop();
    process.exitCode = code ?? 0;
  });
}

process.on('SIGINT', () => stop('SIGINT'));
process.on('SIGTERM', () => stop('SIGTERM'));

main().catch(async error => {
  console.error('Failed to start local MongoDB:', error);
  await mongoose.disconnect().catch(() => {});
  await mongoServer?.stop().catch(() => {});
  process.exitCode = 1;
});
