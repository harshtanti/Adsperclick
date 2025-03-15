package com.adsperclick.media.views.call.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.call.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    // LiveData for UI components
    private val _participants = MutableLiveData<List<CallParticipant>>(emptyList())
    val participants: LiveData<List<CallParticipant>> = _participants

    private val _activeCall = MutableLiveData<Call?>(null)
    val activeCall: LiveData<Call?> = _activeCall

    private val _callHistory = MutableLiveData<List<Call>>(emptyList())
    val callHistory: LiveData<List<Call>> = _callHistory

    // StateFlow for loading/error states
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    // Current call ID
    private var currentCallId: String? = null

    // Get temporary Agora token for development
    fun getAgoraToken(): String {
        return Constants.TEMP_AGORA_TOKEN
    }

    // Check for active call in a group
    fun checkActiveCall(groupId: String) {
        _callState.value = CallState.Loading
        viewModelScope.launch {
            when (val result = callRepository.getActiveCallInGroup(groupId)) {
                is NetworkResult.Success -> {
                    val call = result.data
                    _activeCall.postValue(call)
                    call?.let {
                        currentCallId = it.callId
                        observeCallParticipants(it.callId)
                    }
                    _callState.value = CallState.Success
                }
                is NetworkResult.Error -> {
                    _callState.value = CallState.Error(result.message ?: "Failed to check active call")
                }
            }
        }
    }

    // Start call
    fun startCall(groupId: String) {
        _callState.value = CallState.Loading
        viewModelScope.launch {
            when (val result = callRepository.startCall(groupId)) {
                is NetworkResult.Success -> {
                    val call = result.data
                    if (call != null) {
                        _activeCall.postValue(call)
                        currentCallId = call.callId
                        observeCallParticipants(call.callId)
                        _callState.value = CallState.Success
                    } else {
                        _callState.value = CallState.Error("Failed to create call")
                    }
                }
                is NetworkResult.Error -> {
                    _callState.value = CallState.Error(result.message ?: "Failed to start call")
                }
            }
        }
    }

    // Join call
    fun joinCall(callId: String) {
        _callState.value = CallState.Loading
        viewModelScope.launch {
            when (val result = callRepository.joinCall(callId)) {
                is NetworkResult.Success -> {
                    currentCallId = callId
                    observeCallParticipants(callId)
                    observeCallDetails(callId)
                    _callState.value = CallState.Success
                }
                is NetworkResult.Error -> {
                    _callState.value = CallState.Error(result.message ?: "Failed to join call")
                }
            }
        }
    }

    // Leave call
    fun leaveCall() {
        val callId = currentCallId ?: return
        _callState.value = CallState.Loading
        viewModelScope.launch {
            when (val result = callRepository.leaveCall(callId)) {
                is NetworkResult.Success -> {
                    currentCallId = null
                    _activeCall.postValue(null)
                    _participants.postValue(emptyList())
                    _callState.value = CallState.Success
                }
                is NetworkResult.Error -> {
                    _callState.value = CallState.Error(result.message ?: "Failed to leave call")
                }
            }
        }
    }

    // End call
    fun endCall() {
        val callId = currentCallId ?: return
        _callState.value = CallState.Loading
        viewModelScope.launch {
            when (val result = callRepository.endCall(callId)) {
                is NetworkResult.Success -> {
                    currentCallId = null
                    _activeCall.postValue(null)
                    _participants.postValue(emptyList())
                    _callState.value = CallState.Success
                }
                is NetworkResult.Error -> {
                    _callState.value = CallState.Error(result.message ?: "Failed to end call")
                }
            }
        }
    }

    // Update mute status
    fun updateMuteStatus(isMuted: Boolean) {
        val callId = currentCallId ?: return
        viewModelScope.launch {
            callRepository.updateParticipantMuteStatus(callId, isMuted)
        }
    }

    // Get call history
    fun getCallHistory(groupId: String) {
        viewModelScope.launch {
            when (val result = callRepository.getCallHistory(groupId)) {
                is NetworkResult.Success -> {
                    _callHistory.postValue(result.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    // Handle error
                }
            }
        }
    }

    // Observe call participants in real-time
    private fun observeCallParticipants(callId: String) {
        viewModelScope.launch {
            callRepository.observeCallParticipants(callId).collect { participantsList ->
                _participants.postValue(participantsList)
            }
        }
    }

    // Observe call details in real-time
    private fun observeCallDetails(callId: String) {
        viewModelScope.launch {
            callRepository.observeCall(callId).collect { call ->
                _activeCall.postValue(call)
                if (call?.status != "active") {
                    currentCallId = null
                    _callState.value = CallState.Idle
                }
            }
        }
    }
}

sealed class CallState {
    object Idle : CallState()
    object Loading : CallState()
    object Success : CallState()
    data class Error(val message: String) : CallState()
}