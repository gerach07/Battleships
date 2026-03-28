// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Battleships",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "Battleships", targets: ["Battleships"]),
    ],
    dependencies: [
        .package(url: "https://github.com/socketio/socket.io-client-swift", from: "16.1.1"),
    ],
    targets: [
        .target(
            name: "Battleships",
            dependencies: [
                .product(name: "SocketIO", package: "socket.io-client-swift"),
            ],
            path: "Battleships",
            exclude: ["Info.plist", "Resources/AppIcon120.png", "Resources/AppIcon180.png", "Resources/AppIcon1024.png",
                      "Resources/bgm_menu.ogg", "Resources/bgm_placement.ogg", "Resources/bgm_battle.ogg",
                      "Resources/bgm_victory.ogg", "Resources/bgm_defeat.ogg",
                      "Resources/ship-sink-explosion.webp"],
            resources: [
                .process("Resources/Assets.xcassets"),
                .copy("Resources/bgm_menu.m4a"),
                .copy("Resources/bgm_placement.m4a"),
                .copy("Resources/bgm_battle.m4a"),
                .copy("Resources/bgm_victory.m4a"),
                .copy("Resources/bgm_defeat.m4a"),
                .copy("Resources/ship-sink-explosion.webp"),
            ]
        ),
    ]
)
