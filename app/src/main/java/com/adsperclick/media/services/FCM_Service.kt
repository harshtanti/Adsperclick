package com.adsperclick.media.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.AdsperclickApplication
import com.adsperclick.media.data.workManager.FCMTokenUpdateWorker
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.FCM.BASIC_NOTIFICATION
import com.adsperclick.media.utils.Constants.FCM.CHANNEL_ID
import com.adsperclick.media.utils.Constants.FCM.ID_OF_GROUP_TO_OPEN
import com.adsperclick.media.utils.Constants.FCM.ITS_A_BROADCAST_NOTIFICATION
import com.adsperclick.media.utils.Constants.MSG_TYPE.CALL
import com.adsperclick.media.views.login.repository.AuthRepository
import com.adsperclick.media.views.splashActivity.SplashActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCM_Service @Inject constructor(): FirebaseMessagingService() {

    @Inject
    lateinit var appContext: Context

    @Inject
    lateinit var authRepository: AuthRepository

    private var mediaPlayer: MediaPlayer? = null

//    private var ringtoneReceiver: BroadcastReceiver? = null




//    companion object {
//        private const val ACTION_STOP_RINGTONE = "com.adsperclick.media.ACTION_STOP_RINGTONE"
//        private const val ACTION_IGNORE_CALL = "com.adsperclick.media.ACTION_IGNORE_CALL"
//
//        @Volatile
//        private var currentCallGroupId: String? = null
//
//        fun stopCurrentCallRingtone(context: Context) {
//            val intent = Intent(ACTION_STOP_RINGTONE)
//            context.sendBroadcast(intent)
//        }
//    }




    // Keep track of messages per group
    private val messagesMap = mutableMapOf<String, MutableList<String>>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Enqueue WorkManager to update FCM token in the background
        val workRequest = OneTimeWorkRequestBuilder<FCMTokenUpdateWorker>()
            .setInputData(workDataOf("newToken" to token))
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }


    // So how we have implemented, if there's one notification in that group then we are displaing
    // using "BigTextStyle" of notification if there's more, we use inbox style, so in case of
    // broadcast notification... i.e. notification sent by Admin to others.... it can contain large text
    // so we always want it to be shown in "bigTextStyle" and all notifications should be separate notifications
    // so groupKey also we're generating unique using timestamp so that no two notifications are clubbed together
    // for broadcast notifications
    // for general notifications they're segregated based on groups...
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.data["title"] ?: "No Title"
        val body = message.data["body"] ?: "No Description"
        val msgType = message.data["msgType"]?.toInt() ?: Constants.MSG_TYPE.TEXT
        val groupId = message.data["groupId"] ?: ITS_A_BROADCAST_NOTIFICATION
        val downloadUrl = message.data["downloadUrl"] ?: ""


        // Below line prevents notification when user is using the app/ ie app is in foreground
        if (AdsperclickApplication.appLifecycleObserver.isAppInForeground
            && groupId != ITS_A_BROADCAST_NOTIFICATION
            && msgType != Constants.MSG_TYPE.CALL
        ) {
            return      // Don't show any notification if app is in use,
        }


        // Special handling for call notifications
        if (msgType == CALL) {
//            currentCallGroupId = groupId  // Store current call group ID

            playRingtone()
            /*startCallRingtone(title, body, groupId)*/
//            return  // Prevent standard notification processing
        }


        // Store message for this group
        if (!messagesMap.containsKey(groupId)) {
            messagesMap[groupId] = mutableListOf()
        }
        messagesMap[groupId]?.add(body)

        // Limit stored messages to prevent excessive memory usage
        if (groupId == ITS_A_BROADCAST_NOTIFICATION) {       // To handle case of broadcast notification, we only want one notification at a time
            messagesMap[groupId] =
                messagesMap[groupId]?.takeLast(1)?.toMutableList() ?: mutableListOf()
        } else if ((messagesMap[groupId]?.size
                ?: 0) > 5
        ) { // For normal notifications max-5 stored in memory, last five
            messagesMap[groupId] =
                messagesMap[groupId]?.takeLast(5)?.toMutableList() ?: mutableListOf()
        }

        sendNotification(BASIC_NOTIFICATION, title, body, groupId, msgType, downloadUrl)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(
        notificationType: Int,
        title: String,
        body: String,
        groupId: String,
        msgType: Int,
        imgUrl: String
    ) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Delivery Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)

        // Default small icon
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ecomm_chat_app_icon, null)
        val defaultBitmap = (drawable as? BitmapDrawable)?.bitmap

        val largeIcon = (ResourcesCompat.getDrawable(
            resources,
            R.drawable.ecomm_chat_app_icon,
            null
        ) as BitmapDrawable).bitmap


        // Group key for this specific chat group
        val groupKey = when (groupId) {
            ITS_A_BROADCAST_NOTIFICATION -> "${System.currentTimeMillis()}"     // Because for Broadcast notifications I want to show a summary notification, and each time a new notificaiton should be generated hence the unique key i.e. timestamp
            else -> "GROUP_CHAT_$groupId"
        }

        val intent = Intent(this, SplashActivity::class.java).apply {
            putExtra(ID_OF_GROUP_TO_OPEN, groupId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }


        val pendingIntent = PendingIntent.getActivity(
            this,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val groupMessages = messagesMap[groupId] ?: listOf()
        val messageCount = groupMessages.size

        // Build the notification
        val summaryNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ecomm_chat_app_icon)
            .setLargeIcon(largeIcon)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // **If Message Type is IMG_URL, Load the Image and Show Notification**
        if (msgType == Constants.MSG_TYPE.IMG_URL && imgUrl.isNotEmpty()) {
            Glide.with(appContext)
                .asBitmap()
                .load(imgUrl)
                .into(object : CustomTarget<Bitmap>() {     // Inside code runs asynchronously
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val imageNotification = summaryNotificationBuilder
                            .setContentTitle(title)
                            .setContentText(groupMessages.last())
                            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(resource))
                            .build()

                        nm.notify(groupKey.hashCode(), imageNotification)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle cleanup if needed
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Fallback to default text notification if image fails to load
                        summaryNotificationBuilder
                            .setContentTitle(title)
                            .setContentText(groupMessages.firstOrNull() ?: "Image message")
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .bigText(groupMessages.firstOrNull() ?: "Image message")
                            )

                        nm.notify(groupKey.hashCode(), summaryNotificationBuilder.build())
                    }
                })
        } /*else if (msgType == CALL) {
            // Call notification handling
            val joinIntent = Intent(this, SplashActivity::class.java).apply {
                putExtra(ID_OF_GROUP_TO_OPEN, groupId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val ignoreIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = ACTION_IGNORE_CALL
                putExtra("groupId", groupId)
            }

            val joinPendingIntent = PendingIntent.getActivity(
                this,
                groupId.hashCode() + 1,
                joinIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val ignorePendingIntent = PendingIntent.getBroadcast(
                this,
                groupId.hashCode() + 2,
                ignoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ecomm_chat_app_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_email, "Join", joinPendingIntent)
                .addAction(R.drawable.ic_email, "Ignore", ignorePendingIntent)

            val nmt = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nmt.notify(groupKey.hashCode(), callNotificationBuilder.build())
        }*/


        // **Else, Handle Normal Text Notifications**
        else if (messageCount == 1) {
            summaryNotificationBuilder
                .setContentTitle(title)
                .setContentText(groupMessages.first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(groupMessages.first()))
        } else {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText("$messageCount messages")

            groupMessages.forEach { message ->
                inboxStyle.addLine(message)
            }

            summaryNotificationBuilder
                .setContentTitle(title)
                .setContentText("$messageCount new messages")
                .setStyle(inboxStyle)
        }

        // **For Non-Image Notifications, Show Immediately** // for images it is asynchronous glide process it's handled above
        if (msgType != Constants.MSG_TYPE.IMG_URL /*&& msgType != CALL*/) {
            nm.notify(groupKey.hashCode(), summaryNotificationBuilder.build())
        }
    }


    // In your FCM service or call notification handler
