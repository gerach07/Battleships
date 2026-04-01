import AVFoundation
import Foundation

final class MusicManager: ObservableObject {
    static let shared = MusicManager()

    @Published var currentTrackName: String? = nil

    private static let trackNames: [String: String] = [
        "bgm_menu":      "The Price of Freedom",
        "bgm_placement": "Beyond New Horizons",
        "bgm_battle":    "Honor and Sword",
        "bgm_victory":   "Victory",
        "bgm_defeat":    "Waves Crash",
    ]

    @Published var enabled = false {
        didSet {
            if !enabled { stopMusic() }
            else { resumeMusic() }
            UserDefaults.standard.set(enabled, forKey: "battleships-music")
        }
    }

    private static let FADE_DURATION: TimeInterval = 0.4
    private static let FADE_STEPS = 16
    private static let VOLUME: Float = 0.35

    private var player: AVAudioPlayer?
    private var currentTrack: String?
    private var currentLoop: Bool = true
    private var fadeTimer: Timer?

    private init() {
        enabled = UserDefaults.standard.bool(forKey: "battleships-music")
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("[MusicManager] Failed to configure audio session: \(error.localizedDescription)")
        }
    }

    private func playTrack(_ name: String, loop: Bool = true) {
        if !enabled {
            currentTrack = name
            currentLoop = loop
            return
        }
        if currentTrack == name, player?.isPlaying == true { return }

        // Fade out old track then start new
        if let oldPlayer = player, oldPlayer.isPlaying {
            fadeOutAndRelease(oldPlayer) { [weak self] in
                self?.startNewTrack(name, loop: loop)
            }
            player = nil
        } else {
            stopMusicNow()
            startNewTrack(name, loop: loop)
        }
    }

    private func startNewTrack(_ name: String, loop: Bool) {
        currentTrack = name
        currentLoop = loop
        currentTrackName = Self.trackNames[name]

        guard let url = Bundle.main.url(forResource: name, withExtension: "m4a")
            ?? Bundle.main.url(forResource: name, withExtension: "mp3")
            ?? Bundle.main.url(forResource: name, withExtension: "caf")
        else { return }

        do {
            player = try AVAudioPlayer(contentsOf: url)
            player?.numberOfLoops = loop ? -1 : 0
            player?.volume = Self.VOLUME
            player?.play()
        } catch {
            print("[MusicManager] Failed to play track \(name): \(error.localizedDescription)")
        }
    }

    private func fadeOutAndRelease(_ p: AVAudioPlayer, onDone: @escaping () -> Void) {
        cancelFade()
        let stepTime = Self.FADE_DURATION / Double(Self.FADE_STEPS)
        var step = 0
        fadeTimer = Timer.scheduledTimer(withTimeInterval: stepTime, repeats: true) { [weak self] timer in
            step += 1
            let vol = Self.VOLUME * Float(1.0 - Double(step) / Double(Self.FADE_STEPS))
            p.volume = max(vol, 0)
            if step >= Self.FADE_STEPS {
                timer.invalidate()
                self?.fadeTimer = nil
                p.stop()
                onDone()
            }
        }
    }

    private func cancelFade() {
        fadeTimer?.invalidate()
        fadeTimer = nil
    }

    func resumeMusic() {
        guard enabled else { return }
        if player?.isPlaying == true { return }
        if let p = player {
            p.play()
            return
        }
        if let track = currentTrack {
            startNewTrack(track, loop: currentLoop)
        }
    }

    func pauseMusic() {
        player?.pause()
    }

    func stopMusic() {
        cancelFade()
        stopMusicNow()
    }

    private func stopMusicNow() {
        player?.stop()
        player = nil
        currentTrackName = nil
    }

    // Phase-specific triggers
    func playMenuMusic()      { playTrack("bgm_menu", loop: true) }
    func playPlacementMusic() { playTrack("bgm_placement", loop: true) }
    func playBattleMusic()    { playTrack("bgm_battle", loop: true) }
    func playVictoryMusic()   { playTrack("bgm_victory", loop: false) }
    func playDefeatMusic()    { playTrack("bgm_defeat", loop: false) }
}
