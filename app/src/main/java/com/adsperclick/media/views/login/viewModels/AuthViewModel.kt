package com.adsperclick.media.views.login.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.data.repositories.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {


    private val _loginLiveData = MutableLiveData<NetworkResult<User>>()
    val loginLiveData: LiveData<NetworkResult<User>> get() = _loginLiveData
//    val registrationLiveData = authRepository.registrationLiveData

    private val _registrationLiveData = MutableLiveData<NetworkResult<User>>()
    val registrationLiveData: LiveData<NetworkResult<User>> get() = _registrationLiveData


    @Inject
    lateinit var tokenManager: TokenManager


    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO){
            val result = authRepository.login(email, password)
            _loginLiveData.postValue(result)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun signOut(){
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.signoutUser()
        }
    }
}