package com.adsperclick.media.views.setting.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.setting.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(private val settingRepository: SettingRepository) : ViewModel() {

    private val _updateUserLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val updateUserLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> get() = _updateUserLiveData

    fun updateUser(userId:String,phoneNumber:String?=null, file: File?=null){
        _updateUserLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = settingRepository.updateUser(userId,phoneNumber,file)
                _updateUserLiveData.postValue(ConsumableValue(result))
            }
        } catch (e : Exception){
            _updateUserLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${e.message}")))
        }
    }
}