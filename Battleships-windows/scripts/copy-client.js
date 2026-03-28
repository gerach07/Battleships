/**
 * Script to copy the built React client into the Electron app's renderer dir.
 * Run after `npm run build` in the web client.
 */

const fs = require('fs');
const path = require('path');

const SRC = path.resolve(__dirname, '..', '..', 'Battleships-web', 'client', 'build');
const DEST = path.resolve(__dirname, '..', 'renderer');

function copyRecursive(src, dest) {
  if (!fs.existsSync(src)) {
    console.error(`ERROR: Source directory not found: ${src}`);
    console.error('       Run "npm run build:client" first to build the React app.');
    process.exit(1);
  }

  if (fs.existsSync(dest)) {
    fs.rmSync(dest, { recursive: true, force: true });
  }
  fs.mkdirSync(dest, { recursive: true });

  const entries = fs.readdirSync(src, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyRecursive(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

console.log('📦 Copying React build to Electron renderer...');
console.log(`   From: ${SRC}`);
console.log(`   To:   ${DEST}`);
copyRecursive(SRC, DEST);
console.log('✅ Done! React client copied to renderer/');
