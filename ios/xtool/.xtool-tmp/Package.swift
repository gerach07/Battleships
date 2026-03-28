// swift-tools-version: 6.0
import PackageDescription
let package = Package(
    name: "Battleships-Builder",
    platforms: [
        .iOS("16.0"),
    ],
    dependencies: [
        .package(name: "RootPackage", path: "../.."),
    ],
    targets: [
        .executableTarget(
    name: "Battleships-App",
    dependencies: [
        .product(name: "Battleships", package: "RootPackage"),
    ],
    linkerSettings: [
    .unsafeFlags([
        "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks",
    ]),
]
)
    ]
)
