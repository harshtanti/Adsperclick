package com.adsperclick.media.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.pagingsource.NotificationsPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.adsperclick.media.utils.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor(private val apiService: ApiService,private val db:FirebaseFirestore) {

    @Inject
    lateinit var tokenManager : TokenManager

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
            val documentRef = db.collection(Constants.DB.NOTIFICATIONS).document()      // This is an offline function

            // Setting Firestore-generated ID inside the notification object
            val updatedNotification = notification.copy(notificationId = documentRef.id)


            val notificationMap = hashMapOf(
                "notificationId" to documentRef.id,
                "notificationTitle" to notification.notificationTitle,
                "notificationDescription" to notification.notificationDescription,
                "sentTo" to notification.sentTo,
                "timestamp" to FieldValue.serverTimestamp() // Setting server-side timestamp, it will populate the field with "time" at that time
            )

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

    private val _listNotificationLiveData = MutableLiveData<NetworkResult<List<NotificationMsg>>>()
    val listNotificationLiveData: LiveData<NetworkResult<List<NotificationMsg>>> get() = _listNotificationLiveData

    suspend fun listNotifications(){
        _listNotificationLiveData.postValue(NetworkResult.Loading())

        try {
            val querySnapshot = db.collection(Constants.DB.NOTIFICATIONS).get().await()
            val notificationList = arrayListOf<NotificationMsg>()

            for(document in querySnapshot.documents){
                val notification = document.toObject(NotificationMsg::class.java)
                notification?.let {
                    notificationList.add(notification)
                }
            }

            _listNotificationLiveData.postValue(NetworkResult.Success(notificationList))
        } catch (e : Exception){
            _listNotificationLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _userLiveData = MutableLiveData<NetworkResult<User>>()
    val userLiveData: LiveData<NetworkResult<User>> get() = _userLiveData

    suspend fun syncUser(){
        _userLiveData.postValue(NetworkResult.Loading())
        try{
            val userId = tokenManager.getUser()?.userId
            val result = db.collection(Constants.DB.USERS).whereEqualTo("userId", userId).get().await()

            if (result.isEmpty) {
                _userLiveData.postValue(NetworkResult.Error(null, "User not found in database"))
                return
            }

            val user = result.documents[0].toObject(User::class.java) // Convert Firestore doc to User object

            user?.let {
                tokenManager.saveUser(it) // Update SharedPreferences
                _userLiveData.postValue(NetworkResult.Success(it)) // Notify UI with updated user
            } ?: _userLiveData.postValue(NetworkResult.Error(null, "Failed to parse user data"))
        }
        catch (e: Exception){
            _userLiveData.postValue(NetworkResult.Error(null, "Error: ${e.message}"))
        }
    }

    // Note in our "NotificationsPagingSource" or any other PagingSource file, we can't
    // directly apply dependency injection! So we need to send the FirebaseFirestore instance
    // from this file to the "NotificationsPagingSource" so that it can access it :)
    fun getNotificationPager(): Pager<QuerySnapshot, NotificationMsg> {

        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { NotificationsPagingSource(db) } // Pass Firestore here
        )
    }

    fun getUserListData(searchQuery: String, userRole: Int): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { UserCommunityPagingSource(db, searchQuery, userRole) }
        ).flow
    }

    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun createGroup(data: GroupChatListingData) = apiService.createGroup(data)
}

