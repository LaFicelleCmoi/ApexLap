// swift-tools-version: 6.0
import PackageDescription

// Platform-independent core of the iOS app (domain + data). It builds on Linux too, so the
// business logic is tested in CI without a simulator.
let package = Package(
    name: "ApexLapKit",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "ApexLapDomain", targets: ["ApexLapDomain"]),
        .library(name: "ApexLapData", targets: ["ApexLapData"]),
        .library(name: "ApexLapLive", targets: ["ApexLapLive"]),
    ],
    targets: [
        .target(name: "ApexLapDomain"),
        // Bundled world champions 1950–2025 (same file as the Android asset).
        .target(name: "ApexLapData", dependencies: ["ApexLapDomain"], resources: [.copy("Resources/champions.json")]),
        .target(name: "ApexLapLive", dependencies: ["ApexLapData"]),
        .testTarget(
            name: "ApexLapKitTests",
            dependencies: ["ApexLapDomain", "ApexLapData", "ApexLapLive"],
            resources: [.copy("Fixtures")]
        ),
    ]
)
