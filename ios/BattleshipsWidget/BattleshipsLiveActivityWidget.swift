import WidgetKit
import SwiftUI
#if canImport(ActivityKit)
import ActivityKit

// MARK: - Shared Attributes (must match main app's BattleshipsAttributes exactly)
struct BattleshipsAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var phase: String
        var isMyTurn: Bool
        var myTime: Double
        var opponentTime: Double
        var mySunkCount: Int
        var theirSunkCount: Int
        var lastShot: String?
        var iWon: Bool?
        var opponentName: String
    }
    var roomCode: String
    var playerName: String
}

// MARK: - Widget Bundle Entry Point
@main
struct BattleshipsWidgetBundle: WidgetBundle {
    var body: some Widget {
        if #available(iOS 16.1, *) {
            BattleshipsLiveActivity()
        }
    }
}

// MARK: - Live Activity Widget
@available(iOS 16.1, *)
struct BattleshipsLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: BattleshipsAttributes.self) { context in
            // Lock Screen / StandBy banner
            LockScreenBannerView(
                state: context.state,
                roomCode: context.attributes.roomCode,
                playerName: context.attributes.playerName
            )
            .padding(12)
            .activityBackgroundTint(Color(red: 0.08, green: 0.15, blue: 0.35))
        } dynamicIsland: { context in
            DynamicIsland {
                // ── Expanded Regions ──
                DynamicIslandExpandedRegion(.leading) {
                    ExpandedPlayerTimer(
                        label: "You",
                        time: context.state.myTime,
                        isActive: context.state.isMyTurn && context.state.phase == "battle",
                        tint: context.state.isMyTurn ? .green : .secondary
                    )
                }
                DynamicIslandExpandedRegion(.trailing) {
                    ExpandedPlayerTimer(
                        label: String(context.state.opponentName.prefix(10)),
                        time: context.state.opponentTime,
                        isActive: !context.state.isMyTurn && context.state.phase == "battle",
                        tint: !context.state.isMyTurn ? .orange : .secondary
                    )
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.attributes.roomCode)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ExpandedBottomView(state: context.state)
                }
            } compactLeading: {
                CompactLeadingView(state: context.state, roomCode: context.attributes.roomCode)
            } compactTrailing: {
                CompactTrailingView(state: context.state)
            } minimal: {
                MinimalView(state: context.state, roomCode: context.attributes.roomCode)
            }
        }
    }
}

// MARK: - Lock Screen Banner
private struct LockScreenBannerView: View {
    let state: BattleshipsAttributes.ContentState
    let roomCode: String
    let playerName: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header row
            HStack {
                Text("⚔️ Battleships")
                    .font(.headline).fontWeight(.bold)
                    .foregroundStyle(.white)
                Spacer()
                Text("Room: \(roomCode)")
                    .font(.caption).foregroundStyle(.white.opacity(0.7))
            }

            if state.phase == "waiting" {
                VStack(spacing: 4) {
                    Text(roomCode)
                        .font(.system(.title2, design: .monospaced))
                        .fontWeight(.bold)
                        .foregroundStyle(.yellow)
                        .tracking(4)
                    HStack {
                        ProgressView().tint(.white)
                        Text("Waiting for opponent…")
                            .font(.subheadline).foregroundStyle(.white.opacity(0.8))
                    }
                }
            } else if state.phase == "placement" {
                Text("🚢 Place your ships!")
                    .font(.subheadline).foregroundStyle(.cyan)
            } else if state.phase == "gameOver" {
                GameOverBanner(state: state)
            } else {
                // Battle phase
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("You").font(.caption2).foregroundStyle(.white.opacity(0.6))
                        TimerDisplay(seconds: state.myTime, isActive: state.isMyTurn, tint: .green)
                    }
                    Spacer()
                    Text("vs").font(.caption).foregroundStyle(.white.opacity(0.5))
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(state.opponentName)
                            .font(.caption2).foregroundStyle(.white.opacity(0.6))
                        TimerDisplay(seconds: state.opponentTime, isActive: !state.isMyTurn, tint: .orange)
                    }
                }
                HStack {
                    TurnIndicator(state: state)
                    Spacer()
                    Text("Sunk: \(state.theirSunkCount)/5 🚢")
                        .font(.caption).foregroundStyle(.white.opacity(0.8))
                    if let shot = state.lastShot {
                        Text(shotEmoji(shot))
                            .font(.caption)
                    }
                }
            }
        }
    }
}

// MARK: - Compact Views
private struct CompactLeadingView: View {
    let state: BattleshipsAttributes.ContentState
    var roomCode: String = ""
    var body: some View {
        Group {
            switch state.phase {
            case "gameOver":
                Text(state.iWon == true ? "🏆" : "💀")
            case "battle":
                Image(systemName: state.isMyTurn ? "scope" : "hourglass")
                    .foregroundStyle(state.isMyTurn ? .green : .orange)
            case "placement":
                Text("🚢")
            default:
                Text(roomCode)
                    .font(.system(.caption, design: .monospaced))
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
            }
        }
    }
}

