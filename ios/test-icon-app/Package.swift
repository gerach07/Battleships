// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "test-icon-app",
    platforms: [
        .iOS(.v17),
        .macOS(.v14),
    ],
    products: [
        // An xtool project should contain exactly one library product,
        // representing the main app.
        .library(
            name: "test_icon_app",
            targets: ["test_icon_app"]
        ),
    ],
    targets: [
        .target(
            name: "test_icon_app"
        ),
    ]
)
