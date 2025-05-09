package com.adsperclick.media.views.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.views.chat.repository.ChatRepository
import com.adsperclick.media.views.login.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {


    var lastTimeWhenNotificationsWereLoaded:Long = 0L

    val createNotificationLiveData :
            LiveData<NetworkResult<NotificationMsg>>
        get()  = notificationRepository.createNotificationLiveData

    fun createNotification(notification : NotificationMsg){
        viewModelScope.launch (Dispatchers.IO){
            notificationRepository.createNotification(notification)
        }
    }

    fun updateLastNotificationSeenTime(userId: String){
        viewModelScope.launch (Dispatchers.IO){
            notificationRepository.updateLastNotificationSeenTime(userId)
        }
    }

    fun getNotificationsPager(userRole: Int?) =
        notificationRepository.getNotificationPager(userRole).flow.cachedIn(viewModelScope)

}