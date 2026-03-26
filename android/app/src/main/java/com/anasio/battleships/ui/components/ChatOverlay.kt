package com.anasio.battleships.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.data.ChatMessage
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette

@Composable
fun ChatOverlay(
    messages: List<ChatMessage>,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onSend: (String) -> Unit,
    unread: Int,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val s = LocalI18n.current
        val c = LocalColorPalette.current
        if (isOpen) {
            // Chat panel
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .widthIn(min = 260.dp, max = 320.dp)
                    .fillMaxWidth(0.85f)
                    .heightIn(min = 280.dp, max = 380.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = c.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.card)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("💬 ${s.chat}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        TextButton(
                            onClick = onToggle,
                            modifier = Modifier.semantics { contentDescription = s.closeChat },
                        ) {
                            Text("✕", color = c.textDim, fontSize = 16.sp)
                        }
                    }

                    // Messages
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatBubble(msg)
                        }
                        if (messages.isEmpty()) {
                            item {
                                Text(
                                    s.noMessagesYet,
                                    color = c.textDim, fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                )
                            }
                        }
                    }

                    // Input
                    var text by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { if (it.length <= 200) text = it },
                            placeholder = { Text(s.messagePlaceholder, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (text.isNotBlank()) { onSend(text); text = "" }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = c.primary,
                                unfocusedBorderColor = c.border,
                            ),
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { if (text.isNotBlank()) { onSend(text); text = "" } },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, s.send, tint = c.primary)
                        }
                    }
                }
            }
        } else {
            // FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                FloatingActionButton(
                    onClick = onToggle,
                    containerColor = c.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp).semantics { contentDescription = s.openChat },
                ) {
                    Text("💬", fontSize = 22.sp)
                }
                if (unread > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                        containerColor = c.red,
                    ) {
                        Text(if (unread > 9) "9+" else "$unread", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val c = LocalColorPalette.current
    val align = if (msg.isMine) Alignment.End else Alignment.Start
    val impTeal = Color(0xFF00BCD4)
    val bg = when {
        msg.isImportant -> impTeal.copy(alpha = .18f)
        msg.isMine -> c.primary.copy(alpha = .15f)
        else -> c.card.copy(alpha = .6f)
    }
    val borderColor = when {
        msg.isImportant -> impTeal.copy(alpha = .5f)
        msg.isMine -> c.primary.copy(alpha = .3f)
        else -> c.border.copy(alpha = .3f)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            if (!msg.isMine) {
                val namePrefix = if (msg.isImportant) "📢 " else ""
                Text(
                    namePrefix + msg.senderName, fontSize = 10.sp,
                    color = if (msg.isImportant) impTeal else c.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(msg.text, fontSize = 13.sp, color = c.textPrimary)
        }
    }
}
