package com.anasio.battleships.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.i18n.LocalLanguage
import com.anasio.battleships.i18n.Strings
import com.anasio.battleships.ui.components.BackgroundShips
import com.anasio.battleships.ui.components.ChatOverlay
import com.anasio.battleships.ui.components.ConnectionOverlay
import com.anasio.battleships.ui.components.GlobalHeader
import com.anasio.battleships.ui.components.MusicBanner
import com.anasio.battleships.ui.components.MessageBanner
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

import com.anasio.battleships.ui.screens.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.ui.theme.BattleshipTheme
import com.anasio.battleships.ui.theme.paletteFor
import com.anasio.battleships.viewmodel.GameViewModel

@Composable
fun BattleshipApp(viewModel: GameViewModel) {
    val phase by viewModel.phase.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val chatOpen by viewModel.chatOpen.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatUnread by viewModel.chatUnread.collectAsState()
    val language by viewModel.language.collectAsState()
    val themeId by viewModel.themeId.collectAsState()
    val palette = paletteFor(themeId)

    BackHandler(enabled = phase != "login") {
        when (phase) {
            "waiting", "placement" -> viewModel.handleBackToMenu()
            "gameOver" -> viewModel.handleBackToMenu()
            // battle → do nothing (use Surrender button)
        }
    }

    CompositionLocalProvider(
        LocalI18n provides Strings.forLanguage(language),
        LocalLanguage provides language,
        LocalColorPalette provides palette,
    ) {
    BattleshipTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        // Background sailing ships (hidden during placement & battle)
        if (phase != "battle" && phase != "placement") {
            BackgroundShips()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            GlobalHeader(viewModel)

            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        Crossfade(targetState = phase, label = "phaseTransition") { targetPhase ->
                            when (targetPhase) {
                                "login" -> LoginScreen(viewModel)
                                "waiting" -> WaitingRoomScreen(viewModel)
                                "placement" -> PlacementScreen(viewModel)
                                "battle" -> BattleScreen(viewModel)
                                "gameOver" -> GameOverScreen(viewModel)
                            }
                        }
                    }
                }
                
                // Music Banner fixed at top of content area (like iOS ZStack alignment: .top)
                Box(Modifier.align(Alignment.TopCenter)) {
                    MusicBanner()
                }
            }
        }

        // Message Toast
        val message by viewModel.message.collectAsState()
        val messageType by viewModel.messageType.collectAsState()
        if (message.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                MessageBanner(message, messageType)
            }
        }

        // Chat available outside login (spectators can chat too)
        if (phase != "login") {
            ChatOverlay(
                messages = chatMessages,
                isOpen = chatOpen,
                onToggle = viewModel::toggleChat,
                onSend = viewModel::sendChat,
                unread = chatUnread,
            )
        }

        // Disconnected overlay
        if (!isConnected) {
            ConnectionOverlay(onReconnect = viewModel::forceReconnect)
        }
    }
    } // BattleshipTheme
    } // CompositionLocalProvider
}
