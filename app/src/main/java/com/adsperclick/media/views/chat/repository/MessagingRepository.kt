package com.adsperclick.media.views.chat.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.di.VersionProvider
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class MessagingRepository  @Inject constructor(
    private val firestore: FirebaseFirestore
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
    lateinit var context: Context



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

    fun getSpecifiedMessages(groupId: String, limit:Int, offset:Int): NetworkResult<List<Message>> {

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



    suspend fun getAgoraCallToken(data : HashMap<String,String>): NetworkResult<String> {
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
}