//    fun startCallRingtone(title: String, body: String, groupId: String) {
//        playRingtone()
//    }

    // Expose method to stop ringtone
//    fun stopRingtone() {
//        mediaPlayer?.apply {
//            if (isPlaying) {
//                stop()
//                release()
//            }
//            mediaPlayer = null
//        }
//        currentCallGroupId = null
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//        ringtoneReceiver?.let {
//            unregisterReceiver(it)
//        }
//        stopRingtone()
//    }


    private fun playRingtone() {
        try {
            // Stop any existing media player
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                    release()
                }
            }

            // Create a new MediaPlayer instance
            mediaPlayer = MediaPlayer.create(this, R.raw.two_time_ringtone).apply {
                isLooping = false
                setVolume(1.0f, 1.0f)
                start()
            }
        } catch (e: Exception) {
            Log.e("FCM_Service", "Error playing ringtone", e)
        }
    }

//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    override fun onCreate() {
//        super.onCreate()
//
//        // Updated receiver to handle ringtone stopping
//        ringtoneReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                when (intent.action) {
//                    ACTION_STOP_RINGTONE -> {
//                        stopRingtone()
//                    }
//                }
//            }
//        }
//
//        registerReceiver(
//            ringtoneReceiver,
//            IntentFilter(ACTION_STOP_RINGTONE),
//            RECEIVER_NOT_EXPORTED
//        )
//    }

