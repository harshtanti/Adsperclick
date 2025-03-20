package com.adsperclick.media.views.call.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.call.repository.CallRepository
import com.adsperclick.media.views.chat.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    var joinedUsers: MutableList<CallParticipant> = mutableListOf()
    val speakingMap = mutableMapOf<Int, Boolean>()

    private val _userCallTokenLiveData = MutableLiveData<ConsumableValue<NetworkResult<Pair<String,String>>>>()
    val userCallTokenLiveData: LiveData<ConsumableValue<NetworkResult<Pair<String,String>>>> get() = _userCallTokenLiveData

    fun getUserCallToken(groupId:String){
        _userCallTokenLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = callRepository.getUserCallToken(groupId)
                _userCallTokenLiveData.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _userCallTokenLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }

    suspend fun getCallParticipantsUpdates(groupId: String): Flow<NetworkResult<Call>> {
        return callRepository.listenParticipantChanges(groupId)
            .flowOn(Dispatchers.IO)
    }

    private val _removeUserLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val removeUserLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> get() = _removeUserLiveData

    fun removeUser(groupData: GroupChatListingData, userData : User){
        _removeUserLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = callRepository.removeUserFromCall(groupData, userData)
                _removeUserLiveData.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _removeUserLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }

    private val _updateUserCallStatusLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val updateUserCallStatusLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> get() = _updateUserCallStatusLiveData

    fun updateUserCallStatus(groupId: String, userId: String, isMuted: Boolean? = null, isSpeaking: Boolean? = null){
        _updateUserCallStatusLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = callRepository.updateUserCallStatus(groupId, userId, isMuted, isSpeaking)
                _updateUserCallStatusLiveData.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _updateUserCallStatusLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }


}