package com.adsperclick.media.views.chat.repository

import android.content.Context
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
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.di.VersionProvider
import com.adsperclick.media.views.chat.pagingsource.NotificationsPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB.CONFIG
import com.adsperclick.media.utils.Constants.DB.GROUPS
import com.adsperclick.media.utils.Constants.DB.MESSAGES
import com.adsperclick.media.utils.Constants.DB.MESSAGES_INSIDE_MESSAGES
import com.adsperclick.media.utils.Constants.DB.MIN_APP_LEVEL_DOC
import com.adsperclick.media.utils.Constants.DB.SERVER_TIME_DOC
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.utils.UtilityFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestore:FirebaseFirestore
) {

    @Inject
    lateinit var tokenManager : TokenManager

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



// ----------------------------------------------------------------------------------------------------------------
// WHEN USER OPENS THE APP, THESE FUNCTIONS ARE SUPPOSED TO RUN ONLY ONE TIME, AFTER THE USER OPENS THE APP,
// SO WE RUN THEM ON SPLASH SCREEN

    suspend fun syncUser():ConsumableValue<NetworkResult<User>>{
        try{

            val userId = tokenManager.getUser()?.userId
            val result = firestore.collection(Constants.DB.USERS).document(userId!!).get().await()

            if (result.exists().not()) {
                return ConsumableValue(NetworkResult.Error(null, "User not found in database"))
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
                return ConsumableValue(NetworkResult.Success(it))  // Notify UI with updated user
            } ?: return ConsumableValue(NetworkResult.Error(null, "Failed to parse user data"))
        }
        catch (e: Exception){
            return ConsumableValue(NetworkResult.Error(null, "Error: ${e.message}"))
        }
    }

    suspend fun syncDeviceTime() {
        try {
            if(UtilityFunctions.isNetworkAvailable(context).not()){ // To handle the case user is offline
                Log.d("skt", "User is offline") // This function is updating firebase, so c
                                        // annot be run when user is offline
                return
            }


            val docRef = firestore.collection(CONFIG).document(SERVER_TIME_DOC)

            // Update the document with the server timestamp
            docRef.update(SERVER_TIME_DOC, FieldValue.serverTimestamp()).await()

            // Fetch the updated timestamp
            val snapshot = docRef.get().await()
            snapshot.getTimestamp(SERVER_TIME_DOC)?.let { serverTime ->
                val timeDiff = UtilityFunctions.timestampToLong(serverTime) - System.currentTimeMillis()
                tokenManager.setServerMinusDeviceTime(timeDiff)
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException,  // No internet connection
                is FirebaseFirestoreException -> {
                    Log.e("skt", "Network error: ${e.message}")
                }
                else -> Log.e("skt", "Unexpected error: ${e.message}")
            }
        }
    }
    suspend fun isCurrentVersionAcceptable(): Boolean {
        return try {
            val result = firestore.collection(CONFIG).document(MIN_APP_LEVEL_DOC).get().await()
            val minAcceptableVersion = result.getLong(MIN_APP_LEVEL_DOC) ?: 1

            val currentAppVersion = versionProvider.appVersion // Get current app version

            if (minAcceptableVersion == null) {
                Log.e("compareAppVersion", "Invalid minAcceptableAppLvl from Firestore")
                return true // Assume app is valid if Firestore data is incorrect
            }

            // If current app version is lower than min required, return false (force update needed)
            currentAppVersion >= minAcceptableVersion
        } catch (e: Exception) {
            Log.e("compareAppVersion", "Error: ${e.message}")
            true // In case of Firestore failure, allow app usage (fail-safe)
        }
    }


    // ---------------------------------------------------------------------------------------------------------------------------------------------------

    private val _lastSeenForEachUserEachGroupLiveData =
        MutableLiveData<ConsumableValue<NetworkResult<Map<String, List<Pair<String, Long?>>>>>>()
    val lastSeenForEachUserEachGroupLiveData:
            LiveData<ConsumableValue<NetworkResult<Map<String, List<Pair<String, Long?>>>>>> get() = _lastSeenForEachUserEachGroupLiveData

    private fun fetchLastSeenTimeForEachUserInEachGroup(listOfGroupChatId: List<String>){
        firestore.collectionGroup("GroupMembersLastSeenTime")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lastSeenData = mutableMapOf<String, MutableList<Pair<String, Long?>>>()

                for (doc in querySnapshot.documents) {
                    val groupId = doc.reference.parent.parent?.id // Get the groupId from the parent document

                    if(groupId in listOfGroupChatId){
                        val userId = doc.id
                        val lastSeenTimestamp = doc.getTimestamp("lastSeenTime")
                        val timestampInLong = UtilityFunctions.timestampToLong(lastSeenTimestamp)

                        groupId?.let {
                            lastSeenData.getOrPut(groupId) { mutableListOf() }
                                .add(userId to timestampInLong)
                        }
                    }
                }
                _lastSeenForEachUserEachGroupLiveData.postValue(ConsumableValue(NetworkResult.Success(lastSeenData)))
            }
            .addOnFailureListener(){
                _lastSeenForEachUserEachGroupLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error: ${it.message}")))
            }
    }


    private val _listOfGroupChatLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>>()
    val listOfGroupChatLiveData: LiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>> get() = _listOfGroupChatLiveData

    suspend fun listenToGroupChatUpdates(listOfGroupChatId: List<String>) {

        fetchLastSeenTimeForEachUserInEachGroup(listOfGroupChatId)      // The lines below will
        // execute immediately and will not wait for this function to return because we are not
        // using "await()" in the above function, we are using "call-backs" i.e. onSuccessListener

        _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        // Firestore listener (Real-time updates)
        val query = firestore.collection(GROUPS)
            .whereIn("groupId", listOfGroupChatId)

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error: ${error.message}")))
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

                _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Success(sortedGroups)))
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

    private val _lastSeenTimestampLiveData = MutableLiveData<ConsumableValue<NetworkResult<Long>>>()
    val lastSeenTimestampLiveData = _lastSeenTimestampLiveData

    suspend fun fetchAllNewMessages(groupId: String) {
        val timeStampOfLastMsgInRoom = messagesDao.getLatestMsgTimestampOrZero(groupId) ?: 0L

        val timestampDataType = UtilityFunctions.longToTimestamp(timeStampOfLastMsgInRoom)      // To obtain timestamp in "Timestamp" data type
        try {

            // First fetching the last-seen timestamp from firestore to find out where we need to show the read message :)
            val lastSeenTimestamp = firestore.collection(MESSAGES)
                .document(groupId).collection("GroupMembers")
                .document(tokenManager.getUser()?.userId!!).get().await()

            _lastSeenTimestampLiveData.postValue(
                ConsumableValue(NetworkResult.Success(
                    UtilityFunctions.timestampToLong(lastSeenTimestamp.getTimestamp("lastSeenTime")))))

            // This we are doing coz firestore has a limit, each document can be of atmost 1MB, so
            // we need each "Message" to be a single document so that there's no issue as per backend limits

            val querySnapshot = firestore.collection(MESSAGES)
                .document(groupId)
                .collection(MESSAGES_INSIDE_MESSAGES)                   // Subcollection for messages
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

        chatListener = firestore.collection(MESSAGES)
            .document(groupId)
            .collection(MESSAGES_INSIDE_MESSAGES)
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


    suspend fun sendMessage(msgText: String, groupId: String, user: User, groupName: String,
                            listOfGroupMemberId: List<String>, msgType: Int) {
        val messagesRef = firestore.collection(MESSAGES)
            .document(groupId)
            .collection(MESSAGES_INSIDE_MESSAGES)

        val msgId = messagesRef.document().id // Generate a unique ID for the message

        val message = Message(
            msgId = msgId,
            message = msgText,
            senderId = user.userId,
            senderName = user.userName,
            senderRole = user.role,
            msgType = msgType,
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
                            firestore.collection(GROUPS)
                                .document(groupId)
                                .update("lastSentMsg", msg) // ✅ Save correct message with timestamp
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error sending message: ${e.message}")
            }

        // firestore.collection(Constants.DB.GROUPS).document(groupId).update("lastSentMsg", message)

        message.message?.let {msgText->
            message.senderId?.let { senderId ->
                message.groupId?.let { groupId->

                    triggerNotificationToGroupMembers(groupId, groupName, msgText, senderId, listOfGroupMemberId)
                } } }
    }

//    groupName, msgText, senderId, listOfGroupMemberId

    private suspend fun triggerNotificationToGroupMembers(
        groupId: String, groupName:String, msgText: String, senderId: String, listOfGroupMemberId: List<String>){
        val data = mapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "msgText" to msgText,
            "senderId" to senderId,
            "listOfGroupMemberId" to listOfGroupMemberId
        )

        try {
            cloudFunctions
                .getHttpsCallable(Constants.FIREBASE_FUNCTION_NAME.SEND_NOTIFICATION_TO_GROUP_MEMBERS)
                .call(data)
                .await()
        } catch (ex: Exception){
            Log.e("skt", "Error : $ex")
        }
    }




    fun updateLastReadMessage(groupId: String, userId: String){
        try {
            val data = mapOf(
                "lastSeenTime" to FieldValue.serverTimestamp()
            )
            firestore.collection(GROUPS).document(groupId)
                .collection("GroupMembersLastSeenTime").document(userId).set(data)

        } catch (ex: Exception){
            Log.e("skt", "Error : $ex")
        }
    }


    private val _imageUploadedLiveData = MutableLiveData<ConsumableValue<NetworkResult<String>>>()
    val imageUploadedLiveData: LiveData<ConsumableValue<NetworkResult<String>>> get() = _imageUploadedLiveData

    suspend fun uploadFile(groupId: String, groupName: String, file: File?,
                           listOfUsers: List<String>, msgType: Int){
        return try {
            // If file is not null, upload it and get the URL
            if (file != null) {
                _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Loading()))
                val storageRef = storageRef
                val imagePath = "SharedInGroup/${groupName}_${groupId}/images/${System.currentTimeMillis()}_${file.name}"
                val imageRef = storageRef.child(imagePath)

                // Upload the file
                val uploadTask = imageRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                // Get the download URL
                val imageUrl = imageRef.downloadUrl.await().toString()
                sendMessage(imageUrl, groupId, tokenManager.getUser()!!, groupName, listOfUsers, msgType)

                _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Success(imageUrl)))
            } else {
                _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Couldn't upload img..")))
            }
        } catch (e: Exception) {
            _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Error(null, e.message ?: "Couldn't upload img..")))
        }
    }




    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun createGroup(data: GroupChatListingData,file: File) = apiService.createGroup(data,file)

    suspend fun getCompanyNameData(companyId: String) = apiService.getCompanyNameData(companyId)

    suspend fun getMultipleUsers(userIds: List<String>) = apiService.getMultipleUsers(userIds)

    suspend fun updateGroupProfile(groupId:String,groupName:String?, file: File?) = apiService.updateGroupProfile(groupId,groupName,file)

    suspend fun removeUserFromGroup(userId: String, groupId: String) = apiService.removeUserFromGroup(userId,groupId)

    suspend fun getGroupDetails(groupId: String) = apiService.getGroupDetails(groupId)

    suspend fun addGroupMember(groupId:String,userSet: MutableSet<String>) = apiService.addGroupMember(groupId,userSet)

}

