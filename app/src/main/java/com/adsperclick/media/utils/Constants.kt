package com.adsperclick.media.utils

import android.Manifest
import android.os.Build
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants.MSG_TYPE.MEDIATOR_ANNOUNCEMENT

object Constants {

//    var CC = 0

    var SERVER_MINUS_DEVICE_TIME_LONG : Long = 0L
    var LAST_SEEN_TIME_EACH_USER_EACH_GROUP : Map<String, MutableMap<String, Long?>> ? = null

    const val LIMIT_MSGS = 100          // This controls the number of messages to be fetched at a time
                        // Messaging Fragment adapter is pretty interesting, it loads LIMIT_MSGS no. of
    // of messages once, and when u scroll to top of it, it will load the next 50 msgs and erase the
    // last 50 msgs, making sure only "100 or whatever is LIMIT_MSGS" msgs are there in the
    // adapter at a time :)  so when u open the most recent msg is considered index-100, when u scroll
    // up to top, u reach index-0 and then when u load more, index-0 to 50 msgs r removed &
    // index-100 to 150 messages are loaded into memory so exact messages are index 50 to 150 & u are
    // currently at index 100, so, each time u try to load more pages, u end up becoming the middle
    // of the page :)


    const val TOKEN_FOR_PREFS = "token_prefs"
    const val USER_IDENTITY = "user_identity"
    const val IS_USER_SIGNED_IN = "is_user_signed_in"
    const val SERVER_MINUS_DEVICE_TIME = "Diff bw server-device for syncing"
    const val AGORA_USER_ID = "AgoraUserId"     // This ID uniquely identifies each person on call
    const val EMPTY = ""
    const val SPACE = " "

    val DEFAULT_SERVICE = Service("All", "All");

    object ROLE{
        const val CLIENT = 1        // Can use Enum instead
        const val EMPLOYEE = 2
        const val MANAGER = 3
        const val ADMIN = 4
    }

    object SEND_TO{
        const val CLIENT = 1
        const val EMPLOYEE = 2
        const val BOTH = 70
    }


    const val USER_TYPE_SEMI_CAPS="UserType"
    const val USER_NAME="UserName"
    const val USER_IMAGE="UserImage"
    const val EMPLOYEES_SEMI_CAPS = "Employees"
    const val CLIENTS_SEMI_CAPS = "Clients"
    const val SERVICES_SEMI_CAPS = "Services"
    const val COMPANIES_SEMI_CAPS = "Companies"
    const val USER_TYPE="UserType"
    const val USER_ID="UserId"
    const val GROUP_PROFILE = "GroupProfile"
    const val EMPLOYEE_SINGULAR = "Employee"
    const val COMPANY_SINGULAR = "Company"
    const val GROUP_ID = "GroupId"


    object MSG_TYPE{
        const val TEXT = 180
        const val IMG_URL = 200
        const val PDF_DOC = 220
        const val VIDEO = 240
        const val DOCUMENT= 320
        const val CALL = 360
        const val MEDIATOR_ANNOUNCEMENT = 400
    }

    const val CALL_CHANNEL_ID = "6969"
    const val INCOMING_CALL_NOTIFICATION_ID = 1169

    val READING_MODE_MSG  = Message("alpha",
        "To Read more messages tap here!",
        msgType = MEDIATOR_ANNOUNCEMENT)

    val BOTTOM_MOST_MSG  = Message("bottomMost",
        "To Read more tap here!",
        msgType = MEDIATOR_ANNOUNCEMENT)

    val TOP_MOST_MSG  = Message("topMost",
        "To Read more tap here!",
        msgType = MEDIATOR_ANNOUNCEMENT)


    const val CLICKED_GROUP = "Jis group ko click kia"
    const val LAST_SEEN_GROUP_TIME = "The time at which this group was visited by this user last"

