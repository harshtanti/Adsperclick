package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.ENDED_THE_CALL
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
import com.adsperclick.media.utils.Constants.LIMIT_MSGS
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.login.repository.AuthRepository
import com.adsperclick.media.views.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) :ViewModel() {

    var lastTimeWhenNotificationsWereLoaded:Long = 0L

    val createNotificationLiveData :
            LiveData<NetworkResult<NotificationMsg>> get()  = chatRepository.createNotificationLiveData

    fun createNotification(notification : NotificationMsg){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.createNotification(notification)
        }
    }


    private val _userLiveData = MutableLiveData<ConsumableValue<NetworkResult<User>>>()
    val userLiveData: LiveData<ConsumableValue<NetworkResult<User>>> get() = _userLiveData

// This function will run only once the app is launched, it syncs user with server object
// to know things like user is not blocked, fetch latest groups user is added to, etc..
    // We also sync device time with server time (See function to know how)
    // We also check if current app version is supported or not
    // These 3 things are checked in parallel concurretly saving time and later when all 3 results
    // come, we post result to UI as per requirements
    fun syncUser(){
        viewModelScope.launch (Dispatchers.IO){
            val syncUserJob = async { chatRepository.syncUser()}
            val syncTimeJob = async { chatRepository.syncDeviceTime()}
            val isAcceptableVersionJob = async{ chatRepository.isCurrentVersionAcceptable() }
            val animationPlayTimer = async{ delay(2100) }

            val userObjectFromBackend = syncUserJob.await()
            val isAcceptableVersion = isAcceptableVersionJob.await()
            syncTimeJob.await()
            animationPlayTimer.await()      // To make sure animation is also completed before we post result

            if(isAcceptableVersion.not()){
                _userLiveData.postValue(ConsumableValue
                    (NetworkResult.Error(null, "Update Required to use this App")))
                return@launch
            }

            _userLiveData.postValue(userObjectFromBackend)
        }
    }





    fun updateLastNotificationSeenTime(){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.updateLastNotificationSeenTime()
        }
    }

    fun signOut(){
        viewModelScope.launch (Dispatchers.IO){
            authRepository.signoutUser()
        }
    }

    val lastSeenForEachUserEachGroupLiveData get() = chatRepository.lastSeenForEachUserEachGroupLiveData

    val listOfGroupChatLiveData: LiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>>
        get() = chatRepository.listOfGroupChatLiveData

    fun startListeningToGroups(groupIds: List<String>){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.listenToGroupChatUpdates(groupIds)
        }
    }

    private val _groupId = MutableLiveData<String>()

    val messages: LiveData<ConsumableValue<List<Message>>> = _groupId.switchMap { groupId ->
        chatRepository.getChatsForGroup(groupId).map { messageList ->
            ConsumableValue(messageList)
        }
    }

    fun setGroupId(roomId: String) {
        _groupId.value = roomId
    }

    val lastSeenTimestampLiveData  = chatRepository.lastSeenTimestampLiveData
    fun fetchAllNewMessages(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.fetchAllNewMessages(groupId)
        }
    }


