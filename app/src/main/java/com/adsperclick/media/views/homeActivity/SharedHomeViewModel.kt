package com.adsperclick.media.views.homeActivity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants.LIMIT_MSGS
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedHomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository
): ViewModel()  {

    var userData : User?= null

    var idOfGroupToOpen : String?=null

    var lastSeenTimeForEachUserEachGroup : Map<String, MutableMap<String, Long?>>? =null



    private val _msgsLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<Message>>>>()
    val msgsLiveData: LiveData<ConsumableValue<NetworkResult<List<Message>>>> get() = _msgsLiveData
    private fun getOffset(pageNo:Int): Int = pageNo*(LIMIT_MSGS /2)
    var pageNo :Int?= null

    fun getSpecifiedMessages(groupId: String){
        viewModelScope.launch(Dispatchers.IO) {
            val result = chatRepository.getSpecifiedMessages(groupId, LIMIT_MSGS, getOffset(pageNo ?: 1))
            _msgsLiveData.postValue(ConsumableValue(result))
        }
    }

    var lastScrollOffset:Int = -1
    var lastScrollPosition:Int = -1

    // Call this when navigating away from MessagingFragment
    fun saveScrollPosition(position: Int, offset: Int) {
        lastScrollPosition = position
        lastScrollOffset = offset
    }

}

