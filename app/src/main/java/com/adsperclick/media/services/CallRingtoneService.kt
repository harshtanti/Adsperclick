package com.adsperclick.media.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.adsperclick.media.R
import com.adsperclick.media.utils.Constants.FCM.ID_OF_GROUP_TO_OPEN
import com.adsperclick.media.views.splashActivity.SplashActivity

/*

class CallRingtoneService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val INCOMING_CALL_NOTIFICATION_ID = 1001
        private const val CALL_CHANNEL_ID = "incoming_call_channel"

        // Method to start the service safely
        fun startCallService(context: Context, title: String, body: String, groupId: String) {
            try {
                val intent = Intent(context, CallRingtoneService::class.java).apply {
                    putExtra("TITLE", title)
                    putExtra("BODY", body)
                    putExtra("GROUP_ID", groupId)
                }

                // Explicitly use startForegroundService
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("CallRingtoneService", "Failed to start service", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        try {
            // Handle hang-up action
            if (intent.action == "HANG_UP") {
                stopRingtone()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            // Extract call details
            val title = intent.getStringExtra("TITLE") ?: "Incoming Call"
            val body = intent.getStringExtra("BODY") ?: "Incoming Call"
            val groupId = intent.getStringExtra("GROUP_ID") ?: ""

            // Create and show notification
            val notification = createIncomingCallNotification(title, body, groupId)
            startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)

            // Start ringing
            startRingtone()
            */
/*startVibration()*//*


            return START_STICKY
        } catch (e: Exception) {
            Log.e("CallRingtoneService", "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    // ... (rest of the methods remain similar to previous implementation)

    private fun startVibration() {
        try {
            val vibratorManager = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Check vibrator availability
            if (!vibratorManager.hasVibrator()) {
                Log.w("CallRingtoneService", "Device does not have a vibrator")
                return
            }

            // Vibration pattern
            val pattern = longArrayOf(0, 400, 200, 400)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                vibratorManager.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibratorManager.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("CallRingtoneService", "Error starting vibration", e)
        }
    }

    private fun startRingtone() {
//        mediaPlayer = MediaPlayer.create(this, R.raw.the_call_yt_library).apply {
//            isLooping = true
//            start()
//        }
    }

    private fun stopRingtone() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                    release()
                }
            }
            mediaPlayer = null

            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("CallRingtoneService", "Error stopping ringtone", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createIncomingCallNotification(title: String, body: String, groupId: String): Notification {
        val intent = Intent(this, SplashActivity::class.java).apply {
            stopRingtone()
            putExtra(ID_OF_GROUP_TO_OPEN, groupId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a channel for call notifications
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)

        // Add a "Hang Up" action
        // Add a "Hang Up" action
        val hangUpIntent = Intent(this, CallRingtoneService::class.java).apply {
            action = "HANG_UP"
            component = ComponentName(this@CallRingtoneService, CallRingtoneService::class.java) // ✅ Explicit component
        }

        val hangUpPendingIntent = PendingIntent.getForegroundService(
            this,
            0,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ecomm_chat_app_icon)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_call_end, "Hang Up", hangUpPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }


}



*/






/*
// New Service for Call Ringtone
class CallRingtoneService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        try {
            // Check if this is a hang-up action
            if (intent.action == "HANG_UP") {
                stopRingtone()
                stopForeground(STOP_FOREGROUND_REMOVE) // ✅ Updated for Android 14+ compatibility
                if(AdsperclickApplication.appLifecycleObserver.isAppInForeground.not()){
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            // Regular incoming call handling
            val title = intent.getStringExtra("TITLE") ?: "Incoming Call"
            val body = intent.getStringExtra("BODY") ?: "Incoming Call"
            val groupId = intent.getStringExtra("GROUP_ID") ?: ""

            // Create incoming call notification
            val notification = createIncomingCallNotification(title, body, groupId)
            startForeground(INCOMING_CALL_NOTIFICATION_ID, notification)

            // Start ringing and vibrating
            startRingtone()
//            startVibration()

            return START_STICKY
        } catch (e: Exception) {
            Log.e("CallRingtoneService", "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createIncomingCallNotification(title: String, body: String, groupId: String): Notification {
        val intent = Intent(this, SplashActivity::class.java).apply {
            stopRingtone()
            putExtra(ID_OF_GROUP_TO_OPEN, groupId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a channel for call notifications
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)

        // Add a "Hang Up" action
        // Add a "Hang Up" action
        val hangUpIntent = Intent(this, CallRingtoneService::class.java).apply {
            action = "HANG_UP"
            component = ComponentName(this@CallRingtoneService, CallRingtoneService::class.java) // ✅ Explicit component
        }

        val hangUpPendingIntent = PendingIntent.getForegroundService(
            this,
            0,
            hangUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ecomm_chat_app_icon)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_call_end, "Hang Up", hangUpPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun startRingtone() {
        mediaPlayer = MediaPlayer.create(this, R.raw.the_call_yt_library).apply {
            isLooping = true
            start()
        }
    }

    private fun startVibration() {
        try {
            // Check if vibration permission is granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.VIBRATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("CallRingtoneService", "Vibration permission not granted")
                return
            }

            val vibratorManager = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Check if vibrator is available
            if (!vibratorManager.hasVibrator()) {
                Log.w("CallRingtoneService", "Device does not have a vibrator")
                return
            }

            // Vibration pattern: 400ms vibrate, 200ms pause, repeat
            val pattern = longArrayOf(0, 400, 200, 400)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                vibratorManager.vibrate(vibrationEffect)
            } else {
                // For older Android versions
                vibratorManager.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("CallRingtoneService", "Error starting vibration", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopRingtone() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
*/