    const val APPLICATION_PDF = "application/pdf"
    const val IMAGE = "image/*"
    const val JPG = ".jpg"
    const val FILES = "files"
    const val yyyyMMdd_HHmmss = "yyyyMMdd_HHmmss"
    const val BACK_SLASH = "/"
    const val DASH = "-"
    const val CAMERA_VISIBLE = "camera"
    const val PDF_VISIBLE = "Doc"
    const val GALLERY_VISIBLE = "gallery"
    const val VIDEO_VISIBLE = "video"
    const val DELETE_VISIBLE = "delete"
    const val CLOSE_VISIBLE = "close"
    const val HEADING_VISIBLE = "heading"

    val REQUIRED_PERMISSIONS_CAMERA  = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            arrayListOf(
                Manifest.permission.CAMERA
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        else -> {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    val REQUIRED_PERMISSIONS_GALLERY_PERMISSION  = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        else -> {
            arrayListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }


    object DB{
        const val USERS = "users"
        const val GROUPS = "groups"
        const val NOTIFICATIONS = "notifications"
        const val MESSAGES = "Messages"
        const val MESSAGES_INSIDE_MESSAGES = "messages"
        const val COMPANY = "companies"
        const val SERVICE = "services"

        const val CONFIG = "config"
        const val SERVER_TIME_DOC = "serverTime"        //Within config collection it is a document (To fetch server-side time and sync it with device time)
        const val MIN_APP_LEVEL_DOC = "minAppLevel"      //Within config collection it is a document"
        const val GROUP_CALL_LOG = "call_log"

        const val GROUP_MEMBERS_LAST_SEEN_TIME = "GroupMembersLastSeenTime"
    }

    object TXT_MSG_TYPE{
        const val SINGLE_MSG_BY_CURRENT_USER = 11
        const val FIRST_MSG_BY_CURRENT_USER = 22
        const val MIDDLE_MSG_BY_CURRENT_USER = 33
        const val LAST_MSG_BY_CURRENT_USER = 44

        const val SINGLE_MSG_RIGHT = 189
        const val FIRST_MSG_RIGHT = 289
        const val MIDDLE_MSG_RIGHT = 389
        const val LAST_MSG_RIGHT = 489

        const val SINGLE_MSG_LEFT = 123
        const val FIRST_MSG_LEFT = 223
        const val MIDDLE_MSG_LEFT = 323
        const val LAST_MSG_LEFT = 423
    }


    object Time{
        const val ONE_HOUR = 3600000L
        const val TWO_HOUR = 7200000L
    }


    object FIREBASE_FUNCTION_NAME{
        const val GET_USER_COUNT = "getUserCount"
        const val SEND_NOTIFICATION_TO_GROUP_MEMBERS = "sendNotificationToGroupMembers"
    }

    object FCM{
        const val CHANNEL_ID = "delivery_notification_channel"
        const val NOTIFICATION_ID = 698334

        // Types of notifications
        const val BASIC_NOTIFICATION = 123132
        const val NOTIFICATION_WITH_INTENT_TO_GO_TO_SECOND_ACTIVITY = 7832
        const val NOTIFICATION_WITH_BIG_PICTURE_STYLE = 298332
        const val NOTIFICATION_WITH_INBOX_STYLE = 3178323

        // For shared-preferences storing current device token-v-
        const val DEVICE_TOKEN = "fcm_token_for_this_device"

        const val ITS_A_BROADCAST_NOTIFICATION = "Not a group, It's a broadcast notification"
        const val ID_OF_GROUP_TO_OPEN = "This contains groupId for group to open in chat fragment"
    }

    const val CLICKED_GROUP_T = "clicked_group"
    const val ROLE_T = "role"

    // Agora constants
    const val AGORA_APP_ID = "9a5d155555ca420193c5fdfbb4f1184d" // Replace with your Agora App ID

    // Temporary token (for development only!) - Replace with your token from Agora console
    const val TEMP_AGORA_TOKEN = "006your-token-from-agora-console"


    object CALL{
        object STATUS{
            const val ONGOING = "ongoing_call"
            const val COMPLETED = "completed_call"
        }
        object TYPE{
            const val VOICE = "voice_call"
            const val VIDEO = "video_call"
        }
    }

    const val INITIATED_A_CALL = "Initiated a Call"
    const val ENDED_THE_CALL = "Ended the Call"
}