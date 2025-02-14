package com.adsperclick.media.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    @Inject
    lateinit var tokenManager : TokenManager

    private val _createNotificationLiveData = MutableLiveData<NetworkResult<NotificationMsg>>()
    val createNotificationLiveData: LiveData<NetworkResult<NotificationMsg>> get() = _createNotificationLiveData

    suspend fun createNotification(notification: NotificationMsg) {
        _createNotificationLiveData.postValue(NetworkResult.Loading())

        try {
            // This will create an empty document! Basically provide us an unique id
            // for our notification-object, we need this unique id because our notification object
            // has a parameter for notificationId, that needs to be updated
            val documentRef = db.collection(Constants.DB.NOTIFICATIONS).document()      // This is an offline function

            // Setting Firestore-generated ID inside the notification object
            val updatedNotification = notification.copy(notificationId = documentRef.id)

            // Sending our notification object to backend
            // Firestore supports offline persistence! even if internet is not there and user made below
            // request, this request will be queued, and even if this app is running in background of
            // your mobile, and some how internet is accessed, this task will be executed!! As I have
            // tested!
            db.collection(Constants.DB.NOTIFICATIONS).document(documentRef.id).set(updatedNotification).await()

            _createNotificationLiveData.postValue(NetworkResult.Success(updatedNotification))
        } catch (e: Exception) {
            _createNotificationLiveData.postValue(NetworkResult.Error(null, e.message ?: "Failed to create notification"))
        }
    }
}