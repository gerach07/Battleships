package com.anasio.battleships.data

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object SocketManager {
    private var socket: Socket? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    /** Track registered listeners per event for safe removal */
    private val registeredListeners = ConcurrentHashMap<String, Emitter.Listener>()

    val socketId: String? get() = socket?.id()

    fun connect() {
        if (socket?.connected() == true) return
        try {
            // Clean up any existing disconnected socket to prevent leaking threads/listeners
            socket?.let { old ->
                old.off()
                old.disconnect()
            }
            socket = null
            synchronized(this) {
                registeredListeners.clear()
            }

            val opts = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                reconnectionAttempts = Int.MAX_VALUE
                timeout = 60000
            }
            socket = IO.socket(SERVER_URL, opts)
            socket?.on(Socket.EVENT_CONNECT) { _isConnected.value = true }
            socket?.on(Socket.EVENT_DISCONNECT) { _isConnected.value = false }
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun forceReconnect() {
        if (socket?.connected() != true) {
            if (socket != null) {
                socket?.connect()
            } else {
                connect()
            }
        }
    }

    /** Register a listener and track it for safe per-event removal */
    fun on(event: String, listener: Emitter.Listener) {
        // Remove any previously registered listener for this event first
        registeredListeners[event]?.let { socket?.off(event, it) }
        registeredListeners[event] = listener
        socket?.on(event, listener)
    }

    /** Remove only the tracked listener for this event (not ALL listeners) */
    fun off(event: String) {
        registeredListeners[event]?.let { listener ->
            socket?.off(event, listener)
            registeredListeners.remove(event)
        }
    }

    /** Remove a specific listener for an event */
    fun off(event: String, listener: Emitter.Listener) {
        socket?.off(event, listener)
        if (registeredListeners[event] === listener) {
            registeredListeners.remove(event)
        }
    }

    fun emit(event: String) {
        socket?.emit(event)
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun disconnect() {
        registeredListeners.clear()
        socket?.disconnect()
        socket?.off()
        socket = null
        _isConnected.value = false
    }
}
