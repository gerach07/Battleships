# Battleships iOS

A SwiftUI iOS client for the AB Games Battleships multiplayer game.

## Requirements

- **macOS 13+** with **Xcode 15+**
- **iOS 16+** deployment target
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) (optional, for generating .xcodeproj)

## Setup

### Option A: Using XcodeGen (recommended)

1. Install XcodeGen:
   ```bash
   brew install xcodegen
   ```

2. Generate the Xcode project:
   ```bash
   cd ios
   xcodegen generate
   ```

3. Open `Battleships.xcodeproj` in Xcode.

4. The Socket.IO Swift Package dependency will be resolved automatically.

5. Select your team in **Signing & Capabilities**, then build and run on a simulator or device.

### Option B: Manual Xcode setup

1. Open Xcode → **File → New → Project → App**
2. Name it `Battleships`, Interface: **SwiftUI**, Language: **Swift**
3. Delete the auto-generated `ContentView.swift` and `BattleshipsApp.swift`
4. Drag all files from the `Battleships/` folder into the project navigator
5. **File → Add Package Dependencies** → enter:
   ```
   https://github.com/socketio/socket.io-client-swift
   ```
   - Rule: **Branch → master**
   - Add `SocketIO` library to the `Battleships` target
6. Set minimum deployment target to **iOS 16.0**
7. In Info.plist / project settings, ensure:
   - `NSAppTransportSecurity` → `NSAllowsArbitraryLoads` = YES
   - `UIBackgroundModes` → `audio`
8. Build and run

## Project Structure

```
Battleships/
├── BattleshipsApp.swift          # @main App entry point
├── Info.plist                     # App configuration
├── Data/
│   ├── Constants.swift            # Grid size, ships, cell states, server URL
│   └── Models.swift               # Board, RoomInfo, ChatMessage, PlacedShip
├── Networking/
│   └── SocketManager.swift        # Socket.IO wrapper singleton
├── ViewModel/
│   └── GameViewModel.swift        # All game state + socket event handling
├── Util/
│   ├── GameHelpers.swift          # Board logic, placement, parsing
│   ├── SoundManager.swift         # AVAudioEngine-based tone synthesis
│   └── MusicManager.swift         # AVAudioPlayer with crossfade
├── I18n/
│   └── Strings.swift              # EN/LV/RU translations
├── UI/
│   ├── ContentView.swift          # Phase router + header + message toast
│   ├── Screens/
│   │   ├── LoginScreen.swift      # Menu, create, join, PIN, name entry
│   │   ├── WaitingRoomScreen.swift# Room code, player slots, host controls
│   │   ├── PlacementScreen.swift  # Ship placement grid + controls
│   │   ├── BattleScreen.swift     # Two boards, timer, scoreboard
│   │   └── GameOverScreen.swift   # Victory/defeat, play again
│   └── Components/
│       ├── GameBoard.swift        # Reusable 10×10 grid
│       ├── GameTimer.swift        # Live countdown timer
│       ├── ChatOverlay.swift      # In-game chat panel
│       ├── ConnectionOverlay.swift# Disconnection overlay
│       └── BackgroundShips.swift  # Animated ship emojis
└── Resources/
    └── Assets.xcassets/           # App icon assets
```

## Features (matching Web & Android)

- Full multiplayer via Socket.IO (create, join, spectate)
- Ship placement with tap-to-place, random placement
- Real-time battle with turn-based shooting
- In-game chat with /imp command for important messages
- Reconnection handling with game state restoration
- Host controls (start game, kick player)
- Play again / rematch flow
- Live countdown timers per player
- 3 languages: English, Latvian, Russian
- Sound effects via tone synthesis (no audio files needed)
- Background music support (add .m4a/.mp3 files to Resources)
- Background ship animations
- Haptic feedback on hits

## Server

The app connects to `https://battleships-server-jtit.onrender.com` by default.

For local development, change `SERVER_URL` in `Data/Constants.swift`:
```swift
let SERVER_URL = "http://localhost:3001"
```

## Music Files

To add background music, place audio files in `Resources/` and name them:
- `bgm_menu.m4a`
- `bgm_placement.m4a`
- `bgm_battle.m4a`
- `bgm_victory.m4a`
- `bgm_defeat.m4a`

Supported formats: `.m4a`, `.mp3`, `.caf`, `.wav`

The music system will automatically play the appropriate track per game phase.