//    class CallNotificationReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                ACTION_IGNORE_CALL -> {
//                    // Stop ringtone via a broadcast method
//                    val stopRingtoneIntent = Intent(ACTION_STOP_RINGTONE)
//                    context.sendBroadcast(stopRingtoneIntent)
//
//                    // Dismiss the notification
//                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                    val groupId = intent.getStringExtra("groupId")
//                    groupId?.let {
//                        nm.cancel(it.hashCode())
//                    }
//                }
//            }
//        }
//    }

}
























//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun sendNotification(
//        notificationType: Int,
//        title: String,
//        body: String,
//        groupId: String,
//        msgType: Int,
//        imgUrl : String
//    ) {
//        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//
//        // Create Notification Channel
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Delivery Notifications",
//            NotificationManager.IMPORTANCE_HIGH
//        )
//
//        nm.createNotificationChannel(channel)
//
//        // Convert Drawable to Bitmap safely
//        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.notifications_24px, null)
//        val bitmap = (drawable as? BitmapDrawable)?.bitmap
//
//        // Group key for this specific chat group
//        val groupKey = when(groupId){
//            ITS_A_BROADCAST_NOTIFICATION -> "${System.currentTimeMillis()}"     // Because for Broadcast notifications I want to show a summary notification, and each time a new notificaiton should be generated hence the unique key i.e. timestamp
//            else -> "GROUP_CHAT_$groupId"
//        }
//
//        // Create intent for when notification is clicked
//        val intent = Intent(this, MainActivity::class.java).apply {
//            putExtra("groupId", groupId)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            groupId.hashCode(),
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//
//        // Get the stored messages for this group
//        val groupMessages = messagesMap[groupId] ?: listOf()
//        val messageCount = groupMessages.size
//
//        val summaryNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.notifications_24px)
//            .setGroup(groupKey)
//            .setGroupSummary(true)  // This is the summary
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//
//        if(msgType == IMG_URL){
//
////            val bitmap = imgToBitmap
//            summaryNotificationBuilder
//                .setContentTitle(title)
//                .setContentText(groupMessages.first())  // Show full message in small preview
//                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap)) // Expand full message
//        }
//        else if (messageCount == 1) {
//            // Single message -> Use BigTextStyle to show full message
//            summaryNotificationBuilder
//                .setContentTitle(title)
//                .setContentText(groupMessages.first())  // Show full message in small preview
//                .setStyle(NotificationCompat.BigTextStyle().bigText(groupMessages.first())) // Expand full message
//        } else {
//            // Multiple messages -> Use InboxStyle to stack messages
//            val inboxStyle = NotificationCompat.InboxStyle()
//                .setBigContentTitle(title)
//                .setSummaryText("$messageCount messages")
//
//            // Add each message to the inbox style
//            groupMessages.forEach { message ->
//                inboxStyle.addLine(message)
//            }
//
//            summaryNotificationBuilder
//                .setContentTitle(title)
//                .setContentText("$messageCount new messages")
//                .setStyle(inboxStyle)
//        }
//
//        nm.notify(groupKey.hashCode(), summaryNotificationBuilder.build())
//    }





