package com.adsperclick.media.views.login.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.chat.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(private val apiService: ApiService, private val chatRepo : ChatRepository) {

    @Inject
    lateinit var firebaseAuth : FirebaseAuth

    @Inject
    lateinit var firebaseDb : FirebaseFirestore

    @Inject
    lateinit var fcm : FirebaseMessaging

    @Inject
    lateinit var tokenManager : TokenManager

    @Inject
    lateinit var messagesDao : MessagesDao


    private val _loginLiveData = MutableLiveData<NetworkResult<User>>()
    val loginLiveData: LiveData<NetworkResult<User>> get() = _loginLiveData


    val _userStateLiveData = MutableLiveData<NetworkResult<User>>()
//    val userStateLiveData : LiveData<NetworkResult<FirebaseUser>> get() = _userStateLiveData

//    private val _userApiResponseLiveData = MutableLiveData<Event<NetworkResult<UserAndToken>>>()

    suspend fun login(email: String, password: String): NetworkResult<User> {
        _loginLiveData.postValue(NetworkResult.Loading())

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return NetworkResult.Error(null, "User authentication failed")

            val snapshot = firebaseDb.collection(Constants.DB.USERS)
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java) ?: return NetworkResult.Error(null, "User data missing")

            if(user.blocked == true) return NetworkResult.Error(null, "You have been blocked!")

            if(user.listOfGroupsAssigned.isNullOrEmpty()) return NetworkResult.Error(null, "You are not part of any group!")

            // Updating device token for FCM, on db
            val fcmDeviceToken = try {
                FirebaseMessaging.getInstance().token.await()  // Get FCM token
            } catch (e: Exception) {
                Log.e("FCM_Service", "Error getting FCM token: ${e.message}")
                null  // Handle token retrieval failure, otherwise device would crash
            }
            Log.d("FCM_Service", "FCM token: $fcmDeviceToken")
            if (!fcmDeviceToken.isNullOrBlank()) {
                tokenManager.saveFcmToken(fcmDeviceToken)
                val tokenList = user.fcmTokenListOfDevices?.toMutableSet() ?: mutableSetOf()
                if (!tokenList.contains(fcmDeviceToken)) {      // If this device token is not saved in user's token list, we'll add it
                    tokenList.add(fcmDeviceToken)
                    firebaseDb.collection(Constants.DB.USERS)
                        .document(firebaseUser.uid)
                        .update("fcmTokenListOfDevices", tokenList.toList())
                        .await()
                    user.fcmTokenListOfDevices = tokenList.toList()
                }
            }

            user.listOfGroupsAssigned.let {
                chatRepo.fetchLastSeenTimeForEachUserInEachGroup(it)
            }

            // Saving user in shared preferences
            tokenManager.saveUser(user)
            NetworkResult.Success(user)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Login failed")
        }
    }


    // So for registration what we are doing is : Using "FirebaseAuth" to create an "User" using "email" and "password"
    // After that user object is created using func "createUserWithEmailAndPassword" we obtain the "user object" using
    // firebaseAuthResponse.result.user!!   now this is "FirebaseUser" class object, it contains "email" and "uid",
    // this "uid" will be unique identification for each user,  NOTE: U CAN'T OBTAIN PASSWORD AS IT IS ENCRYPTED FOR SECURITY PURPOSE
    // so in our Realtime DB we are also storing things other than "email" and "uid" right, so we'll create a "users" node in our
    // realtime db .. which will store the various other things related to the user... like user's name, email, profilePic, etc...

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private val _signoutLiveData : MutableLiveData<ConsumableValue<NetworkResult<Boolean>>> = MutableLiveData()
    val signoutLiveData : LiveData<ConsumableValue<NetworkResult<Boolean>>> = _signoutLiveData

    suspend fun signoutUser() = withContext(Dispatchers.IO) {

        try {
            messagesDao.clearAllMessages()

            clearDeviceFromFcmTokenList()
            clearFirestoreCache()

            firebaseAuth.signOut()
            tokenManager.signOut()
            _signoutLiveData.postValue(ConsumableValue(NetworkResult.Success(true)))
        } catch (ex: Exception){
            _signoutLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${ex.message}")))
        }
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {

        try {
            messagesDao.clearAllMessages()
            tokenManager.getUser()?.userId?.let { userId ->
                firebaseDb.collection(Constants.DB.USERS)
                    .document(userId)
                    .update("accountDeleted", true)
                    .await()
            }

            clearDeviceFromFcmTokenList()
            clearFirestoreCache()
            tokenManager.signOut()

            firebaseAuth.currentUser?.delete()?.await()

            _signoutLiveData.postValue(ConsumableValue(NetworkResult.Success(true)))
        } catch (ex: Exception){
            _signoutLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error ${ex.message}")))
        }
    }

    private suspend fun clearDeviceFromFcmTokenList(){
        val user = tokenManager.getUser()

        var fcmDeviceTokenList = user?.fcmTokenListOfDevices

        val token = tokenManager.getFcmToken()

        if(fcmDeviceTokenList?.contains(token) == true){
            val tokenList = fcmDeviceTokenList.toMutableList()
            tokenList.remove(token)
            fcmDeviceTokenList = tokenList.toList()

            user?.userId?.let { userId->
                try {
                    firebaseDb.collection(Constants.DB.USERS)
                        .document(userId)
                        .update("fcmTokenListOfDevices", fcmDeviceTokenList)
                        .await()
                } catch (ex: Exception){
                    Log.d("skt", "Error : ${ex.message}")
                }
            }
        }
    }

    suspend fun onNewToken(newToken : String){      // Called from services when this device's FCM-token is changed!
        val currentToken = tokenManager.getFcmToken()
        tokenManager.saveFcmToken(newToken)
        if(currentToken != newToken) {
            val user = tokenManager.getUser()
            val tokenList = user?.fcmTokenListOfDevices?.toMutableSet() ?: mutableSetOf()
            tokenList.remove(currentToken)
            tokenList.add(newToken)

            user?.userId?.let { userId->
                try {
                    firebaseDb.collection(Constants.DB.USERS)
                        .document(userId)
                        .update("fcmTokenListOfDevices", tokenList)
                        .await()
                } catch (ex: Exception){
                    Log.d("skt", "Error : ${ex.message}")
                }
            }
        }
    }


    private suspend fun clearFirestoreCache() {
        try {
            firebaseDb.clearPersistence().await()
        } catch (e: Exception) {
            Log.e("Firestore", "Error clearing cache: ${e.message}")
        }
    }
}