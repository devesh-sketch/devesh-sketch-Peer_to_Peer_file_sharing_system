package com.p2p.fileshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.p2p.fileshare.MainActivity

class TransferForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "p2p_transfer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_FILE_NAME = "EXTRA_FILE_NAME"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_IS_SENDING = "EXTRA_IS_SENDING"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "File Transfer"
                val isSending = intent.getBooleanExtra(EXTRA_IS_SENDING, true)
                startForeground(NOTIFICATION_ID, buildNotification(fileName, 0, "Starting...", isSending))
            }
            ACTION_UPDATE -> {
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "File Transfer"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val speed = intent.getStringExtra(EXTRA_SPEED) ?: ""
                val isSending = intent.getBooleanExtra(EXTRA_IS_SENDING, true)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(fileName, progress, speed, isSending))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(fileName: String, progress: Int, speed: String, isSending: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionText = if (isSending) "Sending" else "Receiving"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$actionText: $fileName")
            .setContentText(if (speed.isNotBlank()) "$progress% • $speed" else "$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P File Transfer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress and speed during P2P file transfers"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlashShare::TransferWakeLock")?.apply {
                acquire(3 * 60 * 60 * 1000L) // 3 hours max timeout for large movie transfers
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "FlashShare::WifiLock")
            } else {
                wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FlashShare::WifiLock")
            }?.apply {
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
