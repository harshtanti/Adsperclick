package com.adsperclick.media.data.repositories

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.pagingsource.NotificationsPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.UtilityFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.sql.Time
import javax.inject.Inject

class ChatRepository @Inject constructor(private val apiService: ApiService, private val firestore:FirebaseFirestore) {

    @Inject
    lateinit var tokenManager : TokenManager

    @Inject
    lateinit var messagesDao: MessagesDao

// ----------------------------------------------------------------------------------------------------------------
//  NOTIFICATION CREATION, LISTING, UPDATING_LAST_NOTIFICATION_SEEN_TIME, NOTIFICATION_PAGER


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
            val documentRef = firestore.collection(Constants.DB.NOTIFICATIONS).document()      // This is an offline function

            // Setting Firestore-generated ID inside the notification object
            val updatedNotification = notification.copy(notificationId = documentRef.id)


            val notificationMap = updatedNotification.mapifyForFirestoreTimestamp()
            /*hashMapOf(
                "notificationId" to documentRef.id,
                "notificationTitle" to notification.notificationTitle,
                "notificationDescription" to notification.notificationDescription,
                "sentTo" to notification.sentTo,
                "timestamp" to FieldValue.serverTimestamp() // Setting server-side timestamp, it will populate the field with "time" at that time
            )*/

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
            val querySnapshot = firestore.collection(Constants.DB.NOTIFICATIONS).get().await()
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

    suspend fun updateLastNotificationSeenTime() {
        try {
            val userId = tokenManager.getUser()?.userId
            if (userId.isNullOrEmpty()) {
                Log.d("skt", "User ID is null or empty, cannot update")
                return
            }

            val userRef = firestore.collection(Constants.DB.USERS).document(userId)

            userRef.update("lastNotificationSeenTime", System.currentTimeMillis())
                .addOnSuccessListener {
                    Log.d("skt", "Successfully updated lastNotificationSeenTime")
                }
                .addOnFailureListener { e ->
                    Log.d("skt", "Failed to update lastNotificationSeenTime: ${e.message}")
                }

        } catch (e: Exception) {
            Log.d("skt", "updateLastNotificationSeenTime() function encountered an error: ${e.message}")
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
            pagingSourceFactory = { NotificationsPagingSource(firestore) } // Pass Firestore here
        )
    }


//  NOTIFICATION CREATION, LISTING, UPDATING_LAST_NOTIFICATION_SEEN_TIME, NOTIFICATION_PAGER
// ----------------------------------------------------------------------------------------------------------------


    private val _userLiveData = MutableLiveData<NetworkResult<User>>()
    val userLiveData: LiveData<NetworkResult<User>> get() = _userLiveData

    suspend fun syncUser(){
        _userLiveData.postValue(NetworkResult.Loading())
        try{
            val userId = tokenManager.getUser()?.userId
            val result = firestore.collection(Constants.DB.USERS).document(userId!!).get().await()

            if (result.exists().not()) {
                _userLiveData.postValue(NetworkResult.Error(null, "User not found in database"))
                return
            }

            val user = result.toObject(User::class.java) // Convert Firestore doc to User object

            if(user?.role != Constants.ROLE.CLIENT){
                // Now we fetch list of all services and put it in "User" object, thereafter it will be saved in shared Prefs
                val servicesList = firestore.collection(Constants.DB.SERVICE).orderBy("serviceName").get().await()
                val listOfServices = arrayListOf<Service>(DEFAULT_SERVICE)      // Filled first service with default service i.e. "All"
                for(document in servicesList.documents){
                    val service = document.toObject(Service::class.java)
                    service?.let {
                        listOfServices.add(it)
                    }
                }
                user?.listOfServicesAssigned = listOfServices
            }

            user?.let {
                // Write code for when new "User" object obtained from backend successfully
                tokenManager.saveUser(it) // Update SharedPreferences
                _userLiveData.postValue(NetworkResult.Success(it)) // Notify UI with updated user
            } ?: _userLiveData.postValue(NetworkResult.Error(null, "Failed to parse user data"))
        }
        catch (e: Exception){
            _userLiveData.postValue(NetworkResult.Error(null, "Error: ${e.message}"))
        }
    }