//}
















//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun sendNotification(notificationType: Int, title:String, body:String, groupId: String) {
//        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//
//        // Create Notification Channel
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Delivery Notifications",
//            NotificationManager.IMPORTANCE_HIGH
//        )
//
//        nm.createNotificationChannel(channel)
//
//        // Convert Drawable to Bitmap safely
//        // Below is code for obtaining image which would be displayed on notification, don't use "SVG"!!
//        // Because we want the image in "Bitmap" form, and there is a "bit-map" conversion factory which
//        // is internally called, that cannot convert SVG to Bitmap use only PNG (Recommended) and JPG if u want to
//        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.notifications_24px, null)
//        val bitmap = (drawable as? BitmapDrawable)?.bitmap
//
//        // Build Notification
//        val notification = notificationBuilder(notificationType, bitmap, title, body, groupId)
//
//        // Send notification to user using notification manager
//        // Now this "NOTIFICATION_ID" is basically "id" for a notification, if ID is same for two notifications,
//        // then they will be displayed as a single notification, but if ID if differenet, they'll be separate notifications
//        // E.g. in whatsapp, when multiple msgs come from a single person they are all consolidated in a single notification,
//        // but if different people are msging, then they are shown as separate notifications (As they have different IDS :)
//        // we're using "groupId.hashCode()" as NOTIFICATION_ID
//        nm.notify(groupId.hashCode(), notification)
//    }


//    private fun notificationBuilder(notificationType: Int, bitmap: Bitmap?, title: String, body: String, groupId: String): Notification {
//        val groupKey = "GROUP_CHAT_$groupId" // Unique key for each group
//
//        // Get the stored messages for this group
//        val groupMessages = messagesMap[groupId] ?: listOf()
//        val messageCount = groupMessages.size
//
//        // Build summary notification (ensures grouped notifications like WhatsApp)
//        val summaryNotification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle(title)
//            .setContentText("$messageCount new messages")
//            .setSmallIcon(R.drawable.notifications_24px)
//            .setGroup(groupKey)
//            .setGroupSummary(true)  // This is the summary
//            .setAutoCancel(true)
//            .setContentIntent(pendingIntent)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setStyle(NotificationCompat.InboxStyle().apply {
//                setBigContentTitle(title)
//                setSummaryText("$messageCount messages")
//
//                // Add each message to the inbox style
//                groupMessages.forEach { message ->
//                    addLine(message)
//                }
//            })
//            .build()
//
//        return summaryNotification
//    }

