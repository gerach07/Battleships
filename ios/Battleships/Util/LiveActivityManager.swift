import Foundation
import os.log
#if canImport(ActivityKit)
import ActivityKit

private let log = OSLog(subsystem: "com.abgames.battleships", category: "LiveActivity")

/// Manages the lifecycle of a single Battleships Live Activity (Dynamic Island + Lock Screen).
@available(iOS 16.2, *)
final class LiveActivityManager {
    static let shared = LiveActivityManager()
    private var currentActivity: Activity<BattleshipsAttributes>?
    private init() {}

    var isActive: Bool { currentActivity != nil }

    func start(roomCode: String, playerName: String, state: BattleshipsAttributes.ContentState) {
        _ = startWithResult(roomCode: roomCode, playerName: playerName, state: state)
    }

    func startWithResult(roomCode: String, playerName: String, state: BattleshipsAttributes.ContentState) -> String {
        // End stale activity if any
        if currentActivity != nil { endImmediately() }

        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            return "DISABLED in Settings"
        }

        let attributes = BattleshipsAttributes(roomCode: roomCode, playerName: playerName)

        do {
            currentActivity = try Activity.request(
                attributes: attributes,
                content: .init(state: state, staleDate: Date().addingTimeInterval(600)),
                pushType: nil
            )
            return "OK id=\(currentActivity?.id ?? "nil")"
        } catch {
            return "ERROR: \(error)"
        }
    }

    func update(state: BattleshipsAttributes.ContentState) {
        guard let activity = currentActivity else { return }
        Task {
            await activity.update(.init(state: state, staleDate: Date().addingTimeInterval(600)))
        }
    }

    /// End with final state shown for 30 seconds, then dismiss.
    func end(state: BattleshipsAttributes.ContentState) {
        guard let activity = currentActivity else { return }
        let finalContent = ActivityContent(state: state, staleDate: nil)
        Task {
            await activity.end(finalContent, dismissalPolicy: .after(.now + 30))
        }
        currentActivity = nil
    }

    func endImmediately() {
        guard let activity = currentActivity else { return }
        Task { await activity.end(dismissalPolicy: .immediate) }
        currentActivity = nil
    }
}
#endif
