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
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.di.VersionProvider
import com.adsperclick.media.views.chat.pagingsource.NotificationsPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
import com.adsperclick.media.utils.Constants.LAST_SEEN_TIME_EACH_USER_EACH_GROUP
import com.adsperclick.media.utils.Constants.LIMIT_MSGS
import com.adsperclick.media.utils.Constants.MSG_TYPE.IMG_URL
import com.adsperclick.media.utils.Constants.MSG_TYPE.PDF_DOC
import com.adsperclick.media.utils.Constants.MSG_TYPE.VIDEO
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.utils.UtilityFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
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

            val time = UtilityFunctions.getTime()
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



// ----------------------------------------------------------------------------------------------------------------
// WHEN USER OPENS THE APP, THESE FUNCTIONS ARE SUPPOSED TO RUN ONLY ONE TIME, AFTER THE USER OPENS THE APP,
// SO WE RUN THEM ON SPLASH SCREEN

    suspend fun syncUser():ConsumableValue<NetworkResult<User>>{
        try{

            val userId = tokenManager.getUser()?.userId
            val result = firestore.collection(DB.USERS).document(userId!!).get().await()

            if (result.exists().not()) {
                return ConsumableValue(NetworkResult.Error(null, "User not found in database"))
            }

            val user = result.toObject(User::class.java) // Convert Firestore doc to User object

            if(user?.role != Constants.ROLE.CLIENT){
                // Now we fetch list of all services and put it in "User" object, thereafter it will be saved in shared Prefs
                val servicesList = firestore.collection(DB.SERVICE).orderBy("serviceName").get().await()
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
                fetchLastSeenTimeForEachUserInEachGroup(it.listOfGroupsAssigned?: listOf())

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
                                        // Cannot be run when user is offline
                return
            }


            val docRef = firestore.collection(DB.CONFIG).document(DB.SERVER_TIME_DOC)

            // Update the document with the server timestamp
            docRef.update(DB.SERVER_TIME_DOC, FieldValue.serverTimestamp()).await()

            // Fetch the updated timestamp
            val snapshot = docRef.get().await()
            snapshot.getTimestamp(DB.SERVER_TIME_DOC)?.let { serverTime ->
                val timeDiff = UtilityFunctions.timestampToLong(serverTime) - System.currentTimeMillis()
                // Some timeDiff coz of time taken to fetch server-side time, so we ignore upto 1 sec
                if(timeDiff > 1000L) tokenManager.setServerMinusDeviceTime(timeDiff)
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
            val result = firestore.collection(DB.CONFIG).document(DB.MIN_APP_LEVEL_DOC).get().await()
            val minAcceptableVersion = result.getLong(DB.MIN_APP_LEVEL_DOC) ?: 1

            val currentAppVersion = versionProvider.appVersion // Get current app version

            // If current app version is lower than min required, return false (force update needed)
            currentAppVersion >= minAcceptableVersion
        } catch (e: Exception) {
            Log.e("compareAppVersion", "Error: ${e.message}")
            true // In case of Firestore failure, allow app usage (fail-safe)
        }
    }


    // ---------------------------------------------------------------------------------------------------------------------------------------------------

    private val _lastSeenForEachUserEachGroupLiveData =
        MutableLiveData<NetworkResult<Map<String, MutableMap<String, Long?>>>>()
    val lastSeenForEachUserEachGroupLiveData:
            LiveData<NetworkResult<Map<String, MutableMap<String, Long?>>>>
        get() = _lastSeenForEachUserEachGroupLiveData


    suspend fun fetchLastSeenTimeForEachUserInEachGroup(
        listOfGroupChatId: List<String>
    ): NetworkResult<Map<String, MutableMap<String, Long?>>> {
        return try {
            val querySnapshot = firestore.collectionGroup(DB.GROUP_MEMBERS_LAST_SEEN_TIME).get().await()
            val lastSeenData = mutableMapOf<String, MutableMap<String, Long?>>()

            for (doc in querySnapshot.documents) {
                val groupId = doc.reference.parent.parent?.id // Get the groupId from the parent document

                if (groupId in listOfGroupChatId) {
                    val userId = doc.id
                    val lastSeenTimestamp = doc.getTimestamp("lastSeenTime")
                    val timestampInLong = UtilityFunctions.timestampToLong(lastSeenTimestamp)

                    groupId?.let {
                        lastSeenData.getOrPut(groupId) { mutableMapOf() }[userId] = timestampInLong
                    }
                }
            }
            LAST_SEEN_TIME_EACH_USER_EACH_GROUP = lastSeenData
            NetworkResult.Success(lastSeenData)
        } catch (e: Exception) {
            Log.e("skt", "Error: ${e.message}")
            NetworkResult.Error(null, "Error: ${e.message}")
        }
    }


    private val _listOfGroupChatLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>>()
    val listOfGroupChatLiveData: LiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>> get() = _listOfGroupChatLiveData

    suspend fun listenToGroupChatUpdates(listOfGroupChatId: List<String>) {

        // Just for knowledge if "fetchLastSeenTimeForEachUserInEachGroup" was here..
        // fetchLastSeenTimeForEachUserInEachGroup(listOfGroupChatId)
        // The lines below will
        // execute immediately and will not wait for this function to return because we are not
        // using "await()" in the above function, we are using "call-backs" i.e. onSuccessListener

        try {
            _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

            // Firestore listener (Real-time updates)
            val query = firestore.collection(DB.GROUPS)
                .whereIn("groupId", listOfGroupChatId)

            query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error: ${error.message}")))
                    Log.e("skt", "Error: ${error.message}")
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
        } catch (e:Exception){
            Log.d("skt", "Error: ${e.message}")
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
//  MESSAGE RETRIEVAL FROM FIREBASE AND REALTIME LISTENING IN MESSAGING_FRAGMENT
    fun getChatsForGroup(groupId: String): LiveData<List<Message>> {
        return messagesDao.getChatsForThisGroup(groupId, LIMIT_MSGS)
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
            // This we are doing coz firestore has a limit, each document can be of atmost 1MB, so
            // we need each "Message" to be a single document so that there's no issue as per backend limits

            val querySnapshot = firestore.collection(DB.MESSAGES)
                .document(groupId)
                .collection(DB.MESSAGES_INSIDE_MESSAGES)                   // Subcollection for messages
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

         try {
             // Remove any existing listener before setting a new one
             chatListener?.remove()

             chatListener = firestore.collection(DB.MESSAGES)
                 .document(groupId)
                 .collection(DB.MESSAGES_INSIDE_MESSAGES)
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
         } catch (e:Exception){
             Log.e("skt", "Error : $e")
         }
    }

    fun stopRealtimeListening(){
        chatListener?.remove()
    }

    fun getSpecifiedMessages(groupId: String, limit:Int, offset:Int): NetworkResult<List<Message>>{

        return try {
            val msgs = messagesDao.getSpecifiedMessages(groupId, limit, offset)
            return NetworkResult.Success(msgs ?: listOf())
        } catch (e: Exception){
            Log.e("skt", "Error: ${e.message}")
            NetworkResult.Error(null, "Error ${e.message}")
        }
    }
// ----------------------------------------------------------------------------------------------------------------


    suspend fun sendMessage(msgText: String, groupId: String, user: User, groupName: String,
                            listOfGroupMemberId: List<String>, msgType: Int) {
        val messagesRef = firestore.collection(DB.MESSAGES)
            .document(groupId)
            .collection(DB.MESSAGES_INSIDE_MESSAGES)

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
                            firestore.collection(DB.GROUPS)
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

//    groupName, msgText, senderId, listOfGroupMemberId

    suspend fun triggerNotificationToGroupMembers(
        groupId: String, groupName:String, msgText: String, senderId: String,
        msgType: Int, listOfGroupMemberId: List<String>)
    {
        val data = mapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "msgText" to msgText,
            "msgType" to msgType,
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
            firestore.collection(DB.GROUPS).document(groupId)
                .collection(Constants.DB.GROUP_MEMBERS_LAST_SEEN_TIME).document(userId).set(data)

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

                val fileType = when(msgType){
                    IMG_URL -> "images"
                    PDF_DOC -> "pdfs"
                    VIDEO -> "videos"
                    else -> {"Unknown"}
                }

                val filePath = "SharedInGroup/${groupName}_${groupId}/$fileType/${System.currentTimeMillis()}_${file.name}"
                val fileRef = storageRef.child(filePath)

                // Upload the file
                val uploadTask = fileRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                // Get the download URL
                val imageUrl = fileRef.downloadUrl.await().toString()

                // After URL is created at backend and obtained here, we sendMessage & notification to group-members
                sendMessage(imageUrl, groupId, tokenManager.getUser()!!, groupName, listOfUsers, msgType)

                tokenManager.getUser()?.userId?.let {
                    triggerNotificationToGroupMembers(groupId= groupId,
                        groupName= groupName, msgText = imageUrl, senderId = it,
                        msgType = msgType, listOfGroupMemberId = listOfUsers)
                }

                _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Success(imageUrl)))
            } else {
                _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Couldn't upload img..")))
            }
        } catch (e: Exception) {
            Log.e("skt", "Error: ${e.message}")
            _imageUploadedLiveData.postValue(ConsumableValue(NetworkResult.Error(null, e.message ?: "Couldn't upload img..")))
        }
    }



    suspend fun getAgoraCallToken(data : HashMap<String,String>):NetworkResult<String> {
        try {
            // Call the Cloud Function
            val response = cloudFunctions.getHttpsCallable("generateAgoraToken").call(data).await()

            // Extract the result data
            val result = response.getData() as HashMap<String, String?>

            // Update shared pref if "agoraUserId" is fetched for first time
            if(tokenManager.getUser()?.agoraUserId == null){
                val userObj = tokenManager.getUser()?.copy(agoraUserId = result["agoraUserId"]?.toIntOrNull())
                userObj?.let {user->
                    tokenManager.saveUser(user)
                }
            }
            // Return the token
            result["token"]?.let { token->
                return NetworkResult.Success(token)
            } ?: run {
                return NetworkResult.Error(null, "Failed to obtain token")
            }
        } catch (e: Exception){
            Log.e("skt", e.message ?: "Failed to call group")
            return NetworkResult.Error(null, e.message ?: "Failed to call group")
        }
    }


    // This function will check whether a call is ongoing or not
    // If a call is not ongoing, it means that this user has started the call,
    // in that case it'll send msg & notif to all group-members so that they know call is ongoing
    // else it will add the user to call ie. list of call participants
    suspend fun addUserToCall(groupData: GroupChatListingData, userData: User) {

        val groupId = groupData.groupId ?: ""
        val groupName = groupData.groupName ?: ""
        val userId = userData.userId ?: ""
        val userName = userData.userName
        val userProfileImgUrl = userData.userProfileImgUrl
        val agoraUserId = userData.agoraUserId

        val groupDoc = firestore.collection(Constants.DB.GROUPS).document(groupId)
        val callLog = groupDoc.collection(Constants.DB.GROUP_CALL_LOG)

        try {
            // Fetch latest ONGOING CALL (Only one call should be ongoing in a group at a time)
            val querySnapshot = callLog
                .whereEqualTo("status", Constants.CALL.STATUS.ONGOING)
                .limit(1)
                .get()
                .await()

            val ongoingCall: DocumentSnapshot? = when{
                querySnapshot.documents.isEmpty() -> null
                else -> querySnapshot.documents.firstOrNull()
            }

            val callDocRef: DocumentReference

            if (ongoingCall == null) {
                // No ongoing call, user is initiating the call, so we'll create call object and also send msg in group

                val listOfGroupMemberId = groupData.listOfUsers?.map { it.userId } ?: emptyList()

                // Sending msg in group
                CoroutineScope(Dispatchers.IO).launch {
                    sendMessage("Initiated a Call", groupId, userData, groupName,
                        listOfGroupMemberId, Constants.MSG_TYPE.CALL)
                }

                // Sending notif to group members
                CoroutineScope(Dispatchers.IO).launch {
                    triggerNotificationToGroupMembers(
                        groupId= groupId,
                        groupName= groupName, msgText = "Initiated a Call", senderId = userId,
                        msgType = Constants.MSG_TYPE.CALL, listOfGroupMemberId = listOfGroupMemberId)
                }

                // Creating Call object
                callDocRef = callLog.document() // Auto-generate document ID

                val newCall = Call(callDocRef.id, groupId, UtilityFunctions.getTime(), null,
                    userId, userName, Constants.CALL.STATUS.ONGOING, Constants.CALL.TYPE.VOICE, hashMapOf())

                callDocRef.set(newCall).await()
            } else {
                // Ongoing call found, get reference
                callDocRef = callLog.document(ongoingCall.id)
            }

            // Fetch existing participants
            val callData = callDocRef.get().await()
            val participants = callData.get("participants") as? MutableMap<String, Any> ?: mutableMapOf()

            if(participants.containsKey(userId)){
                Log.d("skt", "User already in call, he's trying to join from another phone!")
                return
            }

            // Add or update participant
            val newParticipant = CallParticipant(userId, userName,
                userProfileImgUrl, UtilityFunctions.getTime(), muteOn = false, speakerOn = false,agoraUserId
            )

            participants[userId] = newParticipant

            // Update Firestore with the modified participants map
            callDocRef.update("participants", participants).await()

            Log.d("CallRepository", "Participant added: $newParticipant")

        } catch (e: Exception) {
            Log.e("CallRepository", "Error adding user to call: ${e.message}")
        }
    }

    // Below func checks if last call msg in group-chat is "Initiated a Call"
    // or "Ended the call", based on that they will show different UI
    suspend fun getLastCallMsg(groupId: String): NetworkResult<Boolean> {
        return try {
              val lastMsg = messagesDao.getLastMsgOfGivenType(groupId, Constants.MSG_TYPE.CALL)

            lastMsg?.let { msg ->
                if (msg.message == INITIATED_A_CALL) {
                    NetworkResult.Success(true)
                } else NetworkResult.Success(false)
            } ?: NetworkResult.Success(false)
        } catch (e: Throwable) {
            Log.e("skt", "Error", e)
            NetworkResult.Error(null, e.localizedMessage ?: "Unknown error")
        }
    }




    suspend fun removeUserFromCall(groupData: GroupChatListingData, userData: User) = apiService.removeUserFromCall(groupData,userData)

    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun createGroup(data: GroupChatListingData,file: File) = apiService.createGroup(data,file)

    suspend fun getCompanyNameData(companyId: String) = apiService.getCompanyNameData(companyId)

    suspend fun getMultipleUsers(userIds: List<String>) = apiService.getMultipleUsers(userIds)

    suspend fun updateGroupProfile(groupId:String,groupName:String?, file: File?) = apiService.updateGroupProfile(groupId,groupName,file)

    suspend fun removeUserFromGroup(userId: String, groupId: String) = apiService.removeUserFromGroup(userId,groupId)

    suspend fun getGroupDetails(groupId: String) = apiService.getGroupDetails(groupId)

    suspend fun addGroupMember(groupId:String,userSet: MutableSet<String>) = apiService.addGroupMember(groupId,userSet)

}

