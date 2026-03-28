import SwiftUI

// MARK: - Credits sheet

struct CreditsSheet: View {
    @Environment(\.dismiss) private var dismiss

    private let tracks: [(role: String, title: String, source: String)] = [
        ("Menu",       "The Price of Freedom",   "Royalty-Free Music"),
        ("Placement",  "Beyond New Horizons",     "Royalty-Free Music"),
        ("Battle",     "Honor and Sword",         "No-Copyright Music"),
        ("Victory",    "Victory",                 "Free Sound Effect"),
        ("Defeat",     "Waves Crash",             "Free Sound Effect"),
    ]

    var body: some View {
        ZStack {
            Color(red: 0.05, green: 0.08, blue: 0.18).ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    // Title
                    VStack(spacing: 6) {
                        Text("⚓").font(.system(size: 48))
                        Text("Battleships")
                            .font(.system(size: 28, weight: .black))
                            .foregroundStyle(
                                LinearGradient(colors: [.cyan, .blue], startPoint: .leading, endPoint: .trailing)
                            )
                        Text("Created by Adrians Bergmanis")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    .padding(.top, 16)

                    Divider().background(Color.white.opacity(0.1))

                    // Music credits
                    VStack(alignment: .leading, spacing: 16) {
                        HStack(spacing: 6) {
                            Text("🎵").font(.title3)
                            Text("Music & Sound Credits")
                                .font(.headline)
                                .foregroundColor(.white)
                        }

                        Text("All tracks are royalty-free / no-copyright and are used in compliance with their respective free-use terms.")
                            .font(.caption)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.leading)

                        VStack(spacing: 10) {
                            ForEach(tracks, id: \.title) { track in
                                HStack(alignment: .top, spacing: 10) {
                                    Text("♪")
                                        .font(.caption)
                                        .foregroundColor(.blue.opacity(0.8))
                                        .frame(width: 14)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("\"\(track.title)\"")
                                            .font(.caption.bold())
                                            .foregroundColor(.white)
                                        Text("\(track.role)  •  \(track.source)")
                                            .font(.system(size: 10))
                                            .foregroundColor(.gray)
                                    }
                                    Spacer()
                                }
                            }
                        }
                        .padding(14)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.04))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.08), lineWidth: 1))
                        )
                    }

                    Divider().background(Color.white.opacity(0.1))

                    Text("© Adrians Bergmanis. All rights reserved.")
                        .font(.system(size: 10))
                        .foregroundColor(.gray.opacity(0.5))
                        .padding(.bottom, 24)
                }
                .padding(.horizontal, 24)
            }
        }
        .presentationDetents([.medium, .large])
        .preferredColorScheme(.dark)
    }
}

// MARK: - Marquee text (scrolling single-line label)

struct MarqueeText: View {
    let text: String
    let font: Font
    let color: Color

    @State private var animate = false
    @State private var textWidth: CGFloat = 0
    @State private var containerWidth: CGFloat = 0

    init(_ text: String, font: Font = .caption, color: Color = .primary) {
        self.text = text
        self.font = font
        self.color = color
    }

    private var shouldScroll: Bool { textWidth > containerWidth }

    var body: some View {
        GeometryReader { geo in
            let cw = geo.size.width
            ZStack(alignment: .leading) {
                Text(text)
                    .font(font)
                    .foregroundColor(color)
                    .lineLimit(1)
                    .fixedSize()
                    .background(GeometryReader { inner in
                        Color.clear.onAppear {
                            textWidth = inner.size.width
                            containerWidth = cw
                            if textWidth > cw { startAnimation() }
                        }
                    })
                    .offset(x: animate && shouldScroll ? -(textWidth - cw + 8) : 0)
                    .animation(
                        animate
                            ? .linear(duration: Double(textWidth) / 30).repeatForever(autoreverses: true)
                            : .default,
                        value: animate
                    )
            }
            .clipped()
        }
        .frame(height: font == .caption ? 14 : 12)
    }

    private func startAnimation() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            animate = true
        }
    }
}
