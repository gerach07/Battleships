import AVFoundation
import Foundation

final class SoundManager {
    static let shared = SoundManager()
    var enabled = true

    private let sampleRate: Double = 44100
    private let engine = AVAudioEngine()
    private let mixerNode: AVAudioMixerNode
    private var engineRunning = false
    private let queue = DispatchQueue(label: "SoundManager", qos: .userInteractive)

    private init() {
        mixerNode = engine.mainMixerNode
    }

    private func ensureEngine() {
        guard !engineRunning else { return }
        do {
            try engine.start()
            engineRunning = true
        } catch {}
    }

    private func playTone(freq: Float, durationMs: Int, volume: Float = 0.3) {
        guard enabled else { return }
        queue.async { [self] in
            ensureEngine()

            let dur = Float(durationMs) / 1000.0
            let numSamples = Int(sampleRate * Double(dur))

            guard let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1),
                  let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(numSamples)) else { return }
            buffer.frameLength = AVAudioFrameCount(numSamples)
            if let channelData = buffer.floatChannelData?[0] {
                for i in 0..<numSamples {
                    let t = Float(i) / Float(sampleRate)
                    let envelope = max(1.0 - t / dur, 0.001)
                    channelData[i] = volume * envelope * sin(2.0 * .pi * freq * t)
                }
            }

            let player = AVAudioPlayerNode()
            engine.attach(player)
            engine.connect(player, to: mixerNode, format: format)
            player.scheduleBuffer(buffer) { [weak engine] in
                DispatchQueue.global().async {
                    engine?.detach(player)
                }
            }
            player.play()
        }
    }

    func playHit() {
        playTone(freq: 800, durationMs: 150, volume: 0.2)
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.08) { [self] in
            playTone(freq: 600, durationMs: 100, volume: 0.15)
        }
    }

    func playMiss() { playTone(freq: 300, durationMs: 250, volume: 0.12) }

    func playSunk() {
        playTone(freq: 200, durationMs: 300, volume: 0.2)
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.15) { [self] in
            playTone(freq: 150, durationMs: 400, volume: 0.25)
        }
    }

    func playVictory() {
        let freqs: [Float] = [523, 659, 784, 1047]
        for (i, f) in freqs.enumerated() {
            DispatchQueue.global().asyncAfter(deadline: .now() + Double(i) * 0.15) { [self] in
                playTone(freq: f, durationMs: 300, volume: 0.25)
            }
        }
    }

    func playDefeat() {
        let freqs: [Float] = [400, 350, 300, 250]
        for (i, f) in freqs.enumerated() {
            DispatchQueue.global().asyncAfter(deadline: .now() + Double(i) * 0.2) { [self] in
                playTone(freq: f, durationMs: 350, volume: 0.2)
            }
        }
    }

    func playChat() { playTone(freq: 1200, durationMs: 50, volume: 0.08) }

    func playTurn() {
        playTone(freq: 880, durationMs: 100, volume: 0.15)
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) { [self] in
            playTone(freq: 1100, durationMs: 150, volume: 0.15)
        }
    }

    func playPlace() { playTone(freq: 500, durationMs: 80, volume: 0.1) }
}
