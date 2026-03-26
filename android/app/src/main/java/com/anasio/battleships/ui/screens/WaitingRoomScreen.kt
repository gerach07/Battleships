package com.anasio.battleships.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette
import com.anasio.battleships.viewmodel.GameViewModel

@Composable
fun WaitingRoomScreen(viewModel: GameViewModel) {
    val gameId by viewModel.gameId.collectAsState()
    val roomPassword by viewModel.roomPassword.collectAsState()
    val gameTimeLimit by viewModel.gameTimeLimit.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val showKickDialog by viewModel.showKickDialog.collectAsState()
    val context = LocalContext.current
    val s = LocalI18n.current
    val c = LocalColorPalette.current

    val hasOpponent = opponentName.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Room code card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = c.surface),
            border = BorderStroke(1.dp, c.yellow.copy(alpha = .4f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.roomCodeLabel, fontSize = 12.sp, color = c.textDim, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    gameId, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                    color = c.yellow, letterSpacing = 4.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (roomPassword.isNotBlank()) {
                        Text(
                            "🔒 ${s.pinLabel}: $roomPassword",
                            fontSize = 12.sp, color = c.orange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.orange.copy(alpha = .1f))
                                .border(1.dp, c.orange.copy(alpha = .3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        "⏱️ ${gameTimeLimit / 60} ${s.min}",
                        fontSize = 12.sp, color = c.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.primary.copy(alpha = .1f))
                            .border(1.dp, c.primary.copy(alpha = .3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Player slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // You slot
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = c.green.copy(alpha = .08f)),
                border = BorderStroke(1.dp, c.green.copy(alpha = .3f)),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("✓", fontSize = 22.sp, color = c.green)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.youSlot, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = c.green,
                        )
                        Text(
                            s.readyStatus, fontSize = 10.sp,
                            color = c.green.copy(alpha = .7f),
                            letterSpacing = 2.sp,
                        )
                    }
                    if (isHost) {
                        Text(
                            s.hostBadge,
                            fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A2E),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(c.yellow, c.orange)
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            // Opponent slot
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasOpponent) c.green.copy(alpha = .08f) else c.primary.copy(alpha = .05f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (hasOpponent) c.green.copy(alpha = .3f) else c.primary.copy(alpha = .2f)
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (hasOpponent) {
                        Text("✓", fontSize = 22.sp, color = c.green)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            opponentName, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = c.green,
                            maxLines = 1,
                        )
                        Text(
                            s.readyStatus, fontSize = 10.sp,
                            color = c.green.copy(alpha = .7f),
                            letterSpacing = 2.sp,
                        )
                    } else {
                        BounceDots()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.waitingFor, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = c.textDim,
                        )
                        Text(
                            s.notJoined, fontSize = 10.sp,
                            color = c.textDim.copy(alpha = .6f),
                            letterSpacing = 2.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Message
        if (message.isNotBlank()) {
            MessageBanner(message, messageType)
            Spacer(Modifier.height(12.dp))
        }

        // Host controls — show when host and opponent has joined
        AnimatedVisibility(visible = isHost && hasOpponent) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = c.yellow.copy(alpha = .06f)),
                border = BorderStroke(1.dp, c.yellow.copy(alpha = .25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            s.hostControls, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = c.yellow,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var startLoading by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (!startLoading) {
                                    startLoading = true
                                    viewModel.handleStartGame()
                                }
                            },
                            enabled = !startLoading,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.green),
                        ) {
                            if (startLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(s.startGame, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = viewModel::requestKickPlayer,
                            modifier = Modifier.height(48.dp).semantics { contentDescription = s.kickPlayer },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.red),
                        ) {
                            Text("⛔", fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.hostHint, fontSize = 10.sp,
                        color = c.textDim, textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Non-host waiting for host to start
        AnimatedVisibility(visible = !isHost && hasOpponent) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = c.primary.copy(alpha = .06f)),
                border = BorderStroke(1.dp, c.primary.copy(alpha = .25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("⏳ ${s.waitingForHost}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(s.waitingForHostHint, fontSize = 10.sp, color = c.textDim, textAlign = TextAlign.Center)
                }
            }
        }

        // Share buttons — show when no opponent yet
        if (!hasOpponent) {
            Spacer(Modifier.height(16.dp))
            Text(s.waitingForOpponent, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("Room Code", gameId))
                    Toast.makeText(context, s.roomCodeCopied, Toast.LENGTH_SHORT).show()
                }) { Text("📋 ${s.copyCode}") }

                OutlinedButton(onClick = {
                    val txt = "${s.joinMyGame}\n${s.roomCodeLabel}: $gameId" +
                            if (roomPassword.isNotBlank()) "\n${s.pinLabel}: $roomPassword" else ""
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, txt); type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, s.shareRoom))
                }) { Text("🔗 ${s.share}") }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = viewModel::handleBackToMenu,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.red),
        ) { Text("← ${s.leaveRoom}") }

        Spacer(Modifier.height(16.dp))
        Text(
            "⚓ Battleships by Adrians Bergmanis",
            fontSize = 9.sp,
            color = c.textDim.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }

    // Kick confirmation dialog
    if (showKickDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelKick,
            title = { Text(s.kickPlayer) },
            text = { Text(s.confirmKick) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmKick) { Text(s.yes) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelKick) { Text(s.no) }
            },
        )
    }
}

@Composable
private fun BounceDots() {
    val c = LocalColorPalette.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(400); tick++ } }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val active = tick % 3 == i
            val animatedSize by animateDpAsState(
                targetValue = if (active) 12.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                label = "dotSize"
            )
            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) c.primary else c.card)
                    .border(1.dp, if (active) c.primary else c.border, RoundedCornerShape(50)),
            )
        }
    }
}
