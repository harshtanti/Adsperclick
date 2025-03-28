package com.adsperclick.media.views.newGroup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.views.chat.repository.ChatRepository
import com.adsperclick.media.views.newGroup.repository.NewGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NewGroupViewModel@Inject constructor(private val newGroupRepository: NewGroupRepository) : ViewModel() {
    var selectedTabPosition = 0

    var selectedUserSet: MutableSet<String> = mutableSetOf()
    var selectedUserSetTotal: MutableSet<String> = mutableSetOf()
    var serviceList:List<Service> = listOf()
    var selectedService: Service? = null
    var groupName: String? = null
    var selectedImageFile: File? = null


    fun getUserListData(searchTxt:String="",role:Int): Flow<PagingData<CommonData>> {
        return newGroupRepository.getUserListData(
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
                val result = newGroupRepository.getServiceList()
                _listServiceLiveData.postValue(result)
            }
        } catch (e : Exception){
            _listServiceLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _createGroupLiveData = MutableLiveData<NetworkResult<Boolean>>()
    val createGroupLiveData: LiveData<NetworkResult<Boolean>> get() = _createGroupLiveData

    fun createGroup(groupData: GroupChatListingData,file: File){
        _createGroupLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = newGroupRepository.createGroup(groupData,file)
                _createGroupLiveData.postValue(result)
            }
        } catch (e : Exception){
            _createGroupLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _companyDataLiveData = MutableLiveData<NetworkResult<Company>>()
    val companyDataLiveData: LiveData<NetworkResult<Company>> get() = _companyDataLiveData

    fun getCompanyNameData(companyId: String?=null) {
        _companyDataLiveData.postValue(NetworkResult.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (companyId.isNullOrEmpty()) {
                    return@launch
                }
                val result = newGroupRepository.getCompanyNameData(companyId)
                _companyDataLiveData.postValue(result)
            } catch (e: Exception) {
                _companyDataLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
            }
        }
    }

    private val _addGroupMemberLiveData = MutableLiveData<NetworkResult<Boolean>>()
    val addGroupMemberLiveData: LiveData<NetworkResult<Boolean>> get() = _addGroupMemberLiveData

    fun addGroupMember(groupId:String,userSet: MutableSet<String>){
        _addGroupMemberLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = newGroupRepository.addGroupMember(groupId,userSet)
                _addGroupMemberLiveData.postValue(result)
            }
        } catch (e : Exception){
            _addGroupMemberLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        _createGroupLiveData.postValue(NetworkResult.Loading())
        _listServiceLiveData.postValue(NetworkResult.Loading())
        _companyDataLiveData.postValue(NetworkResult.Loading())

        // Reset the variables
        selectedUserSet.clear()
        serviceList = listOf()
        selectedService = null
        groupName = null
    }
}