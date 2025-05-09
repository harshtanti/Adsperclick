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
import com.adsperclick.media.views.chat.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


// This view model is shared across all fragments of the "HomeActivity"
// It is considered good practice to share view models across fragments for passing data,
// For complex data classes using "arguments" can be cumbersome, so using shared view model is fun
// Also bringing back data from "tokenManager" requires more performance overhead than storing them
//in shared VM once and accessing later from there, like in our case, in HomeActivity I took data
// from shared prefs and saved it in shared VM. Then accessing it from there everytime
// Also when in messaging fragment, and moving to MediaPreviewFragment then coming back, we need to
// save state of at what position fragment was when we left, and come back to the same position

@HiltViewModel
class SharedHomeViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository
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
            val result = messagingRepository.getSpecifiedMessages(groupId, LIMIT_MSGS, getOffset(pageNo ?: 1))
            _msgsLiveData.postValue(ConsumableValue(result))
        }
    }

    var lastScrollOffset:Int = -1
    var lastScrollPosition:Int = -1

    // Call this when navigating away from MessagingFragment, to save state of where recycler view was,
    // so that when user comes back again recycler view is at exact same pos,
    // (coming from Media to Msg frag)
    fun saveScrollPosition(position: Int, offset: Int) {
        lastScrollPosition = position
        lastScrollOffset = offset
    }

}

