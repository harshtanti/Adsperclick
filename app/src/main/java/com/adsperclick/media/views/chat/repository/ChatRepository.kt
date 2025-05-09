package com.adsperclick.media.views.chat.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.di.VersionProvider
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.Constants.LAST_SEEN_TIME_EACH_USER_EACH_GROUP
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.utils.Utils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestore:FirebaseFirestore
) {

    @Inject
    lateinit var tokenManager : TokenManager

    @Inject
    lateinit var messagesDao: MessagesDao

    @Inject
    lateinit var cloudFunctions: FirebaseFunctions

    @Inject
    lateinit var storageRef: StorageReference

    @Inject
    lateinit var versionProvider: VersionProvider

    @Inject
    lateinit var context: Context


// ----------------------------------------------------------------------------------------------------------------
// WHEN USER OPENS THE APP, THESE FUNCTIONS ARE SUPPOSED TO RUN ONLY ONE TIME, AFTER THE USER OPENS THE APP,
// SO WE RUN THEM ON SPLASH SCREEN

    suspend fun syncUser():ConsumableValue<NetworkResult<User>>{
        try{

            val userId = tokenManager.getUser()?.userId
            val result = firestore.collection(DB.USERS).document(userId!!).get().await()

            if (result.exists().not()) {
                return ConsumableValue(NetworkResult.Error(null, "User not found in database"))
            }

            val user = result.toObject(User::class.java) // Convert Firestore doc to User object

            if(user?.role != Constants.ROLE.CLIENT){
                // Now we fetch list of all services and put it in "User" object, thereafter it will be saved in shared Prefs
                val servicesList = firestore.collection(DB.SERVICE).orderBy("serviceName").get().await()
                val listOfServices = arrayListOf<Service>(DEFAULT_SERVICE)      // Filled first service with default service i.e. "All"
                for(document in servicesList.documents){
                    val service = document.toObject(Service::class.java)
                    service?.let {
                        listOfServices.add(it)
                    }
                }
                user?.listOfServicesAssigned = listOfServices
            }

            user?.let {
                // Write code for when new "User" object obtained from backend successfully

                // "fetchLastSeenTimeForEachUserInEachGroup" fun provides last seen time for each user
                // in each group
                fetchLastSeenTimeForEachUserInEachGroup(it.listOfGroupsAssigned?: listOf())

                tokenManager.saveUser(it) // Update SharedPreferences
                return ConsumableValue(NetworkResult.Success(it))  // Notify UI with updated user
            } ?: return ConsumableValue(NetworkResult.Error(null, "Failed to parse user data"))
        }
        catch (e: Exception){
            return ConsumableValue(NetworkResult.Error(null, "Error: ${e.message}"))
        }
    }


    // This function is used to sync device time with server time, it basically brings the server time
    // at this instance and compares it with device time, if there's a difference between them, it means
    // that this person's device's time is incorrect! In that case, we'll save the difference b/w
    // serverTime - currentDeviceTime
    suspend fun syncDeviceTime() {
        try {
            if(Utils.isNetworkAvailable(context).not()){ // To handle the case user is offline
                Log.d("skt", "User is offline") // This function is updating firebase, so c
                                        // Cannot be run when user is offline
                return
            }


            val docRef = firestore.collection(DB.CONFIG).document(DB.SERVER_TIME_DOC)

            // Update the document with the server timestamp
            docRef.update(DB.SERVER_TIME_DOC, FieldValue.serverTimestamp()).await()

            // Fetch the updated timestamp
            val snapshot = docRef.get().await()
            snapshot.getTimestamp(DB.SERVER_TIME_DOC)?.let { serverTime ->
                val timeDiff = Utils.timestampToLong(serverTime) - System.currentTimeMillis()
                // Some timeDiff coz of time taken to fetch server-side time, so we ignore upto 1 sec
                if(timeDiff > 1000L) tokenManager.setServerMinusDeviceTime(timeDiff)
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException,  // No internet connection
                is FirebaseFirestoreException -> {
                    Log.e("skt", "Network error: ${e.message}")
                }
                else -> Log.e("skt", "Unexpected error: ${e.message}")
            }
        }
    }

    // Below fun checks if the current version on user's phone is high enough or not!
    // It compares the current version of app on this device (Which is written in Build.Gradle as
    // " versionCode = 1 " to the minimum acceptable version on firestore, if the appVersion is
    // ">=" minAcceptableVersion the app can be used, else ask user to update app because
    // force update is required
    suspend fun isCurrentVersionAcceptable(): Boolean {
        return try {
            val result = firestore.collection(DB.CONFIG).document(DB.MIN_APP_LEVEL_DOC).get().await()
            val minAcceptableVersion = result.getLong(DB.MIN_APP_LEVEL_DOC) ?: 1

            val currentAppVersion = versionProvider.appVersion // Get current app version

            // If current app version is lower than min required, return false (force update needed)
            currentAppVersion >= minAcceptableVersion
        } catch (e: Exception) {
            Log.e("compareAppVersion", "Error: ${e.message}")
            true // In case of Firestore failure, allow app usage (fail-safe)
        }
    }


    // ------------------------------------------------------------------------------------------------------------------


    // This "fetchLastSeenTimeForEachUserInEachGroup" fun fetches last seen time for each user in each
    // group
    // Here we have a collection called "groups", in this collection, documents are named as "groupId"
    // Inside each "document", there are several fields... and a "subCollection" called "groupMembersLastSeenTime"
    // Now this "subCollection" is present for all documents and there's no direct way to get all the
    // subCollections inside each document
    // So firebase provides us a concept! Called "collectionGroup", when u request a collectionGroup
    // and ask for a collection name in parameter... it will look across the entire firestore db
    // and wherever it finds a "collection" or "subCollection" with the name "groupMembersLastSeenTime"
    // this fun will fetch it! Therefore all collections/subCollections with this name is fetched
    // so even if these subCollections are in different documents they are fetched.
    suspend fun fetchLastSeenTimeForEachUserInEachGroup(
        listOfGroupChatId: List<String>
    ): NetworkResult<Map<String, MutableMap<String, Long?>>> {
        return try {
            val querySnapshot = firestore.collectionGroup(DB.GROUP_MEMBERS_LAST_SEEN_TIME).get().await()
            // This above line provides all the subCollections with name "groupMembersLastSeenTime"
            val lastSeenData = mutableMapOf<String, MutableMap<String, Long?>>()

            for (doc in querySnapshot.documents) {
                val groupId = doc.reference.parent.parent?.id // Get the groupId from the parent document
                                                              // See on firestore console for better idea
                if (groupId in listOfGroupChatId) {
                    val userId = doc.id
                    val lastSeenTimestamp = doc.getTimestamp("lastSeenTime")
                    val timestampInLong = Utils.timestampToLong(lastSeenTimestamp)

                    groupId?.let {
                        lastSeenData.getOrPut(groupId) { mutableMapOf() }[userId] = timestampInLong
                    }
                }
            }
            LAST_SEEN_TIME_EACH_USER_EACH_GROUP = lastSeenData
            NetworkResult.Success(lastSeenData)
        } catch (e: Exception) {
            Log.e("skt", "Error: ${e.message}")
            NetworkResult.Error(null, "Error: ${e.message}")
        }
    }


    private val _listOfGroupChatLiveData = MutableLiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>>()
    val listOfGroupChatLiveData: LiveData<ConsumableValue<NetworkResult<List<GroupChatListingData>>>> get() = _listOfGroupChatLiveData

    suspend fun listenToGroupChatUpdates(listOfGroupChatId: List<String>) {

        // Just for knowledge if "fetchLastSeenTimeForEachUserInEachGroup" was here..
        // fetchLastSeenTimeForEachUserInEachGroup(listOfGroupChatId)
        // The lines below will
        // execute immediately and will not wait for this function to return because we are not
        // using "await()" in the above function, we are using "call-backs" i.e. onSuccessListener

        try {
            _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Loading()))

            // Firestore listener (Real-time updates)
            val query = firestore.collection(DB.GROUPS)
                .whereIn("groupId", listOfGroupChatId)

            query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Error(null, "Error: ${error.message}")))
                    Log.e("skt", "Error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val listOfGroups = arrayListOf<GroupChatListingData>()

                    for (document in snapshot.documents) {
                        val group = document.toObject(GroupChatListingData::class.java)
                        group?.let { listOfGroups.add(it) }
                    }

                    // Sort by lastSentMsg timestamp in descending order (latest message first)
                    val sortedGroups = listOfGroups.sortedByDescending { it.lastSentMsg?.timestamp ?: 0L }

                    _listOfGroupChatLiveData.postValue(ConsumableValue(NetworkResult.Success(sortedGroups)))
                }
            }
        } catch (e:Exception){
            Log.d("skt", "Error: ${e.message}")
        }
    }


    suspend fun removeUserFromCall(groupData: GroupChatListingData, userData: User) = apiService.removeUserFromCall(groupData,userData)

    suspend fun getMultipleUsers(userIds: List<String>) = apiService.getMultipleUsers(userIds)

    suspend fun updateGroupProfile(groupId:String,groupName:String?, file: File?) = apiService.updateGroupProfile(groupId,groupName,file)

    suspend fun removeUserFromGroup(userId: String, groupId: String) = apiService.removeUserFromGroup(userId,groupId)

    suspend fun getGroupDetails(groupId: String) = apiService.getGroupDetails(groupId)

}

