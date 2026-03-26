import Foundation
import SocketIO

final class GameSocketManager: ObservableObject {
    static let shared = GameSocketManager()

    private var manager: SocketManager?
    private var socket: SocketIOClient?

    @Published var isConnected = false

    var socketId: String? { socket?.sid }

    private init() {}

    func connect() {
        guard socket?.status != .connected else { return }

        // Clean up if already existing
        socket?.removeAllHandlers()
        socket?.disconnect()
        manager?.disconnect()

        guard let url = URL(string: SERVER_URL) else { return }

        manager = SocketManager(socketURL: url, config: [
            .log(false),
            .reconnects(true),
            .reconnectWait(1),
            .reconnectWaitMax(5),
            .forceNew(true),
        ])

        socket = manager?.defaultSocket

        socket?.on(clientEvent: .connect) { [weak self] _, _ in
            DispatchQueue.main.async { self?.isConnected = true }
        }
        socket?.on(clientEvent: .disconnect) { [weak self] _, _ in
            DispatchQueue.main.async { self?.isConnected = false }
        }

        socket?.connect()
    }

    func forceReconnect() {
        if socket?.status != .connected {
            if socket != nil {
                socket?.connect()
            } else {
                connect()
            }
        }
    }

    func on(_ event: String, callback: @escaping ([Any]) -> Void) {
        socket?.on(event) { data, _ in
            callback(data)
        }
    }

    func off(_ event: String) {
        socket?.off(event)
    }

    func emit(_ event: String) {
        socket?.emit(event)
    }

    func emit(_ event: String, _ data: [String: Any]) {
        socket?.emit(event, data)
    }

    func disconnect() {
        socket?.removeAllHandlers()
        socket?.disconnect()
        manager?.disconnect()
        socket = nil
        manager = nil
        DispatchQueue.main.async { self.isConnected = false }
    }
}
