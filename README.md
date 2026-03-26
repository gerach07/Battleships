# ⚓ Battleships — Multiplayer Naval Combat

A real-time multiplayer Battleships game available on **Web**, **Android**, and **iOS**.

> **Play now:** [abbattleships.web.app](https://abbattleships.web.app)

---

## Platforms

| Platform | Tech Stack | Path |
|----------|-----------|------|
| **Web** | React 18 · Tailwind CSS · Socket.IO | [`web/`](web/) |
| **Android** | Kotlin · Jetpack Compose · Socket.IO | [`android/`](android/) |
| **iOS** | SwiftUI · Socket.IO Swift | [`ios/`](ios/) |
| **Server** | Node.js · Express · Socket.IO | [`web/server/`](web/server/) |

## Features

- **Create & join** rooms with a 4-digit PIN code
- **Host controls** — start game, kick players
- **Ship placement** — tap-to-place or random placement
- **Real-time battle** — turn-based shooting with live timers
- **Spectator mode** — watch ongoing games
- **In-game chat** with `/imp` important messages
- **Reconnection** — seamless game state restoration on disconnect
- **Play again / rematch** flow after game ends
- **3 languages** — English, Latvian, Russian
- **Sound effects** — synthesized tones (no audio files needed)
- **Background music** with crossfade transitions
- **Animated background ships** on menu screens

## Quick Start

### Web (Client + Server)

```bash
cd web
npm run install:all
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) — the server runs on port 3001.

### Android

Open `android/` in Android Studio, sync Gradle, and run on a device/emulator (API 24+).

### iOS

```bash
cd ios
brew install xcodegen    # if not installed
xcodegen generate
open Battleships.xcodeproj
```

Requires **macOS 13+**, **Xcode 15+**, targets **iOS 16+**.

## Server

The shared backend is deployed on **Render** and used by all platforms:

```
https://battleships-server-jtit.onrender.com
```

For local development, update the server URL in:
- **Web:** `web/client/src/constants.js`
- **Android:** `android/app/src/main/java/com/abbattleships/network/SocketManager.kt`
- **iOS:** `ios/Battleships/Data/Constants.swift`

## Project Structure

```
├── web/                    # Web client + shared server
│   ├── client/             # React frontend (Firebase hosted)
│   ├── server/             # Node.js/Express/Socket.IO backend
│   ├── firebase.json
│   └── package.json
├── android/                # Android native client
│   ├── app/src/main/       # Kotlin + Jetpack Compose
│   ├── build.gradle.kts
│   └── gradle/
├── ios/                    # iOS native client
│   ├── Battleships/        # SwiftUI source
│   ├── Package.swift       # SPM dependencies
│   ├── project.yml         # XcodeGen config
│   └── README.md
└── README.md               # This file
```

## Releases

Check the [Releases](https://github.com/gerach07/Battleships/releases) page for:
- **Android APK** — install directly on your device
- **Source code** archives (ZIP / TAR.GZ)

## License

All rights reserved © AB Games
