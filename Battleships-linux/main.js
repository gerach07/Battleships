/**
 * ============================================================================
 * AB BATTLESHIPS — Electron Main Process
 * ============================================================================
 * Wraps the React web client in a native Linux desktop window.
 * Connects to the production game server for multiplayer.
 */

const { app, BrowserWindow, Menu, shell, dialog, nativeImage } = require('electron');
const path = require('path');

// Prevent multiple instances
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
}

// ── Globals ─────────────────────────────────────────
let mainWindow = null;

const APP_NAME = 'AB Battleships';
const MIN_WIDTH = 960;
const MIN_HEIGHT = 680;
const DEFAULT_WIDTH = 1280;
const DEFAULT_HEIGHT = 800;

// ── Create Window ───────────────────────────────────
function createWindow() {
  mainWindow = new BrowserWindow({
    title: APP_NAME,
    width: DEFAULT_WIDTH,
    height: DEFAULT_HEIGHT,
    minWidth: MIN_WIDTH,
    minHeight: MIN_HEIGHT,
    icon: getIconPath(),
    backgroundColor: '#0f172a',
    show: false,          // show when ready to avoid flicker
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      spellcheck: false,
    },
  });

  // Load the built React app
  const indexPath = path.join(__dirname, 'renderer', 'index.html');
  mainWindow.loadFile(indexPath);

  // Graceful show
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();
  });

  // Open external links in system browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http')) {
      shell.openExternal(url);
    }
    return { action: 'deny' };
  });

  // Prevent navigation to external URLs inside the app window
  mainWindow.webContents.on('will-navigate', (event, url) => {
    const appUrl = `file://${path.join(__dirname, 'renderer')}`;
    if (!url.startsWith(appUrl) && !url.startsWith('file://')) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Build application menu
  buildMenu();
}

// ── Icon Path ───────────────────────────────────────
function getIconPath() {
  // In production (installed), icons are in the resources directory
  const iconCandidates = [
    path.join(__dirname, 'assets', 'icon.png'),
    path.join(__dirname, '..', 'assets', 'icon.png'),
    '/usr/share/icons/hicolor/256x256/apps/abbattleships.png',
  ];
  for (const p of iconCandidates) {
    try {
      require('fs').accessSync(p);
      return p;
    } catch { /* continue */ }
  }
  return undefined;
}

// ── Application Menu ────────────────────────────────
function buildMenu() {
  const template = [
    {
      label: APP_NAME,
      submenu: [
        {
          label: 'About AB Battleships',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: 'About AB Battleships',
              message: 'AB Battleships',
              detail: `Version ${app.getVersion()}\n\nA multiplayer battleships game.\nSink enemy ships before they sink yours!\n\n© 2024-2026 AB Battleships`,
            });
          },
        },
        { type: 'separator' },
        {
          label: 'New Game Window',
          accelerator: 'CmdOrCtrl+N',
          click: () => { createWindow(); },
        },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
      ],
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'How to Play',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: 'How to Play',
              message: 'How to Play Battleships',
              detail:
                '1. Create or join a game room\n' +
                '2. Place your ships on the board\n' +
                '3. Take turns firing at your opponent\'s grid\n' +
                '4. Sink all enemy ships to win!\n\n' +
                'Ships: Carrier (5), Battleship (4), Destroyer (3), Submarine (3), Patrol (2)',
            });
          },
        },
        { type: 'separator' },
        {
          label: 'Report Issue',
          click: () => shell.openExternal('https://github.com'),
        },
      ],
    },
  ];

  // Add DevTools in development
  if (!app.isPackaged) {
    template[1].submenu.push(
      { type: 'separator' },
      { role: 'toggleDevTools' },
    );
  }

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

// ── App Lifecycle ───────────────────────────────────

app.on('ready', createWindow);

app.on('second-instance', () => {
  // Focus existing window when user tries to open a second instance
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.focus();
  }
});

app.on('window-all-closed', () => {
  app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// Security: block creating new webviews
app.on('web-contents-created', (_event, contents) => {
  contents.on('will-attach-webview', (event) => {
    event.preventDefault();
  });
});
