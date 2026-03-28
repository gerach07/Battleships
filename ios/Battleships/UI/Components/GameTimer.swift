import SwiftUI
import Combine

struct GameTimerView: View {
    let playerTimeLeft: [String: Double]
    let turnStartedAt: Double?
    let currentTurn: String?
    let myId: String
    let opponentName: String
    let strings: I18nStrings
    var spectatorPlayerNames: [String: String] = [:]

    @State private var tick = 0
    @State private var timer: AnyCancellable?

    var body: some View {
        let _ = tick // force SwiftUI recomposition on timer tick
        let ids = orderedIds
        HStack(spacing: 8) {
            ForEach(ids, id: \.self) { pid in
                PlayerTimerCell(
                    pid: pid,
                    myId: myId,
                    opponentName: opponentName,
                    spectatorName: spectatorPlayerNames[pid],
                    isActive: pid == currentTurn,
                    liveTime: liveTime(for: pid),
                    strings: strings
                )
            }
        }
        .onAppear { startTimer() }
        .onDisappear { timer?.cancel() }
        .onChange(of: currentTurn) { _ in startTimer() }
    }

    private func liveTime(for pid: String) -> Double {
        let isActive = pid == currentTurn
        let stored = playerTimeLeft[pid] ?? 0
        if isActive, let start = turnStartedAt {
            return max(0, stored - (Date().timeIntervalSince1970 * 1000 - start) / 1000)
        }
        return stored
    }

    private var orderedIds: [String] {
        let ids = Array(playerTimeLeft.keys)
        if ids.contains(myId) {
            return [myId] + ids.filter { $0 != myId }
        }
        return ids
    }

    private func startTimer() {
        timer?.cancel()
        guard currentTurn != nil, turnStartedAt != nil else { return }
        timer = Timer.publish(every: 0.5, on: .main, in: .common)
            .autoconnect()
            .sink { _ in tick += 1 }
    }

    private func formatTime(_ seconds: Double) -> String {
        let s = max(0, Int(ceil(seconds)))
        return "\(s / 60):\(String(format: "%02d", s % 60))"
    }
}

private struct PlayerTimerCell: View {
    let pid: String
    let myId: String
    let opponentName: String
    let spectatorName: String?
    let isActive: Bool
    let liveTime: Double
    let strings: I18nStrings

    private var isCritical: Bool { isActive && liveTime <= 10 }
    private var isLow: Bool { isActive && liveTime <= 30 }
    private var label: String {
        if let name = spectatorName { return name }
        return pid == myId ? strings.you : (opponentName.isEmpty ? strings.opponent : opponentName)
    }
    private var timeColor: Color {
        guard isActive else { return .gray }
        if isCritical { return .red }
        if isLow { return .orange }
        return .green
    }
    private var bgColor: Color {
        guard isActive else { return Color.white.opacity(0.04) }
        if isCritical { return Color.red.opacity(0.2) }
        if isLow { return Color.orange.opacity(0.15) }
        return Color.green.opacity(0.1)
    }
    private var borderColor: Color {
        guard isActive else { return Color.white.opacity(0.08) }
        if isCritical { return Color.red.opacity(0.4) }
        if isLow { return Color.orange.opacity(0.3) }
        return Color.green.opacity(0.3)
    }
    private func formatTime(_ seconds: Double) -> String {
        let s = max(0, Int(ceil(seconds)))
        return "\(s / 60):\(String(format: "%02d", s % 60))"
    }

    var body: some View {
        VStack(spacing: 4) {
            Text(label.uppercased())
                .font(.system(size: 9, weight: .bold))
                .tracking(1)
                .foregroundColor(.white.opacity(0.6))

            Text(formatTime(liveTime))
                .font(.system(size: 20, weight: .bold, design: .monospaced))
                .foregroundColor(timeColor)

            if isActive {
                Text(strings.ticking)
                    .font(.system(size: 8))
                    .foregroundColor(.white.opacity(0.4))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(bgColor)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(borderColor, lineWidth: 1))
        )
    }
}