/*
    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationBuilderAll(notificationType : Int, bitmap : Bitmap?, title:String, body:String): Notification
    {
       // val notification_icon =  (ResourcesCompat.getDrawable(Resources.getSystem(), R.drawable.notifications_24px, null) as BitmapDrawable).bitmap

        return when(notificationType){

            BASIC_NOTIFICATION -> {
                Notification.Builder(appContext, CHANNEL_ID)
                    .setLargeIcon(bitmap)                               // This img is displayed on right side (Not mandatory) (U will see this in some notifications & in some u won't)
                    .setSmallIcon(R.mipmap.ic_launcher)                 // Use a valid small icon   // This is usually "icon" of the "app" it is displayed on left top corner..
                    .setContentTitle(title)             // Title of notification
                    .setContentText(body)
                    .build()
            }

//        NOTIFICATION_WITH_INTENT_TO_GO_TO_SECOND_ACTIVITY -> {

            /*val intent = Intent(applicationContext, SecondActivity::class.java)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)*/     //  Use of this line : If the activity you're launching (SecondActivity) already exists in the activity stack,
            // all activities on top of it will be removed (cleared), and the existing instance of SecondActivity will be brought to the foreground.
            // If instance of (SecondActivity) is not there in back-stack, it will create a new instance


            /*val pendingIntent = PendingIntent.getActivity(this, REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)*/
            // If a PendingIntent with the same request code (REQ_CODE) already exists,
            // its extras or data are updated with the new intent’s extras or data
            // this is because of "  PendingIntent.FLAG_UPDATE_CURRENT  "
            // This prevents creating multiple PendingIntent objects for the same action

            // PendingIntent.FLAG_IMMUTABLE ---> Immutable (FLAG_IMMUTABLE): The PendingIntent cannot be altered after creation.
            //Use this when you don’t need to modify the intent later.


//            Notification.Builder(this, CHANNEL_ID)
//                .setLargeIcon(bitmap)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("This is notification..")
//                .setContentText("with intent to move to second activity")
//                .setContentIntent(pendingIntent)                        // This line is attaching the "pendingIntent" to notification
//                .setAutoCancel(true)        // Dismiss notification on click (so notification is removed after it has been selected)
//                .build()
//        }

            NOTIFICATION_WITH_BIG_PICTURE_STYLE -> {
                val bigPictureStyle = Notification.BigPictureStyle()
                    /*.bigPicture(bitmap)  */                   // This is the actual "big picture which will be displayed as a large picture"
//                    .bigLargeIcon(notification_icon)                    // This is basically "setLargeIcon" for bigPicture mode, it will replace the "setLargeIcon" when we enter the big picture mode
                    .setBigContentTitle("Image sent by Raman")                      // When u expand the img to see the "big picture"
                    .setSummaryText("This msg will be visible on expanding img")    // these two text msgs will be visible instead of
                // "setContentTitle" and "setContentText"

                Notification.Builder(appContext, CHANNEL_ID)
                    /*.setLargeIcon(bitmap)*/
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Title when not expanded")
                    .setContentText("Text-content when not expanded")
                    .setStyle(bigPictureStyle)      // "setStyle()" function is used to leverage styles when a notification is expanded
                    .build()                        // on expansion code related to this style would be visible
            }

            NOTIFICATION_WITH_INBOX_STYLE -> {
                val inboxStyle = Notification.InboxStyle()
                    .addLine("This is line-1")
                    .addLine("This is line-2")
                    .addLine("This is line-3")
                    .addLine("This is line-4")
                    .addLine("This is line-5")
                    .addLine("This is line-6")
                    .addLine("This is line-7")
                    .addLine("This is line-8")
                    .addLine("This is line-9")
                    .addLine("This is line-10")
                    .setBigContentTitle("This is title on expansion")
                    .setSummaryText("This is content on expansion")

                Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher) // Use a valid small icon
                    .setContentTitle("Title when not expanded")
                    .setContentText("content when not expanded")
                    .setStyle(inboxStyle)                       // This style would be visible on expansion
                    .build()
            }

            else -> {
                Notification.Builder(this, CHANNEL_ID)
                    .setLargeIcon(bitmap)
                    .setSmallIcon(R.mipmap.ic_launcher) // Use a valid small icon
                    .setContentTitle("This is the heading")
                    .setContentText("This is the most basic notification")
                    .build()
            }
        }
    }
}
*/