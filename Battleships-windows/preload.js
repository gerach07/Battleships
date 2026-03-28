/**
 * Electron preload script — runs in a secure, isolated context.
 * Exposes minimal APIs to the renderer (React app) via contextBridge.
 */

const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  isElectron: true,
  appVersion: process.env.npm_package_version || '1.0.0',
});
