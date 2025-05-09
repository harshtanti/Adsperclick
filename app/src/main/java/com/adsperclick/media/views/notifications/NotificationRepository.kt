package com.adsperclick.media.views.notifications

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.di.VersionProvider
import com.adsperclick.media.utils.Constants.DB
import com.adsperclick.media.utils.Utils
import com.adsperclick.media.views.notifications.pagingsource.NotificationsPagingSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject



class NotificationRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestore: FirebaseFirestore
) {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var messagesDao: MessagesDao

    @Inject
    lateinit var cloudFunctions: FirebaseFunctions

    @Inject
    lateinit var storageRef: StorageReference

    @Inject
    lateinit var versionProvider: VersionProvider

    @Inject
    lateinit var context: Context



    // ----------------------------------------------------------------------------------------------------------------
    //  NOTIFICATION CREATION, LISTING, UPDATING_LAST_NOTIFICATION_SEEN_TIME, NOTIFICATION_PAGER

    // For notification listing we're using Page-3 library

    private val _createNotificationLiveData = MutableLiveData<NetworkResult<NotificationMsg>>()
    val createNotificationLiveData: LiveData<NetworkResult<NotificationMsg>> get() = _createNotificationLiveData

    // Best practice is to use "Server-side" timestamps (Because someone can change time on their
    // device and a different timestamp could be saved for our notification which we don't want
    // Now, to implement server-side timestamp, we need to send data to backend in a "hashmap"
    // this hashmap would be similar to our data-class, but with a small difference that the
    // timestamp field in it will be populated immediately when it hits the server with the actual time
    suspend fun createNotification(notification: NotificationMsg) {
        _createNotificationLiveData.postValue(NetworkResult.Loading())

        try {
            // This will create an empty document! Basically provide us an unique id
            // for our notification-object, we need this unique id because our notification object
            // has a parameter for notificationId, that needs to be updated
            val documentRef = firestore.collection(DB.NOTIFICATIONS).document()      // This is an offline function

            // Setting Firestore-generated ID inside the notification object
            val updatedNotification = notification.copy(notificationId = documentRef.id)


            val notificationMap = updatedNotification.mapifyForFirestoreTimestamp()

            // Sending our notification object to backend
            // Firestore supports offline persistence! even if internet is not there and user made below
            // request, this request will be queued, and even if this app is running in background of
            // your mobile, and some how internet is accessed, this task will be executed!! As I have
            // tested!
            documentRef.set(notificationMap).await()

            _createNotificationLiveData.postValue(NetworkResult.Success(updatedNotification))
        } catch (e: Exception) {
            _createNotificationLiveData.postValue(NetworkResult.Error(null, e.message ?: "Failed to create notification"))
        }
    }

    fun updateLastNotificationSeenTime(userId: String) {
        try {
            if (userId.isEmpty()) {
                Log.d("skt", "User ID is null or empty, cannot update")
                return
            }

            val userRef = firestore.collection(DB.USERS).document(userId)

            val time = Utils.getTime()
            userRef.update("lastNotificationSeenTime", time)

        } catch (e: Exception) {
            Log.d("skt", "updateLastNotificationSeenTime() function encountered an error: ${e.message}")
        }
    }


    // Note in our "NotificationsPagingSource" or any other PagingSource file, we can't
    // directly apply dependency injection! So we need to send the FirebaseFirestore instance
    // from this file to the "NotificationsPagingSource" so that it can access it :)
    fun getNotificationPager(userRole: Int?): Pager<QuerySnapshot, NotificationMsg> {

        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { NotificationsPagingSource(firestore, userRole) } // Pass Firestore here
        )
    }


    //  NOTIFICATION CREATION, LISTING, UPDATING_LAST_NOTIFICATION_SEEN_TIME, NOTIFICATION_PAGER
    // ----------------------------------------------------------------------------------------------------------------


}

