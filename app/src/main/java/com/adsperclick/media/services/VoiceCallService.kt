package com.adsperclick.media.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.adsperclick.media.views.call.fragment.VoiceCallFragment

class VoiceCallService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_call_channel"
        private const val CHANNEL_NAME = "Voice Call"
        const val ACTION_START_CALL = "START_CALL"
        const val ACTION_END_CALL = "END_CALL"
        const val ACTION_END_CALL_FROM_NOTIFICATION = "END_CALL_FROM_NOTIFICATION"
        const val EXTRA_CHANNEL_NAME = "channelName"
    }

    private val binder = LocalBinder()
    private var isCallActive = false

    // Binder class for client communication
    inner class LocalBinder : Binder() {
        fun getService(): VoiceCallService = this@VoiceCallService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("CallService", "VoiceCallService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CallService", "onStartCommand received with action: ${intent?.action}")

        intent?.let {
            when (it.action) {
                ACTION_START_CALL -> {
                    val channelName = it.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Unknown"
                    Log.d("CallService", "Starting call foreground with channel: $channelName")

                    try {
                        startCallForeground(channelName)
                        isCallActive = true
                        Log.d("CallService", "Successfully started foreground service")
                    } catch (e: Exception) {
                        Log.e("CallService", "Error starting foreground: ${e.message}", e)
                    }
                }
                ACTION_END_CALL -> {
                    val broadcastIntent = Intent(ACTION_END_CALL_FROM_NOTIFICATION).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Add flags to help broadcast reach its target on Android 12+
                            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        }
                    }

                    Log.d("CallService", "Broadcasting END_CALL_FROM_NOTIFICATION with flags")

                    // Use explicit sendBroadcast call
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // For Android 13+, use a more explicit approach
                        broadcastIntent.setPackage(packageName)
                    }
                    sendBroadcast(broadcastIntent)

                    stopSelf()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    isCallActive = false
                }

                else -> {}
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for voice call notifications"
                    setSound(null, null) // No sound as it would interfere with call
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }

                notificationManager.createNotificationChannel(channel)
                Log.d("CallService", "Created notification channel: $CHANNEL_ID")
            }
        }
    }

    private fun startCallForeground(channelName: String) {
        // Create intent for notification tap action (return to app)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, VoiceCallFragment::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "OPEN_FROM_NOTIFICATION"
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create end call action intent
        val endCallIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceCallService::class.java).apply {
                action = ACTION_END_CALL
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use default icons
        val callIcon = android.R.drawable.ic_menu_call
        val endCallIcon = android.R.drawable.ic_menu_close_clear_cancel

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Call in Progress")
            .setContentText("Connected to channel: $channelName")
            .setSmallIcon(callIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(endCallIcon, "End Call", endCallIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .build()

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isCallActive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isCallActive = false
        }
    }

    fun updateNotification(update: String) {
        if (!isCallActive) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, VoiceCallFragment::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "OPEN_FROM_NOTIFICATION"
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endCallIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceCallService::class.java).apply {
                action = ACTION_END_CALL
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Default icons
        val callIcon = android.R.drawable.ic_menu_call
        val endCallIcon = android.R.drawable.ic_menu_close_clear_cancel

        // Build updated notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Call in Progress")
            .setContentText(update)
            .setSmallIcon(callIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(endCallIcon, "End Call", endCallIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
