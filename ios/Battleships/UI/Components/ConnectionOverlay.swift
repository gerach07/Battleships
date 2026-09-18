import SwiftUI

struct ConnectionOverlay: View {
    let strings: I18nStrings
    var onRetry: (() -> Void)? = nil

    var body: some View {
        ZStack {
            Color.black.opacity(0.7)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Text("📡").font(.system(size: 40))
                Text(strings.connectionLost)
                    .font(.headline.bold())
                    .foregroundColor(.white)
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)
                Text(strings.reconnecting)
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.6))
                Text(strings.tapToRetry)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.4))
            }
            .padding(32)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(red: 0.1, green: 0.15, blue: 0.25).opacity(0.95))
            )
        }
        .onTapGesture { onRetry?() }
    }
}