private struct CompactTrailingView: View {
    let state: BattleshipsAttributes.ContentState
    var body: some View {
        Group {
            switch state.phase {
            case "gameOver":
                Text(state.iWon == true ? "WIN" : "LOSS")
                    .font(.caption2).fontWeight(.bold)
                    .foregroundStyle(state.iWon == true ? .green : .red)
            case "battle":
                if state.isMyTurn {
                    TimerDisplay(seconds: state.myTime, isActive: true, tint: .green)
                        .font(.system(.caption2, design: .monospaced))
                } else {
                    TimerDisplay(seconds: state.opponentTime, isActive: true, tint: .orange)
                        .font(.system(.caption2, design: .monospaced))
                }
            case "placement":
                Text("SETUP")
                    .font(.caption2).fontWeight(.bold)
                    .foregroundStyle(.cyan)
            default:
                Text("WAIT")
                    .font(.caption2).fontWeight(.bold)
                    .foregroundStyle(.yellow)
            }
        }
    }
}

// MARK: - Minimal View
private struct MinimalView: View {
    let state: BattleshipsAttributes.ContentState
    var roomCode: String = ""
    var body: some View {
        Group {
            switch state.phase {
            case "gameOver":
                Text(state.iWon == true ? "🏆" : "💀")
            case "battle":
                Image(systemName: "scope")
                    .foregroundStyle(state.isMyTurn ? .green : .gray)
            case "waiting":
                Text("⏳")
            default:
                Text("⚔️")
            }
        }
    }
}

// MARK: - Expanded Bottom
private struct ExpandedBottomView: View {
    let state: BattleshipsAttributes.ContentState
    var body: some View {
        switch state.phase {
        case "waiting":
            HStack {
                ProgressView().tint(.white)
                Text("Waiting for opponent…")
                    .font(.caption).foregroundStyle(.secondary)
            }
        case "placement":
            Text("🚢 Place your ships!")
                .font(.caption).foregroundStyle(.cyan)
        case "gameOver":
            GameOverBanner(state: state)
        default:
            HStack(spacing: 12) {
                TurnIndicator(state: state)
                Spacer()
                Text("🚢 \(state.theirSunkCount)/5")
                    .font(.caption2)
                if let shot = state.lastShot {
                    Text(shotEmoji(shot))
                        .font(.caption2)
                }
            }
        }
    }
}

// MARK: - Expanded Player Timer
private struct ExpandedPlayerTimer: View {
    let label: String
    let time: Double
    let isActive: Bool
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption2).fontWeight(.semibold)
                .foregroundStyle(.secondary)
            TimerDisplay(seconds: time, isActive: isActive, tint: tint)
                .font(.system(.title3, design: .monospaced)).fontWeight(.bold)
        }
    }
}

// MARK: - Shared Helpers
private struct TimerDisplay: View {
    let seconds: Double
    let isActive: Bool
    let tint: Color

    var body: some View {
        if isActive && seconds > 0 {
            Text(Date().addingTimeInterval(seconds), style: .timer)
                .monospacedDigit()
                .foregroundStyle(tint)
        } else {
            Text(formatTime(seconds))
                .monospacedDigit()
                .foregroundStyle(tint.opacity(0.7))
        }
    }

    private func formatTime(_ s: Double) -> String {
        let total = max(0, Int(s))
        return String(format: "%d:%02d", total / 60, total % 60)
    }
}

private struct TurnIndicator: View {
    let state: BattleshipsAttributes.ContentState
    var body: some View {
        if state.isMyTurn {
            Text("🎯 YOUR TURN")
                .font(.caption).fontWeight(.bold)
                .foregroundStyle(.green)
        } else {
            Text("⏳ \(state.opponentName)'s turn")
                .font(.caption)
                .foregroundStyle(.orange)
        }
    }
}

private struct GameOverBanner: View {
    let state: BattleshipsAttributes.ContentState
    var body: some View {
        if state.iWon == true {
            HStack {
                Text("🏆 VICTORY!")
                    .font(.headline).fontWeight(.bold)
                    .foregroundStyle(.green)
                Spacer()
                Text("Sunk all 5 ships")
                    .font(.caption).foregroundStyle(.white.opacity(0.7))
            }
        } else {
            HStack {
                Text("💀 DEFEAT")
                    .font(.headline).fontWeight(.bold)
                    .foregroundStyle(.red)
                Spacer()
                Text("Better luck next time")
                    .font(.caption).foregroundStyle(.white.opacity(0.7))
            }
        }
    }
}

private func shotEmoji(_ shot: String) -> String {
    switch shot {
    case "hit":  return "🔥 HIT"
    case "miss": return "💨 MISS"
    case "sunk": return "💀 SUNK"
    default:     return ""
    }
}
#endif
