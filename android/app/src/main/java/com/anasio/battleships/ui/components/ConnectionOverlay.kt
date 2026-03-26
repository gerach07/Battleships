package com.anasio.battleships.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasio.battleships.i18n.LocalI18n
import com.anasio.battleships.ui.theme.*
import com.anasio.battleships.ui.theme.LocalColorPalette

@Composable
fun ConnectionOverlay(onReconnect: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .75f))
            .clickable { onReconnect() },
        contentAlignment = Alignment.Center,
    ) {
        val s = LocalI18n.current
        val c = LocalColorPalette.current
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = c.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("⚡ ${s.connectionLost}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(s.reconnecting, fontSize = 13.sp, color = c.textDim)
            Spacer(Modifier.height(12.dp))
            Text("(${s.tapToRetry})", fontSize = 11.sp, color = c.textDim.copy(alpha=0.6f))
        }
    }
}
