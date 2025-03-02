package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.views.login.repository.AuthRepository
import com.adsperclick.media.views.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    val userLiveData :
            LiveData<NetworkResult<User>> get()  = chatRepository.userLiveData
    fun syncUser(){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.syncUser()
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

    val listOfGroupChatLiveData: LiveData<NetworkResult<List<GroupChatListingData>>>
        get() = chatRepository.listOfGroupChatLiveData

    fun startListeningToGroups(groupIds: List<String>){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.listenToGroupChatUpdates(groupIds)
        }
    }

    private val _groupId = MutableLiveData<String>()

    val messages: LiveData<List<Message>> = _groupId.switchMap { roomId ->
        chatRepository.getChatsForRoom(roomId)
    }

    fun setGroupId(roomId: String) {
        _groupId.value = roomId
    }


    fun fetchAllNewMessages(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.fetchAllNewMessages(groupId)
        }
    }

    fun stopRealtimeListening(){
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.stopRealtimeListening()
        }
    }

    fun sendMessage(text: String, groupId: String, currentUser: User) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(
                msgText = text,
                groupId = groupId,
                user = currentUser
            )
        }
    }

    val notificationsPager = chatRepository.getNotificationPager().flow.cachedIn(viewModelScope)

    private val _usersListLiveData = MutableLiveData<NetworkResult<List<User>>>()
    val usersListLiveData: LiveData<NetworkResult<List<User>>> = _usersListLiveData

    fun fetchGroupUsers(groupUsers: List<GroupUser>?) {
        viewModelScope.launch {
            _usersListLiveData.value = NetworkResult.Loading()

            if (groupUsers.isNullOrEmpty()) {
                _usersListLiveData.value = NetworkResult.Error(null,"No user IDs provided")
                return@launch
            }

            val userIds = groupUsers.map { it.userId }
            val result = chatRepository.getMultipleUsers(userIds)
            _usersListLiveData.value = result
        }
    }

    private val _updateGroupLiveData = MutableLiveData<NetworkResult<Boolean>>()
    val updateGroupLiveData: LiveData<NetworkResult<Boolean>> get() = _updateGroupLiveData

    fun updateGroupProfile(groupId:String,groupName:String?=null, file: File?=null){
        _updateGroupLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.updateGroupProfile(groupId,groupName,file)
                _updateGroupLiveData.postValue(result)
            }
        } catch (e : Exception){
            _updateGroupLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _leaveGroupResult = MutableLiveData<NetworkResult<String>>()
    val leaveGroupResult: LiveData<NetworkResult<String>> = _leaveGroupResult

    fun leaveGroup(userId: String, groupId: String) {
        _leaveGroupResult.postValue(NetworkResult.Loading())
        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.removeUserFromGroup(userId, groupId)
                _leaveGroupResult.postValue(result)
            }
        } catch (e : Exception){
            _leaveGroupResult.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _groupDetailResult = MutableLiveData<NetworkResult<GroupChatListingData>>()
    val groupDetailResult: LiveData<NetworkResult<GroupChatListingData>> = _groupDetailResult

    fun getGroupDetails(groupId: String) {
        _groupDetailResult.postValue(NetworkResult.Loading())
        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.getGroupDetails(groupId)
                _groupDetailResult.postValue(result)
            }
        } catch (e : Exception){
            _groupDetailResult.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

}