// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Battleships",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "Battleships", targets: ["Battleships"]),
        .library(name: "BattleshipsWidget", targets: ["BattleshipsWidget"]),
    ],
    dependencies: [
        .package(url: "https://github.com/socketio/socket.io-client-swift", from: "16.1.1"),
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "10.22.0"),
        .package(url: "https://github.com/google/GoogleSignIn-iOS.git", from: "7.1.0"),
    ],
    targets: [
        .target(
            name: "Battleships",
            dependencies: [
                .product(name: "SocketIO", package: "socket.io-client-swift"),
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS"),
            ],
            path: "Battleships",
            exclude: ["Info.plist", "Resources/AppIcon120.png", "Resources/AppIcon180.png", "Resources/AppIcon1024.png",
                      "Resources/bgm_menu.m4a", "Resources/bgm_placement.m4a", "Resources/bgm_battle.m4a",
                      "Resources/bgm_victory.m4a", "Resources/bgm_defeat.m4a"],
            resources: [
                .process("Resources/Assets.xcassets"),
                .process("GoogleService-Info.plist"),
            ]
        ),
        .target(
            name: "BattleshipsWidget",
            path: "BattleshipsWidget"
        ),
    ]
)
