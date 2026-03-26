import AVFoundation
import Foundation

final class SoundManager {
    static let shared = SoundManager()
    var enabled = true

    private let sampleRate: Double = 44100

    private init() {}

    private func playTone(freq: Float, durationMs: Int, volume: Float = 0.3) {
        guard enabled else { return }
        DispatchQueue.global(qos: .userInteractive).async { [sampleRate] in
            let dur = Float(durationMs) / 1000.0
            let numSamples = Int(sampleRate * Double(dur))
            var samples = [Float](repeating: 0, count: numSamples)
            for i in 0..<numSamples {
                let t = Float(i) / Float(sampleRate)
                let envelope = max(1.0 - t / dur, 0.001)
                samples[i] = volume * envelope * sin(2.0 * .pi * freq * t)
            }

            let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
            guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(numSamples)) else { return }
            buffer.frameLength = AVAudioFrameCount(numSamples)
            if let channelData = buffer.floatChannelData?[0] {
                for i in 0..<numSamples { channelData[i] = samples[i] }
            }

            let engine = AVAudioEngine()
            let player = AVAudioPlayerNode()
            engine.attach(player)
            engine.connect(player, to: engine.mainMixerNode, format: format)
            do {
                try engine.start()
                player.scheduleBuffer(buffer, completionHandler: nil)
                player.play()
                Thread.sleep(forTimeInterval: Double(dur) + 0.05)
                engine.stop()
            } catch {}
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
