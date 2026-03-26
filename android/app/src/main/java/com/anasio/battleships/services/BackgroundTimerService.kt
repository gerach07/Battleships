package com.anasio.battleships.services

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.anasio.battleships.MainActivity
import com.anasio.battleships.R
import com.anasio.battleships.data.SocketManager
import android.util.Log
import kotlinx.coroutines.*

class BackgroundTimerService : Service() {
    private var countdownJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val CHANNEL_ID = "game_background_channel"
        private const val NOTIFICATION_ID = 1
        private const val COUNTDOWN_SECONDS = 60
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundServiceWithNotification()
        startCountdown()
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = createNotification(
            getString(R.string.notif_game_paused),
            getString(R.string.notif_forfeit_countdown, COUNTDOWN_SECONDS)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try { startForeground(NOTIFICATION_ID, notification) } catch (_: Exception) {}
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .build()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        // Acquire a partial wake lock to ensure the countdown completes even if screen is off
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Battleships::ForfeitCountdown"
            )?.apply { acquire(90_000L) } // 90s timeout (60s countdown + buffer)
        } catch (e: Exception) {
            Log.w("BackgroundTimerService", "Failed to acquire wake lock: ${e.message}")
        }
        countdownJob = serviceScope.launch {
            for (i in COUNTDOWN_SECONDS downTo 1) {
                val notification = createNotification(
                    getString(R.string.notif_game_paused),
                    getString(R.string.notif_forfeit_countdown, i)
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, notification)
                delay(1000)
            }
            // Time's up - ensure we are connected before emitting if possible
            withContext(Dispatchers.IO) {
                SocketManager.forceReconnect()
                var connected = false
                for (attempt in 1..3) {
                    delay(1000)
                    if (SocketManager.isConnected.value) {
                        connected = true
                        break
                    }
                    SocketManager.forceReconnect()
                }
                if (connected) {
                    SocketManager.emit("forfeit")
                } else {
                    Log.w("BackgroundTimerService", "Failed to reconnect after 3 attempts; forfeit not sent")
                }
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
        serviceScope.cancel()
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wakeLock = null
    }
}
