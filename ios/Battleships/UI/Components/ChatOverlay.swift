import SwiftUI

struct ChatOverlay: View {
    @ObservedObject var vm: GameViewModel
    @State private var messageText = ""
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text(vm.s.chat)
                    .font(.headline)
                    .foregroundColor(.white)
                Spacer()
                if vm.chatUnread > 0 {
                    Text("\(vm.chatUnread)")
                        .font(.caption2).fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(Color.red))
                }
                Button(action: { vm.chatOpen = false }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.white.opacity(0.6))
                        .font(.title3)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            Divider().background(Color.white.opacity(0.2))

            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(Array(vm.chatMessages.enumerated()), id: \.element.id) { index, msg in
                            let showName = (index == 0) || (vm.chatMessages[index - 1].senderName != msg.senderName) || vm.chatMessages[index - 1].isSystem || msg.isSystem
                            chatBubble(msg, showName: showName)
                                .id(msg.id)
                        }
                    }
                    .padding(12)
                }
                .onChange(of: vm.chatMessages.count) { _ in
                    if let last = vm.chatMessages.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            Divider().background(Color.white.opacity(0.2))

            // Input
            HStack(spacing: 8) {
                TextField(vm.s.messagePlaceholder, text: $messageText)
                    .textFieldStyle(.plain)
                    .padding(8)
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(8)
                    .foregroundColor(.white)
                    .focused($isFocused)
                    .onSubmit { send() }

                Button(action: send) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(messageText.isEmpty ? .gray : .blue)
                }
                .disabled(messageText.isEmpty)
            }
            .padding(12)
        }
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(red: 0.08, green: 0.12, blue: 0.22).opacity(0.97))
        )
        .onAppear { vm.chatUnread = 0 }
    }

    private func send() {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        vm.sendChat(text)
        messageText = ""
    }

    @ViewBuilder
    private func chatBubble(_ msg: ChatMessage, showName: Bool) -> some View {
        let isMe = msg.isMine
        if msg.isSystem {
            HStack {
                Spacer()
                Text(msg.text)
                    .font(.caption)
                    .italic()
                    .foregroundColor(.white.opacity(0.5))
                    .padding(.vertical, 4)
                Spacer()
            }
        } else {
            HStack {
                if isMe { Spacer(minLength: 40) }
                VStack(alignment: isMe ? .trailing : .leading, spacing: 2) {
                    if showName {
                        Text(isMe ? vm.s.youSlot : msg.senderName)
                            .font(.caption2)
                            .foregroundColor((isMe ? Color.blue : Color.white).opacity(0.6))
                    }
                    Text(msg.text)
                        .font(.subheadline)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(isMe ? Color.blue.opacity(0.6) : Color.white.opacity(0.15))
                        )
                        .foregroundColor(.white)
                }
                if !isMe { Spacer(minLength: 40) }
            }
            .padding(.top, showName ? 4 : 0)
        }
    }
}