//    private val _msgsLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<Message>>>>()
//    val msgsLiveData: LiveData<ConsumableValue<NetworkResult<List<Message>>>> get() = _msgsLiveData
//    private fun getOffset(pageNo:Int): Int = /*(pageNo* LIMIT_MSGS)-(LIMIT_MSGS/2)*/ pageNo*(LIMIT_MSGS/2)
//
//    fun getSpecifiedMessages(groupId: String, pageNo:Int){
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = chatRepository.getSpecifiedMessages(groupId, LIMIT_MSGS, getOffset(pageNo))
//            _msgsLiveData.postValue(ConsumableValue(result))
//        }
//    }


    fun stopRealtimeListening(){
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.stopRealtimeListening()
        }
    }

    fun sendMessage(text: String, groupId: String, currentUser: User, groupName: String,
                    listOfGroupMemberId: List<String>, msgType:Int = Constants.MSG_TYPE.TEXT) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(
                msgText = text,
                groupId = groupId,
                user = currentUser,
                groupName = groupName,
                listOfGroupMemberId = listOfGroupMemberId,
                msgType = msgType
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            currentUser.userId?.let {
                chatRepository.triggerNotificationToGroupMembers(groupId= groupId,
                    groupName= groupName, msgText = text, senderId = it,
                    msgType = msgType, listOfGroupMemberId = listOfGroupMemberId)
            }
        }
    }

    val notificationsPager = chatRepository.getNotificationPager().flow.cachedIn(viewModelScope)

    private val _usersListLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<User>>>>()
    val usersListLiveData: LiveData<ConsumableValue<NetworkResult<List<User>>>> = _usersListLiveData

    fun fetchGroupUsers(groupUsers: List<GroupUser>?) {
        _usersListLiveData.postValue(ConsumableValue(NetworkResult.Loading()))
        try {
            viewModelScope.launch {

                if (groupUsers.isNullOrEmpty()) {
                    _usersListLiveData.postValue(ConsumableValue(NetworkResult.Error(null,"No user IDs provided")))
                    return@launch
                }

                val userIds = groupUsers.map { it.userId }
                val result = chatRepository.getMultipleUsers(userIds)
                _usersListLiveData.postValue(ConsumableValue(result))
            }
        } catch (e:Exception){
            _usersListLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }

    }

    private val _updateGroupLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val updateGroupLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> get() = _updateGroupLiveData

    fun updateGroupProfile(groupId:String,groupName:String?=null, file: File?=null){
        _updateGroupLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.updateGroupProfile(groupId,groupName,file)
                _updateGroupLiveData.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _updateGroupLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }

    private val _leaveGroupResult = MutableLiveData<ConsumableValue<NetworkResult<String>>>()
    val leaveGroupResult: LiveData<ConsumableValue<NetworkResult<String>>> = _leaveGroupResult

    fun leaveGroup(userId: String, groupId: String) {
        _leaveGroupResult.postValue(ConsumableValue(NetworkResult.Loading()))
        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.removeUserFromGroup(userId, groupId)
                _leaveGroupResult.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _leaveGroupResult.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }

    private val _groupDetailResult = MutableLiveData<ConsumableValue<NetworkResult<GroupChatListingData>>>()
    val groupDetailResult: LiveData<ConsumableValue<NetworkResult<GroupChatListingData>>> = _groupDetailResult

    fun getGroupDetails(groupId: String) {
        _groupDetailResult.postValue(ConsumableValue(NetworkResult.Loading()))
        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.getGroupDetails(groupId)
                _groupDetailResult.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _groupDetailResult.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }

    fun updateLastReadMsg(groupId: String, lastReadMsgId: String, currentUserId:String, listOfUsers : List<GroupUser>) {
        viewModelScope.launch (Dispatchers.IO){
//            val updatedUserList = listOfUsers.map { member ->
//                if (member.userId == currentUserId) {
//                    member.copy(lastSeenMsgId = lastReadMsgId)
//                } else {
//                    member
//                }
//            }
            chatRepository.updateLastReadMessage(groupId, currentUserId)
        }
    }


    val imageUploadedLiveData get() = chatRepository.imageUploadedLiveData
    fun uploadFile(groupId: String, groupName: String, file: File?,
                   listOfUsers: List<String>, msgType: Int){
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.uploadFile(groupId, groupName, file, listOfUsers, msgType)
        }
    }

    private val _getAgoraTokenLiveData = MutableLiveData<ConsumableValue<NetworkResult<String>>>()
    val getAgoraTokenLiveData: LiveData<ConsumableValue<NetworkResult<String>>> = _getAgoraTokenLiveData

    fun getAgoraCallToken(groupData : GroupChatListingData, userData : User){

        viewModelScope.launch(Dispatchers.IO) {
            _getAgoraTokenLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

            // Prepare the data to send to the Cloud Function
            val data = hashMapOf(
                "groupId" to (groupData.groupId ?: ""),
                "groupName" to (groupData.groupName ?: ""),
                "agoraUserId" to (userData.agoraUserId ?: "").toString(),
                "userId" to (userData.userId ?: "")
            )

            launch {
                chatRepository.addUserToCall(groupData, userData)
            }

            val result = chatRepository.getAgoraCallToken(data)
            _getAgoraTokenLiveData.postValue(ConsumableValue(result))
        }
    }

    private val _userLeftCallLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val userLeftCallLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> = _userLeftCallLiveData
    fun LeaveCall(groupData: GroupChatListingData, userData: User){

        viewModelScope.launch(Dispatchers.IO) {
            val result = chatRepository.removeUserFromCall(groupData, userData)
            _userLeftCallLiveData.postValue(ConsumableValue(result))
        }
    }

    private val _isCallOngoingLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val isCallOngoingLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> = _isCallOngoingLiveData
    fun isCallOngoing(groupId: String){

        viewModelScope.launch(Dispatchers.IO) {
            val result = chatRepository.getLastCallMsg(groupId)
            _isCallOngoingLiveData.postValue(ConsumableValue(result))
        }
    }

    fun checkIfLastMsgRelatedToCall(message: Message?){
        viewModelScope.launch(Dispatchers.IO) {
            message?.let { msg->
                if(msg.msgType == Constants.MSG_TYPE.CALL){
                    when(msg.message){
                        ENDED_THE_CALL-> {
                            _isCallOngoingLiveData.postValue(ConsumableValue(NetworkResult.Success(false)))
                        }
                        INITIATED_A_CALL-> {
                            _isCallOngoingLiveData.postValue(ConsumableValue(NetworkResult.Success(true)))
                        }
                    }
                }
            }
        }
    }

}

