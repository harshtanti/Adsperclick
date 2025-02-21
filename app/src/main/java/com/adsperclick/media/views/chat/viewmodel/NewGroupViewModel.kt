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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewGroupViewModel@Inject constructor(private val chatRepository: ChatRepository) : ViewModel() {
    var selectedTabPosition = 0

    var selectedUserSet: MutableSet<String> = mutableSetOf()
    var serviceList:List<Service> = listOf()
    var selectedService: Service? = null
    var groupName: String? = null


    fun getUserListData(searchTxt:String,role:Int): Flow<PagingData<CommonData>> {
        return chatRepository.getUserListData(
            searchTxt,
            role
        ).cachedIn(viewModelScope)
    }

    private val _listServiceLiveData = MutableLiveData<NetworkResult<ArrayList<Service>>>()
    val listServiceLiveData: LiveData<NetworkResult<ArrayList<Service>>> get() = _listServiceLiveData

    fun getServiceList(){
        _listServiceLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.getServiceList()
                _listServiceLiveData.postValue(result)
            }
        } catch (e : Exception){
            _listServiceLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _createGroupLiveData = MutableLiveData<NetworkResult<GroupChatListingData>>()
    val createGroupLiveData: LiveData<NetworkResult<GroupChatListingData>> get() = _createGroupLiveData

    fun createGroup(groupData: GroupChatListingData){
        _createGroupLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = chatRepository.createGroup(groupData)
                _createGroupLiveData.postValue(result)
            }
        } catch (e : Exception){
            _createGroupLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    fun resetLiveData() {
        // Reset LiveData to default values
        _createGroupLiveData.postValue(NetworkResult.Loading())
        _listServiceLiveData.postValue(NetworkResult.Loading())

        // Reset the variables
        selectedUserSet.clear()
        serviceList = listOf()
        selectedService = null
        groupName = null

    }
}