    private val _listOfGroupChatLiveData = MutableLiveData<NetworkResult<List<GroupChatListingData>>>()
    val listOfGroupChatLiveData: LiveData<NetworkResult<List<GroupChatListingData>>> get() = _listOfGroupChatLiveData

    suspend fun listenToGroupChatUpdates(listOfGroupChatId: List<String>) {
        _listOfGroupChatLiveData.postValue(NetworkResult.Loading())

        // Firestore listener (Real-time updates)
        val query = firestore.collection(Constants.DB.GROUPS)
            .whereIn("groupId", listOfGroupChatId)

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _listOfGroupChatLiveData.postValue(NetworkResult.Error(null, "Error: ${error.message}"))
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val listOfGroups = arrayListOf<GroupChatListingData>()

                for (document in snapshot.documents) {
                    val group = document.toObject(GroupChatListingData::class.java)
                    group?.let { listOfGroups.add(it) }
                }

                // Sort by lastSentMsg timestamp in descending order (latest message first)
                val sortedGroups = listOfGroups.sortedByDescending { it.lastSentMsg?.timestamp ?: 0L }

                _listOfGroupChatLiveData.postValue(NetworkResult.Success(sortedGroups))
            }
        }
    }



    fun getUserListData(searchQuery: String, userRole: Int): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { UserCommunityPagingSource(firestore, searchQuery, userRole) }
        ).flow
    }


// ----------------------------------------------------------------------------------------------------------------
//  MESSAGE RETRIEVAL FROM FIREBASE AND REALTIME LISTENING
    fun getChatsForRoom(roomId: String): LiveData<List<Message>> {
        return messagesDao.getChatsForThisRoom(roomId)
    }

    fun DocumentSnapshot.toMessage(): Message? {
        return try {
            val timestampLong = UtilityFunctions.timestampToLong(this.getTimestamp("timestamp"))     // Convert Firestore Timestamp -> Long

            Message(
                msgId = getString("msgId") ?: "null string",
                message = getString("message"),
                senderId = getString("senderId"),
                senderName = getString("senderName"),
                senderRole = getLong("senderRole")?.toInt(),
                msgType = getLong("msgType")?.toInt(),
                groupId = getString("groupId"),
                timestamp = timestampLong // Store as Long (milliseconds)
            )
        } catch (e: Exception) {
            Log.e("Firestore", "Error parsing Message: ${e.message}")
            null
        }
    }

    suspend fun fetchAllNewMessages(groupId: String) {
        val timeStampOfLastMsgInRoom = messagesDao.getLatestMsgTimestampOrZero(groupId) ?: 0L

        val timestampDataType = UtilityFunctions.longToTimestamp(timeStampOfLastMsgInRoom)      // To obtain timestamp in "Timestamp" data type
        try {
            val querySnapshot = firestore.collection(Constants.DB.MESSAGES)
                .document(groupId)
                .collection("messages")  // Subcollection for messages
                .whereGreaterThan("timestamp", timestampDataType) // Query only new messages
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await() // 🔹 Convert Firestore call into coroutine

            val newMessages = querySnapshot.documents.mapNotNull { it.toMessage() }

            if (newMessages.isNotEmpty()) {

                CoroutineScope(Dispatchers.IO).launch {
                    messagesDao.insertMessageList(newMessages) // ✅ Now it's inside a suspend function, so no coroutine error
                }

                // Get the latest timestamp for real-time listener
                val latestTimestampForRealtimeListening = newMessages.maxOfOrNull { it.timestamp ?: 0L } ?: timeStampOfLastMsgInRoom

                val lastTimestamp = UtilityFunctions.longToTimestamp(latestTimestampForRealtimeListening)
                realtimeChatUpdatesListener(groupId, lastTimestamp)
            } else {
                // If no new messages found, start listening from the last known timestamp
                realtimeChatUpdatesListener(groupId, timestampDataType)
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching messages: ${e.message}")
        }
    }


    private var chatListener: ListenerRegistration? = null
    var lastMsgTimeStamp:Long = 0L

    // So in Firestore things don't work like Firebase-realtime database,
    // In realtime database we were able to obtain new messages one - by - one
    // and only the new unread message was fetched,
    // But here that's not possible/dangerous to implement,
    // So after realtimeChatUpdatesListener starts listening and users start chatting
    // after every msg sent it will return all messages post the listening timestamp
    // Which means, if 49 messages are sent in group and I send the 50th message, then
    // snapshot will return 50 msgs ... It is what it is... so we're using "realtimeChatUpdatesListener"
    // so we can recognise "newMessages" from "allMessages"
     fun realtimeChatUpdatesListener(groupId: String, lastTimestamp: Timestamp) {
        // Remove any existing listener before setting a new one
        chatListener?.remove()

        chatListener = firestore.collection(Constants.DB.MESSAGES)
            .document(groupId)
            .collection("messages")
            .whereGreaterThan("timestamp", lastTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Realtime updates error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val allMessages = snapshot.documents.mapNotNull { it.toMessage() }  // Here "allMessages" means all messages post the "timestamp" in firebase query
                    val newMessage = allMessages.filter { (it.timestamp ?: 0L) > lastMsgTimeStamp }     // To fetch only the messages post "lastMsgTimeStamp"

                    lastMsgTimeStamp = newMessage.maxOfOrNull { it.timestamp ?: 0L } ?: lastMsgTimeStamp
                    if (newMessage.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            messagesDao.insertMessageList(newMessage) // ✅ Now runs in IO thread safely
                        }
                    }
                }
            }
    }

    fun stopRealtimeListening(){
        chatListener?.remove()
    }
