package com.adsperclick.media.views.user.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.repositories.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    fun register(user:User) {
        viewModelScope.launch(Dispatchers.IO){
            val result = communityRepository.register(user)
            _registrationLiveData.postValue(result)
        }
    }
}