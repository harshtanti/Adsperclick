package com.adsperclick.media.data.repositories

import android.util.Log
import androidx.core.util.TimeUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor() {

    @Inject
    lateinit var firebaseAuth : FirebaseAuth

    @Inject
    lateinit var firebaseDb : FirebaseFirestore

    @Inject
    lateinit var tokenManager : TokenManager


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

    suspend fun signoutUser() {
        tokenManager.signOut()
        clearFirestoreCache()
        firebaseAuth.signOut()
    }

    private suspend fun clearFirestoreCache() {
        try {
            firebaseDb.clearPersistence().await()
        } catch (e: Exception) {
            Log.e("Firestore", "Error clearing cache: ${e.message}")
        }
    }
}