// ----------------------------------------------------------------------------------------------------------------



    fun sendMessage(msgText: String, groupId: String, user: User) {
        val messagesRef = firestore.collection(Constants.DB.MESSAGES)
            .document(groupId)
            .collection("messages")

        val msgId = messagesRef.document().id // Generate a unique ID for the message

        val message = Message(
            msgId = msgId,
            message = msgText,
            senderId = user.userId,
            senderName = user.userName,
            senderRole = user.role,
            msgType = Constants.MSG_TYPE.TEXT,
            groupId = groupId
        )

        messagesRef.document(msgId)
            .set(message.toMapForFirestore()) // Convert to Map to use server timestamp
            .addOnSuccessListener {
                Log.d("Firestore", "Message sent successfully: $msgId")
                // Now since our "Message" object is sent to server, it means the firestore
                // timestamp is updated on it we'll get it back using the "msgId"
                messagesRef.document(msgId).get()
                    .addOnSuccessListener {
                        val updatedMessage = it.toMessage()
                        updatedMessage?.let { msg ->
                            firestore.collection(Constants.DB.GROUPS)
                                .document(groupId)
                                .update("lastSentMsg", msg) // ✅ Save correct message with timestamp
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error sending message: ${e.message}")
            }

        // firestore.collection(Constants.DB.GROUPS).document(groupId).update("lastSentMsg", message)
    }



    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun createGroup(data: GroupChatListingData,file: File) = apiService.createGroup(data,file)

    suspend fun uploadGroupImage(imageFile: File): NetworkResult<String> {
        return try {
            // Create a storage reference
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("group_profile_images/${System.currentTimeMillis()}_${imageFile.name}")

            // Upload the file
            val uploadTask = imageRef.putFile(Uri.fromFile(imageFile))

            // Wait for the upload to complete
            val taskSnapshot = uploadTask.await()

            // Get the download URL
            val downloadUrl = imageRef.downloadUrl.await().toString()

            NetworkResult.Success(downloadUrl)
        } catch (e: Exception) {
            NetworkResult.Error(null, "Failed to upload image: ${e.message}")
        }
    }
}

