package com.adsperclick.media.views.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.repositories.ChatRepository
import com.adsperclick.media.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(private val chatRepository: ChatRepository) :ViewModel() {

//    private val _createNotificationLiveData = MutableLiveData<NetworkResult<NotificationMsg>>()
//    val createNotificationLiveData: LiveData<NetworkResult<NotificationMsg>> get() = _createNotificationLiveData

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

    val notificationsPager = chatRepository.getNotificationPager().flow.cachedIn(viewModelScope)

}