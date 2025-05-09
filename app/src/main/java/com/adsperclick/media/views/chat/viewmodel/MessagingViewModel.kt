package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.ENDED_THE_CALL
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.chat.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository
) : ViewModel(){


    private val _groupId = MutableLiveData<String>()

    val messages: LiveData<ConsumableValue<List<Message>>> = _groupId.switchMap { groupId ->
        messagingRepository.getChatsForGroup(groupId).map { messageList ->
            ConsumableValue(messageList)
        }
    }

    fun setGroupId(roomId: String) {
        _groupId.value = roomId
    }


    // val lastSeenTimestampLiveData  = chatRepository.lastSeenTimestampLiveData
    fun fetchAllNewMessages(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messagingRepository.fetchAllNewMessages(groupId)
        }
    }



    private val _isCallOngoingLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val isCallOngoingLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> = _isCallOngoingLiveData
    fun isCallOngoing(groupId: String){

        viewModelScope.launch(Dispatchers.IO) {
            val result = messagingRepository.getLastCallMsg(groupId)
            _isCallOngoingLiveData.postValue(ConsumableValue(result))
        }
    }

    val imageUploadedLiveData get() = messagingRepository.imageUploadedLiveData
    fun uploadFile(groupId: String, groupName: String, file: File?,
                   listOfUsers: List<String>, msgType: Int){
        viewModelScope.launch(Dispatchers.IO) {
            messagingRepository.uploadFile(groupId, groupName, file, listOfUsers, msgType)
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
                // This function will run asynchronously/ separately, to make
                messagingRepository.addUserToCall(groupData, userData)
            }

            val result = messagingRepository.getAgoraCallToken(data)
            _getAgoraTokenLiveData.postValue(ConsumableValue(result))
        }
    }

//    private val _userLeftCallLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
//    val userLeftCallLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> = _userLeftCallLiveData
//    fun LeaveCall(groupData: GroupChatListingData, userData: User){
//
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = chatRepository.removeUserFromCall(groupData, userData)
//            _userLeftCallLiveData.postValue(ConsumableValue(result))
//        }
//    }


    fun updateLastReadMsg(groupId: String,  currentUserId:String) {
        viewModelScope.launch (Dispatchers.IO){
//            val updatedUserList = listOfUsers.map { member ->
//                if (member.userId == currentUserId) {
//                    member.copy(lastSeenMsgId = lastReadMsgId)
//                } else {
//                    member
//                }
//            }
            messagingRepository.updateLastReadMessage(groupId, currentUserId)
        }
    }


    fun sendMessage(text: String, groupId: String, currentUser: User, groupName: String,
                    listOfGroupMemberId: List<String>, msgType:Int = Constants.MSG_TYPE.TEXT) {
        viewModelScope.launch(Dispatchers.IO) {
            messagingRepository.sendMessage(
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
                messagingRepository.triggerNotificationToGroupMembers(groupId= groupId,
                    groupName= groupName, msgText = text, senderId = it,
                    msgType = msgType, listOfGroupMemberId = listOfGroupMemberId)
            }
        }
    }


    fun stopRealtimeListening(){
        viewModelScope.launch(Dispatchers.IO) {
            messagingRepository.stopRealtimeListening()
        }
    }

    fun checkIfLastMsgRelatedToCall(message: Message?){
        viewModelScope.launch(Dispatchers.IO) {
            message?.let { msg->
                if(msg.msgType == Constants.MSG_TYPE.CALL){
                    when(msg.message){
                        ENDED_THE_CALL -> {
                            _isCallOngoingLiveData.postValue(ConsumableValue(NetworkResult.Success(false)))
                        }
                        INITIATED_A_CALL -> {
                            _isCallOngoingLiveData.postValue(ConsumableValue(NetworkResult.Success(true)))
                        }
                    }
                }
            }
        }
    }

}

