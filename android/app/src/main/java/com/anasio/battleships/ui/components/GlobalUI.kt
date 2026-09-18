package com.anasio.battleships.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.i18n.Language
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.util.MusicManager
import com.anasio.battleships.viewmodel.GameViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bounceScale")
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(requireUnconsumed = false)
                    true
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

@Composable
fun GlobalHeader(viewModel: GameViewModel) {
    val phase by viewModel.phase.collectAsState()
    val language by viewModel.language.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val chatOpen by viewModel.chatOpen.collectAsState()
    val chatUnread by viewModel.chatUnread.collectAsState()
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language Picker
        Box {
            Text(
                when (language) {
                    Language.EN -> "🇬🇧 ${language.name}"
                    Language.LV -> "🇱🇻 ${language.name}"
                    Language.RU -> "🇷🇺 ${language.name}"
                },
                fontSize = 18.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable { expandedMenu = true }
                    .padding(6.dp)
            )
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false },
                modifier = Modifier.background(c.surface)
            ) {
                Language.values().forEach { lang ->
                    val txt = when (lang) { Language.EN -> "🇬🇧 EN"; Language.LV -> "🇱🇻 LV"; Language.RU -> "🇷🇺 RU" }
                    DropdownMenuItem(
                        text = { Text(txt, color = c.textPrimary) },
                        onClick = { viewModel.setLanguage(lang); expandedMenu = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Title
        if (phase != "login") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚓", fontSize = 20.sp)
                Spacer(Modifier.width(4.dp))
                Text(s.battleships, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sound
        IconButton(
            onClick = viewModel::toggleSound,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .size(36.dp)
        ) {
            Text(if (soundEnabled) "🔊" else "🔇", fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))

        // Music
        IconButton(
            onClick = viewModel::toggleMusic,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .size(36.dp)
        ) {
            Text(if (musicEnabled) "🎵" else "🔕", fontSize = 16.sp)
        }
    }
}

@Composable
fun MusicBanner(viewModel: GameViewModel) {
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val currentTrackName by MusicManager.currentTrackName.collectAsState()
    val c = LocalColorPalette.current
    var showCredits by remember { mutableStateOf(false) }

    if (musicEnabled && !currentTrackName.isNullOrBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .background(c.primary.copy(alpha = 0.2f), CircleShape)
            ) {
                Text("🎵", fontSize = 10.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = currentTrackName!!,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { showCredits = true },
                modifier = Modifier.size(22.dp)
            ) {
                Text("ℹ️", fontSize = 12.sp)
            }
        }

        if (showCredits) {
            CreditsDialog(onDismiss = { showCredits = false })
        }
    }
}

@Composable
fun MessageBanner(message: String, type: String) {
    val c = LocalColorPalette.current
    val bg = when (type) {
        "success" -> c.green.copy(alpha = .9f)
        "error" -> c.red.copy(alpha = .9f)
        else -> c.primary.copy(alpha = .9f)
    }
    val border = when (type) {
        "success" -> c.green
        "error" -> c.red
        else -> c.primary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(message, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreditsDialog(onDismiss: () -> Unit) {
    val c = LocalColorPalette.current
    val tracks = listOf(
        Triple("The Price of Freedom",  "Menu Music",           "Royalty-Free Music"),
        Triple("Beyond New Horizons",   "Ship Placement Music", "Royalty-Free Music"),
        Triple("Honor and Sword",       "Battle Music",         "No-Copyright Music"),
        Triple("Victory",               "Victory Sound",        "Free Sound Effect"),
        Triple("Waves Crash",           "Defeat Sound",         "Free Sound Effect"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("⚓ Battleships", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Created by Adrians Bergmanis", fontSize = 11.sp, color = c.textDim.copy(alpha = 0.7f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎵 Music & Sound Credits", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.primary)
                tracks.forEach { (name, role, source) ->
                    Column {
                        Text("♪ \"$name\"", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("$role  •  $source", fontSize = 10.sp, color = c.textDim)
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = c.border.copy(alpha = 0.3f))
                Text(
                    "© Adrians Bergmanis. All rights reserved.",
                    fontSize = 10.sp,
                    color = c.textDim.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = c.primary)
            }
        }
    )
}
