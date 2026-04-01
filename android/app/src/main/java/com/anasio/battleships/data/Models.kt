package com.anasio.battleships.data

import androidx.compose.runtime.Stable

typealias Board = List<List<String>>

@Stable
data class RoomInfo(
    val roomId: String,
    val hostName: String,
    val hasPassword: Boolean,
    val state: String,
    val playerCount: Int,
    val spectatorCount: Int,
    val timeLimit: Int,
)

@Stable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean = false,
    val isImportant: Boolean = false,
    val isSystem: Boolean = false,
)

@Stable
data class PlacedShip(
    val shipId: Int,
    val row: Int,
    val col: Int,
    val length: Int,
    val direction: String,
    val cells: List<Pair<Int, Int>>,
)

data class PendingJoin(
    val roomId: String,
    val password: String? = null,
    val isCreating: Boolean = false,
    val isSpectating: Boolean = false,
    val timeLimit: Int = 300,
)

data class SpectatorBoard(
    val playerId: String,
    val playerName: String,
    val board: Board,
)
