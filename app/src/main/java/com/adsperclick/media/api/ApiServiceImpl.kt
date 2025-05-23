package com.adsperclick.media.api

import android.net.Uri
import android.util.Log
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB
import com.adsperclick.media.utils.Utils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class ApiServiceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRef: StorageReference,
    private val fs: FirebaseStorage
) : ApiService {

    @Inject
    lateinit var cloudFunctions: FirebaseFunctions

    override suspend fun getServiceList(): NetworkResult<ArrayList<Service>> {
        return try {
            val querySnapshot = db.collection(DB.SERVICE).get().await()
            val serviceList = arrayListOf<Service>()
            serviceList.addAll(querySnapshot.toObjects(Service::class.java))
            NetworkResult.Success(serviceList)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Service data fetching failed")
        }
    }

    override suspend fun registerCompany(data: Company): NetworkResult<Company> {
        return try {
            val companyId: String?
            val companyNameInDataBase: String?
            val newCompanyRef = db.collection(DB.COMPANY).document()

            val companyQuery = db.collection(DB.COMPANY)
                .whereEqualTo("gstNumber", data.gstNumber)
                .get()
                .await()

            if (!companyQuery.isEmpty) {
                companyId = companyQuery.documents.first().id
                companyNameInDataBase = companyQuery.documents.first().getString("companyName")
                if(companyNameInDataBase != data.companyName) {
                    return NetworkResult.Error(null, "GST number is already registered with $companyNameInDataBase. Please Try Again")
                }
            } else {
                companyId = newCompanyRef.id
            }

            val company = Company(
                companyId = companyId,
                companyName = data.companyName,
                gstNumber = data.gstNumber,
                listOfServices = data.listOfServices
            )

            newCompanyRef.set(company).await()
            NetworkResult.Success(company)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Company registration failed")
        }
    }

    override suspend fun registerService(data: Service): NetworkResult<Service> {
        return try {

            val existingServices = db.collection(DB.SERVICE)
                .whereEqualTo("serviceName", data.serviceName)
                .get()
                .await()

            if (!existingServices.isEmpty) {
                return NetworkResult.Error(null, "Service with this name already exists.")
            }

            val newServiceRef = db.collection(DB.SERVICE).document()
            val serviceId = newServiceRef.id

            val service = Service(
                serviceId = serviceId,
                serviceName = data.serviceName
            )

            newServiceRef.set(service).await()

            NetworkResult.Success(service)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Service registration failed")
        }
    }

    override suspend fun registerUser(data: User): NetworkResult<User> {
        return try {
            // 2️⃣ Create the user in Firebase Auth
            val result = data.email?.let { email ->
                data.password?.let { password ->
                    auth.createUserWithEmailAndPassword(email, password).await()
                }
            }

            val firebaseUser = result?.user ?: return NetworkResult.Error(null, "User authentication failed")

            val user = data.copy(userId = firebaseUser.uid)

            // 4️⃣ Save User in Firestore
            db.collection(DB.USERS).document(firebaseUser.uid).set(user).await()

            NetworkResult.Success(user)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Registration failed")
        }
    }


    override suspend fun createGroup(data: GroupChatListingData, file: File?): NetworkResult<Boolean> {
        return try {
            val groupCollection = db.collection("groups")

            // Step 1: Check if a group with the same groupName, associatedServiceId, and associatedService exists
            val query = groupCollection
                .whereEqualTo("groupName", data.groupName)
                .whereEqualTo("associatedServiceId", data.associatedServiceId)
                .whereEqualTo("associatedService", data.associatedService)
                .get()
                .await()

            if (query.isEmpty) {
                // Step 2: Prepare group data, with or without image
                val groupRef = groupCollection.document()  // Generates a new document ID
                val groupId = groupRef.id  // Get the auto-generated group ID

                var groupData = data.copy(groupId = groupId)

                // Only handle file upload if file is not null
                if (file != null) {
                    val storageRef = storageRef
                    val imagePath = "images/group_profile_images/${System.currentTimeMillis()}_${file.name}"
                    val imageRef = storageRef.child(imagePath)

                    // Upload the file
                    val uploadTask = imageRef.putFile(Uri.fromFile(file))
                    uploadTask.await()

                    // Get the download URL
                    val imageUrl = imageRef.downloadUrl.await().toString()

                    // Update the group data with the imageUrl
                    groupData = groupData.copy(groupImgUrl = imageUrl)
                }

                // Step 3: Save the group data in Firestore
                groupRef.set(groupData).await()

                // Step 4: Update each user's listOfGroupsAssigned with the new groupId
                val userCollection = db.collection("users")

                data.listOfUsers?.forEach { userPair ->
                    val userId = userPair.userId  // Extract userId

                    val userRef = userCollection.document(userId)
                    userRef.get().await().toObject(User::class.java)?.let { user ->
                        val updatedGroups = user.listOfGroupsAssigned?.toMutableList() ?: mutableListOf()
                        if (!updatedGroups.contains(groupId)) {
                            updatedGroups.add(groupId)
                        }

                        // Step 5: Update user document with new groupId
                        userRef.update("listOfGroupsAssigned", updatedGroups).await()
                    }
                }

                // Step 6: Return success if everything went well
                NetworkResult.Success(true)
            } else {
                // Step 7: Return error if a group with the same name and service already exists
                NetworkResult.Error(null, "A group with this name and service already exists.")
            }

        } catch (e: Exception) {
            // Step 8: Handle any errors that occur
            NetworkResult.Error(null, e.message ?: "Group Creation failed")
        }
    }

    override suspend fun getCompanyList(): NetworkResult<ArrayList<Company>> {
        return try {
            val querySnapshot = db.collection(DB.COMPANY).get().await()
            val companyList = arrayListOf<Company>()
            companyList.addAll(querySnapshot.toObjects(Company::class.java))
            NetworkResult.Success(companyList)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Service data fetching failed")
        }
    }

    override suspend fun deleteService(serviceId: String): NetworkResult<Boolean> {
        return try {
            // First check if the service is used by any groups
            val groupsWithService = db
                .collection(DB.GROUPS)
                .whereEqualTo("associatedServiceId", serviceId)
                .get()
                .await()

            if (!groupsWithService.isEmpty) {
                return NetworkResult.Error(false, "Cannot delete service: It is being used by one or more groups")
            }

            // If not used, delete the service
            db.collection("services")
                .document(serviceId)
                .delete()
                .await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(false, e.message ?: "Error deleting service")
        }
    }

    override suspend fun updateUser(
        userId: String,
        phoneNumber: String?,
        file: File?
    ): NetworkResult<Boolean> {
        return try {
            val userCollection = db.collection(DB.USERS)
            val userRef = userCollection.document(userId)

            // Create a map to hold the fields to update
            val updates = mutableMapOf<String, Any>()

            // Add phone number to updates
            if (phoneNumber != null){
                updates["userPhoneNumber"] = phoneNumber
            }


            // If file is not null, upload it and get the URL
            if (file != null) {
                val storageRef = storageRef
                val imagePath = "images/user_profile_images/${userId}_${System.currentTimeMillis()}_${file.name}"
                val imageRef = storageRef.child(imagePath)

                // Upload the file
                val uploadTask = imageRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                // Get the download URL
                val imageUrl = imageRef.downloadUrl.await().toString()

                // Add profile image URL to the updates
                updates["userProfileImgUrl"] = imageUrl
            }

            // Update the user document with all the changes
            userRef.update(updates).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "User update failed")
        }

    }


    override suspend fun getUserData(userId: String): NetworkResult<User> {
        return try {
            val userDoc = db.collection(DB.USERS).document(userId).get(Source.SERVER).await()
            Log.d("Firestore Debug", "Fetched user data: ${userDoc.data}")
            val user = userDoc.toObject(User::class.java)
                ?: return NetworkResult.Error(null, "User not found")

            Log.d("Firestore Debug", "User object: $user")
            Log.d("Firestore Debug", "Get Boolean : ${userDoc.getBoolean("blocked")}")

            NetworkResult.Success(user)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to fetch user data")
        }
    }

    override suspend fun getCompanyData(companyId: String): NetworkResult<Company> {
        return try {
            val companyDoc = db.collection(DB.COMPANY).document(companyId).get().await()
            val company = companyDoc.toObject(Company::class.java)
                ?: return NetworkResult.Error(null, "Company not found")

            NetworkResult.Success(company)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to fetch company data")
        }
    }

    override suspend fun getCompanyNameData(companyName: String): NetworkResult<Company> {
        return try {
            // Query companies collection where companyName equals the provided name
            val querySnapshot = db.collection(DB.COMPANY)
                .whereEqualTo("companyName", companyName)
                .get()
                .await()

            // Check if any documents match
            if (querySnapshot.isEmpty) {
                return NetworkResult.Error(null, "Company not found")
            }

            // Get the first matching document
            val company = querySnapshot.documents.first().toObject(Company::class.java)
                ?: return NetworkResult.Error(null, "Error converting company data")

            NetworkResult.Success(company)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to fetch company data")
        }
    }

    override suspend fun getMultipleUsers(userIds: List<String>): NetworkResult<List<User>> {
        return try {
            if (userIds.isEmpty()) {
                return NetworkResult.Error(null,"No user IDs provided")
            }

            val usersList = mutableListOf<User>()

            // Fetch users in batches to avoid potential limits
            for (userId in userIds) {
                val userDoc = db.collection(DB.USERS).document(userId).get().await()
                val user = userDoc.toObject(User::class.java)

                user?.let {
                    if (user.accountDeleted == null) {
                        usersList.add(it) }
                }
            }

            if (usersList.isEmpty()) {
                return NetworkResult.Error(null, "No users found")
            }

            NetworkResult.Success(usersList)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to fetch users data")
        }
    }

    override suspend fun updateGroupProfile(
        groupId: String,
        groupName: String?,
        file: File?
    ): NetworkResult<Boolean> {
        return try {
            val userCollection = db.collection(DB.GROUPS)
            val userRef = userCollection.document(groupId)

            // Create a map to hold the fields to update
            val updates = mutableMapOf<String, Any>()

            // Add group name to updates
            if (groupName != null) {
                updates["groupName"] = groupName
            }

            // If file is not null, handle the image update
            if (file != null) {
                // First, check if there's an existing URL
                val documentSnapshot = userRef.get().await()
                val existingImageUrl = documentSnapshot.getString("groupImgUrl")

                // If an existing URL exists, delete that image first
                if (existingImageUrl != null) {
                    try {
                        // Extract the path from the URL (after "images/")
                        val pathReference = fs.getReferenceFromUrl(existingImageUrl)
                        // Delete the file
                        pathReference.delete().await()
                    } catch (e: Exception) {
                        NetworkResult.Error(null, e.message ?: "Error deleting previous image")
                    }
                }
                // Upload the new file
                val storageRef = storageRef
                val imagePath = "images/group_profile_images/${groupId}_${System.currentTimeMillis()}_${file.name}"
                val imageRef = storageRef.child(imagePath)

                // Upload the file
                val uploadTask = imageRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                // Get the download URL
                val imageUrl = imageRef.downloadUrl.await().toString()

                // Add profile image URL to the updates
                updates["groupImgUrl"] = imageUrl
            }

            // Update the user document with all the changes
            userRef.update(updates).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Group update failed")
        }
    }

    override suspend fun removeUserFromGroup(
        userId: String,
        groupId: String
    ): NetworkResult<String> {
        return try {
            // 1. Get reference to the group document
            val groupCollection = db.collection("groups")
            val groupRef = groupCollection.document(groupId)

            // 2. Get the current group data
            val groupSnapshot = groupRef.get().await()
            val groupData = groupSnapshot.toObject(GroupChatListingData::class.java)

            // 3. Remove the user from the group's listOfUsers
            groupData?.let { group ->
                val updatedUsersList = group.listOfUsers?.toMutableList() ?: mutableListOf()
                updatedUsersList.removeIf { it.userId == userId }

                // Update the group document
                groupRef.update("listOfUsers", updatedUsersList).await()

                // 4. Update the user's document to remove this group
                val userCollection = db.collection("users")
                val userRef = userCollection.document(userId)

                val userSnapshot = userRef.get().await()
                val userData = userSnapshot.toObject(User::class.java)

                userData?.let { user ->
                    val updatedGroupsList = user.listOfGroupsAssigned?.toMutableList() ?: mutableListOf()
                    updatedGroupsList.remove(groupId)

                    // Update the user document
                    userRef.update("listOfGroupsAssigned", updatedGroupsList).await()
                }
            }

            NetworkResult.Success(userId)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to remove user from group")
        }
    }

    override suspend fun getGroupDetails(groupId: String): NetworkResult<GroupChatListingData> {
        return try {
            val groupData = db.collection(DB.GROUPS).document(groupId).get().await()
            val group = groupData.toObject(GroupChatListingData::class.java)
                ?: return NetworkResult.Error(null, "Group not found")

            NetworkResult.Success(group)

        } catch (e:Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to fetch group data")
        }

    }

    override suspend fun addGroupMember(
        groupId: String,
        userSet: MutableSet<String>
    ): NetworkResult<Boolean> {
        return try {
            // 1. Update each user's listOfGroupsAssigned
            val userCollection = db.collection("users")
            val groupCollection = db.collection("groups")
            val groupRef = groupCollection.document(groupId)

            // Get the group data first
            val groupSnapshot = groupRef.get().await()
            val groupData = groupSnapshot.toObject(GroupChatListingData::class.java)

            // Create a list to hold new group users
            val currentUsers = groupData?.listOfUsers?.toMutableList() ?: mutableListOf()
            val currentUserIds = currentUsers.map { it.userId }.toSet()
            val newGroupUsers = mutableListOf<GroupUser>()

            // Update each user document and build the list of new group users
            userSet.forEach { userId ->
                // Only add if user is not already in the group
                if (!currentUserIds.contains(userId)) {
                    // Update user's listOfGroupsAssigned
                    val userRef = userCollection.document(userId)
                    userRef.get().await().toObject(User::class.java)?.let { user ->
                        val updatedGroups = user.listOfGroupsAssigned?.toMutableList() ?: mutableListOf()
                        if (!updatedGroups.contains(groupId)) {
                            updatedGroups.add(groupId)
                        }
                        userRef.update("listOfGroupsAssigned", updatedGroups).await()
                    }

                    // Add user to the list of new group users
                    newGroupUsers.add(GroupUser(userId = userId, lastSeenMsgId = null))
                }
            }

            // If there are new users to add to the group
            if (newGroupUsers.isNotEmpty()) {
                // Add all new users to the current users list
                currentUsers.addAll(newGroupUsers)

                // Update the group document
                groupRef.update("listOfUsers", currentUsers).await()
            }

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to add members to group")
        }
    }



    override suspend fun updateCompanyServices(
        companyId: String,
        services: List<Service>
    ): NetworkResult<Boolean> {
        return try {
            val companyRef = db.collection(DB.COMPANY).document(companyId)

            // Update the company document with the new services list
            companyRef.update("listOfServices", services).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(false, e.message ?: "Failed to update company services")
        }
    }


    override suspend fun getUserCallToken(groupId: String): NetworkResult<Pair<String,String>> {
        return try {
            // Prepare the data to send to the Cloud Function
            val data = hashMapOf(
                "groupId" to groupId
            )

            // Use Tasks.await() to wait for the Cloud Function result
            val result = Tasks.await(
                cloudFunctions
                    .getHttpsCallable("generateAgoraToken")
                    .call(data)
            )

            // Extract the result data
            @Suppress("UNCHECKED_CAST")
            val response = result.getData() as HashMap<String, Any>

            val token = response["token"] as String
            val channel = response["channel"] as String
            // Note: channelName is received but not used in this function

            NetworkResult.Success(Pair(token,channel))
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to get user call token")
        }
    }

    override suspend fun listenParticipantChanges(groupId: String): Flow<NetworkResult<Call>> = callbackFlow {
        try {
            val groupDoc = db.collection(DB.GROUPS).document(groupId)
            val callLog = groupDoc.collection(DB.GROUP_CALL_LOG)

            // Query for the ongoing call
            val querySnapshot = callLog
                .whereEqualTo("status", Constants.CALL.STATUS.ONGOING)
                .limit(1).get().await()

            val reqdDoc = querySnapshot.documents.firstOrNull()

            if (reqdDoc != null) {
                // Set up the listener for the specific call document
                val listenerRegistration = db.collection(DB.GROUPS)
                    .document(groupId)
                    .collection(DB.GROUP_CALL_LOG)
                    .document(reqdDoc.id)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("Firestore", "Realtime updates error: ${error.message}")
                            trySend(NetworkResult.Error(null, error.message ?: "Unknown error"))
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            // Convert the snapshot to a Call object
                            val call = snapshot.toObject(Call::class.java)
                            if (call != null) {
                                trySend(NetworkResult.Success(call))
                            }
                        }
                    }

                // Cancel the listener when the flow is closed
                awaitClose {
                    listenerRegistration.remove()
                }
            } else {
                // No ongoing call found
                trySend(NetworkResult.Error(null, "No ongoing call found"))
                close()
            }
        } catch (e: Exception) {
            Log.e("CallRepository", "Error listening for participant changes: ${e.message}")
            trySend(NetworkResult.Error(null, e.message ?: "Unknown error"))
            close()
        }
    }

    override suspend fun removeUserFromCall(groupData: GroupChatListingData, userData: User) :NetworkResult<Boolean>{

        val groupId = groupData.groupId ?: ""
        val groupName = groupData.groupName ?: ""
        val userId = userData.userId ?: ""

        val groupDoc = db.collection(DB.GROUPS).document(groupId)
        val callLog = groupDoc.collection(DB.GROUP_CALL_LOG)

        try {
            // Fetch latest ONGOING CALL
            val querySnapshot = callLog
                .whereEqualTo("status", Constants.CALL.STATUS.ONGOING)
                .limit(1).get().await()

            val ongoingCall: DocumentSnapshot? = when{
                querySnapshot.documents.isEmpty() -> null
                else -> querySnapshot.documents.firstOrNull()
            }

            if (ongoingCall == null) {
                Log.w("CallRepository", "No ongoing call found.")
                return NetworkResult.Error(null, "No ongoing call found.")
            }

            val callDocRef = callLog.document(ongoingCall.id)

            // Fetch existing participants
            val callData = callDocRef.get().await()
            val participants = callData.get("participants") as? MutableMap<String, Any> ?: mutableMapOf()

            if (!participants.containsKey(userId)) {
                Log.w("CallRepository", "User $userId not found in call participants.")
                return NetworkResult.Error(null, "User $userId not found in call participants.")
            }

            // Remove user from participants map
            participants.remove(userId)

            // If no participants are left, mark call as COMPLETED
            val updateMap = if (participants.isEmpty()) {

                // participants empty = last person to leave call = trigger msg & notif
                // Sending msg in group
                val listOfGroupMemberId = groupData.listOfUsers?.map { it.userId } ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    sendMessage("Ended the Call", groupId, userData, groupName,
                        listOfGroupMemberId, Constants.MSG_TYPE.CALL)
                }

                // Sending notif to group members
                CoroutineScope(Dispatchers.IO).launch {
                    triggerNotificationToGroupMembers(
                        groupId= groupId,
                        groupName= groupName, msgText = "Call Ended", senderId = userId,
                        msgType = Constants.MSG_TYPE.CALL, listOfGroupMemberId = listOfGroupMemberId)
                }

                mapOf(
                    "participants" to participants,
                    "status" to Constants.CALL.STATUS.COMPLETED,
                    "endTime" to System.currentTimeMillis()
                )
            } else {
                mapOf("participants" to participants)
            }

            // Update Firestore with modified participants
            callDocRef.update(updateMap).await()        // Using updateMap "we will only touch these fields other remain same"
            return NetworkResult.Success(true)
        } catch (e: Exception) {
            Log.e("CallRepository", "Error removing user from call: ${e.message}")
            return NetworkResult.Error(null, "${e.message}")
        }
    }

    fun sendMessage(msgText: String, groupId: String, user: User, groupName: String,
                            listOfGroupMemberId: List<String>, msgType: Int) {
        val messagesRef = db.collection(DB.MESSAGES)
            .document(groupId)
            .collection(DB.MESSAGES_INSIDE_MESSAGES)

        val msgId = messagesRef.document().id // Generate a unique ID for the message

        val message = Message(
            msgId = msgId,
            message = msgText,
            senderId = user.userId,
            senderName = user.userName,
            senderRole = user.role,
            msgType = msgType,
            groupId = groupId
        )

        messagesRef.document(msgId)
            .set(message.toMapForFirestore()) // Convert to Map to use server timestamp
            .addOnSuccessListener {
                Log.d("Firestore", "Message sent successfully: $msgId")
                // Now since our "Message" object is sent to server, it means the firestore
                // timestamp is updated on it we'll get it back using the "msgId"
                messagesRef.document(msgId).get()
                    .addOnSuccessListener {
                        val updatedMessage = it.toMessage()
                        updatedMessage?.let { msg ->
                            db.collection(DB.GROUPS)
                                .document(groupId)
                                .update("lastSentMsg", msg) // ✅ Save correct message with timestamp
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error sending message: ${e.message}")
            }

        // firestore.collection(Constants.DB.GROUPS).document(groupId).update("lastSentMsg", message)
    }

//    groupName, msgText, senderId, listOfGroupMemberId

    suspend fun triggerNotificationToGroupMembers(
        groupId: String, groupName:String, msgText: String, senderId: String,
        msgType: Int, listOfGroupMemberId: List<String>)
    {
        val data = mapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "msgText" to msgText,
            "msgType" to msgType,
            "senderId" to senderId,
            "listOfGroupMemberId" to listOfGroupMemberId
        )

        try {
            cloudFunctions
                .getHttpsCallable(Constants.FIREBASE_FUNCTION_NAME.SEND_NOTIFICATION_TO_GROUP_MEMBERS)
                .call(data)
                .await()
        } catch (ex: Exception){
            Log.e("skt", "Error : $ex")
        }
    }

    fun DocumentSnapshot.toMessage(): Message? {
        return try {
            val timestampLong = Utils.timestampToLong(this.getTimestamp("timestamp"))     // Convert Firestore Timestamp -> Long

            Message(
                msgId = getString("msgId") ?: "null string",
                message = getString("message"),
                senderId = getString("senderId"),
                senderName = getString("senderName"),
                senderRole = getLong("senderRole")?.toInt(),
                msgType = getLong("msgType")?.toInt(),
                groupId = getString("groupId"),
                timestamp = timestampLong // Store as Long (milliseconds)
            )
        } catch (e: Exception) {
            Log.e("Firestore", "Error parsing Message: ${e.message}")
            null
        }
    }

    /**
     * Updates a user's call status in real-time (mute and speaking status)
     *
     * @param groupId The ID of the group where the call is taking place
     * @param userId The ID of the user whose status is being updated
     * @param isMuted Whether the user's microphone is muted
     * @param isSpeaking Whether the user is currently speaking (based on volume threshold)
     */
    override suspend fun updateUserCallStatus(groupId: String, userId: String, isMuted: Boolean?, isSpeaking: Boolean?): NetworkResult<Boolean> {
        return try {
            // Get reference to the ongoing call
            val groupDoc = db.collection(DB.GROUPS).document(groupId)
            val callLog = groupDoc.collection(DB.GROUP_CALL_LOG)

            // Find the ongoing call
            val querySnapshot = callLog
                .whereEqualTo("status", Constants.CALL.STATUS.ONGOING)
                .limit(1)
                .get()
                .await()

            val ongoingCall = querySnapshot.documents.firstOrNull() ?: run {
                return NetworkResult.Error(false, "No ongoing call found to update user status")
            }

            val callDocRef = callLog.document(ongoingCall.id)

            // Get current participants
            val callData = callDocRef.get().await()
            val participants = callData.get("participants") as? MutableMap<String, Any> ?: run {
                return NetworkResult.Error(false, "Failed to get participants map")
            }

            // Get the specific user's data
            val participantData = participants[userId] as? Map<String, Any> ?: run {
                return NetworkResult.Error(false, "User $userId not found in call participants")
            }

            // Create updated participant with new states
            val updatedParticipant = participantData.toMutableMap()

            // Only update what's provided
            isMuted?.let { updatedParticipant["muteOn"] = it }
            isSpeaking?.let { updatedParticipant["speakerOn"] = it }

            // Update only if there are changes
            if (
                (isMuted != null && isMuted != participantData["muteOn"]) ||
                (isSpeaking != null && isSpeaking != participantData["speakerOn"])
            ) {
                // Update the participant in the map
                participants[userId] = updatedParticipant

                // Update Firestore with the modified participants map
                callDocRef.update("participants", participants).await()
                return NetworkResult.Success(true)
            } else {
                // No changes needed
                return NetworkResult.Error(false, "Error updating user call status")
            }
        } catch (e: Exception) {
            return NetworkResult.Error(false, e.message ?: "Error updating user call status")
        }
    }

}