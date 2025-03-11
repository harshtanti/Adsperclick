package com.adsperclick.media.views.user.viewmodel

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
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.user.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val communityRepository: CommunityRepository) :ViewModel() {
    var selectedTabPosition = 0
    var firstName:String?=null
    var lastName:String?=null
    var companyName:String?=null
    var gstNumber:String?=null
    var aadharNumber:String?=null
    var email:String?=null
    var password:String?=null
    var confirmPassword:String?=null
    var services:String?=null
    var serviceName:String?=null
    var serviceList:List<CommonData> = listOf()
    var selectServiceList = arrayListOf<CommonData>()
    var companyList:List<Company> = listOf()
    var selectedCompany:Company?=null

    private val _registerCompanyLiveData = MutableLiveData<NetworkResult<Company>>()
    val registerCompanyLiveData: LiveData<NetworkResult<Company>> get() = _registerCompanyLiveData

    fun registerCompany(company: Company) {
        viewModelScope.launch(Dispatchers.IO){
            val result = communityRepository.registerCompany(company)
            _registerCompanyLiveData.postValue(result)
        }
    }

    private val _registerServiceLiveData = MutableLiveData<NetworkResult<Service>>()
    val registerServiceLiveData: LiveData<NetworkResult<Service>> get() = _registerServiceLiveData

    fun registerService(service: Service) {
        viewModelScope.launch(Dispatchers.IO){
            val result = communityRepository.registerService(service)
            _registerServiceLiveData.postValue(result)
        }
    }

    private val _registrationLiveData = MutableLiveData<NetworkResult<User>>()
    val registrationLiveData: LiveData<NetworkResult<User>> get() = _registrationLiveData

    fun registerUser(user:User) {
        viewModelScope.launch(Dispatchers.IO){
            val result = communityRepository.registerUser(user)
            _registrationLiveData.postValue(result)
        }
    }

    private val _listServiceLiveData = MutableLiveData<NetworkResult<ArrayList<Service>>>()
    val listServiceLiveData: LiveData<NetworkResult<ArrayList<Service>>> get() = _listServiceLiveData

    fun getServiceList(){
        _listServiceLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = communityRepository.getServiceList()
                _listServiceLiveData.postValue(result)
            }
        } catch (e : Exception){
            _listServiceLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _listCompanyLiveData = MutableLiveData<NetworkResult<ArrayList<Company>>>()
    val listCompanyLiveData: LiveData<NetworkResult<ArrayList<Company>>> get() = _listCompanyLiveData

    fun getCompanyList(){
        _listCompanyLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = communityRepository.getCompanyList()
                _listCompanyLiveData.postValue(result)
            }
        } catch (e : Exception){
            _listCompanyLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _deleteServiceLiveData = MutableLiveData<NetworkResult<Boolean>>()

    fun deleteService(serviceId: String): LiveData<NetworkResult<Boolean>> {
        _deleteServiceLiveData.value = NetworkResult.Loading()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = communityRepository.deleteService(serviceId)
                _deleteServiceLiveData.postValue(result)
            } catch (e: Exception) {
                _deleteServiceLiveData.postValue(NetworkResult.Error(false, e.message ?: "Error deleting service"))
            }
        }

        return _deleteServiceLiveData
    }

    private val _updateUserLiveData = MutableLiveData<NetworkResult<Boolean>>()
    val updateUserLiveData: LiveData<NetworkResult<Boolean>> get() = _updateUserLiveData

    fun updateUser(userId:String,phoneNumber:String?=null, file: File?=null){
        _updateUserLiveData.postValue(NetworkResult.Loading())

        try {
            viewModelScope.launch(Dispatchers.IO){
                val result = communityRepository.updateUser(userId,phoneNumber,file)
                _updateUserLiveData.postValue(result)
            }
        } catch (e : Exception){
            _updateUserLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
        }
    }

    private val _userDataLiveData = MutableLiveData<NetworkResult<User>>()
    val userDataLiveData: LiveData<NetworkResult<User>> get() = _userDataLiveData

    fun getUserData(userId: String): LiveData<NetworkResult<User>> {
        _userDataLiveData.postValue(NetworkResult.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = communityRepository.getUserData(userId)
                _userDataLiveData.postValue(result)
            } catch (e: Exception) {
                _userDataLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
            }
        }

        return _userDataLiveData
    }

    private val _companyDataLiveData = MutableLiveData<NetworkResult<Company>>()
    val companyDataLiveData: LiveData<NetworkResult<Company>> get() = _companyDataLiveData

    fun getCompanyData(companyId: String): LiveData<NetworkResult<Company>> {
        _companyDataLiveData.postValue(NetworkResult.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = communityRepository.getCompanyData(companyId)
                _companyDataLiveData.postValue(result)
            } catch (e: Exception) {
                _companyDataLiveData.postValue(NetworkResult.Error(null, "Error ${e.message}"))
            }
        }

        return _companyDataLiveData
    }

    private val _updateCompanyServicesLiveData = MutableLiveData<NetworkResult<Boolean>>()
    val updateCompanyServicesLiveData: LiveData<NetworkResult<Boolean>> get() = _updateCompanyServicesLiveData

    fun updateCompanyServices(companyId: String, servicesList: List<Service>) {
        _updateCompanyServicesLiveData.postValue(NetworkResult.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert CommonData to Service objects

                val result = communityRepository.updateCompanyServices(companyId, servicesList)
                _updateCompanyServicesLiveData.postValue(result)
            } catch (e: Exception) {
                _updateCompanyServicesLiveData.postValue(
                    NetworkResult.Error(false, "Error updating company services: ${e.message}")
                )
            }
        }
    }

    val userBlockedStatusLiveData: LiveData<ConsumableValue<NetworkResult<Boolean>>> get() = communityRepository.userBlockedStatusLiveData

    fun changeUserBlockedStatus(shouldBlock:Boolean, userId: String){
        viewModelScope.launch(Dispatchers.IO) {
            communityRepository.changeUserBlockedStatus(shouldBlock, userId)
        }
    }

    fun getUserListData(searchTxt:String = "",role:Int): Flow<PagingData<CommonData>> {
        return communityRepository.getUserListData(
            searchTxt,
            role
        ).cachedIn(viewModelScope)
    }

    fun getCompanyListData(searchTxt:String = ""): Flow<PagingData<CommonData>> {
        return communityRepository.getCompanyListData(
            searchTxt
        ).cachedIn(viewModelScope)
    }

    fun getServiceListData(searchTxt:String = ""): Flow<PagingData<CommonData>> {
        return communityRepository.getServiceListData(
            searchTxt
        ).cachedIn(viewModelScope)
    }
}