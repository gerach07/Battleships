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

        if (phase != "login") {
            Spacer(Modifier.width(8.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = viewModel::toggleChat,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .size(36.dp)
                ) {
                    Text("💬", fontSize = 16.sp)
                }
                if (chatUnread > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(16.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chatUnread.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MusicBanner() {
    val currentTrackName by MusicManager.currentTrackName.collectAsState()
    var showCredits by remember { mutableStateOf(false) }

    if (currentTrackName != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFF2563EB).copy(alpha = 0.18f), CircleShape)
            ) {
                Text("🎵", fontSize = 10.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = currentTrackName!!,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { showCredits = true },
                modifier = Modifier.size(24.dp)
            ) {
                Text("ℹ️", fontSize = 14.sp)
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
        "success" -> c.green.copy(alpha = .15f)
        "error" -> c.red.copy(alpha = .15f)
        else -> c.primary.copy(alpha = .15f)
    }
    val border = when (type) {
        "success" -> c.green.copy(alpha = .4f)
        "error" -> c.red.copy(alpha = .4f)
        else -> c.primary.copy(alpha = .4f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(message, fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
