package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.repositories.AuthRepository
import com.adsperclick.media.data.repositories.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(private val chatRepository: ChatRepository,
                                       private val authRepository: AuthRepository) :ViewModel() {

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


    val notificationsPager = chatRepository.getNotificationPager().flow.cachedIn(viewModelScope)

}