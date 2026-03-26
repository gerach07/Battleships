package com.anasio.battleships

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.anasio.battleships.ui.BattleshipApp
import com.anasio.battleships.viewmodel.GameViewModel
import com.anasio.battleships.services.BackgroundTimerService
import com.anasio.battleships.util.MusicManager
import com.anasio.battleships.data.SocketManager
import android.app.NotificationManager

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            BattleshipApp(viewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()

        val phase = viewModel.phase.value
        val isSpectator = viewModel.isSpectator.value
        // Only start background timer if in active game phases and not a spectator
        if ((phase == "placement" || phase == "battle") && !isSpectator) {
            val intent = Intent(this, BackgroundTimerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
        SocketManager.forceReconnect()

        // Stop the background timer service and clear notification
        try {
            val intent = Intent(this, BackgroundTimerService::class.java)
            stopService(intent)
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(1) // Clear the forfeit notification
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure background timer service is stopped to prevent unexpected forfeit
        try {
            val intent = Intent(this, BackgroundTimerService::class.java)
            stopService(intent)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(1)
        } catch (_: Exception) {}
    }
}
