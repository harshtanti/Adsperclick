package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.login.repository.AuthRepository
import com.adsperclick.media.views.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) :ViewModel() {


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
            // The "Job" functions below are used to run the functions in parallel
            val syncUserJob = async { chatRepository.syncUser()}
            val syncTimeJob = async { chatRepository.syncDeviceTime()}
            val isAcceptableVersionJob = async{ chatRepository.isCurrentVersionAcceptable() }
//            val animationPlayTimer = async{ delay(2100) }

            val userObjectFromBackend = syncUserJob.await()
            val isAcceptableVersion = isAcceptableVersionJob.await()
            syncTimeJob.await()
//            animationPlayTimer.await()      // To make sure animation is also completed before we post result

            if(isAcceptableVersion.not()){
                _userLiveData.postValue(ConsumableValue
                    (NetworkResult.Error(null, "Update Required to use this App")))
                return@launch
            }

            _userLiveData.postValue(userObjectFromBackend)
        }
    }



    fun signOut(){
        viewModelScope.launch (Dispatchers.IO){
            authRepository.signoutUser()
        }
    }

    val listOfGroupChatLiveData: LiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>>
        get() = chatRepository.listOfGroupChatLiveData

    fun startListeningToGroups(groupIds: List<String>){
        viewModelScope.launch (Dispatchers.IO){
            chatRepository.listenToGroupChatUpdates(groupIds)
        }
    }

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

}

