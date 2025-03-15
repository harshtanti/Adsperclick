package com.adsperclick.media.api

import android.net.Uri
import android.util.Log
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB.GROUPS
import com.adsperclick.media.utils.Constants.DB.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class ApiServiceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRef: StorageReference,
    private val fs: FirebaseStorage
) : ApiService {

    override suspend fun getServiceList(): NetworkResult<ArrayList<Service>> {
        return try {
            val querySnapshot = db.collection(Constants.DB.SERVICE).get().await()
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
            val newCompanyRef = db.collection(Constants.DB.COMPANY).document()

            val companyQuery = db.collection(Constants.DB.COMPANY)
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
                gstNumber = data.gstNumber
            )

            newCompanyRef.set(company).await()
            NetworkResult.Success(company)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Company registration failed")
        }
    }

    override suspend fun registerService(data: Service): NetworkResult<Service> {
        return try {

            val existingServices = db.collection(Constants.DB.SERVICE)
                .whereEqualTo("serviceName", data.serviceName)
                .get()
                .await()

            if (!existingServices.isEmpty) {
                return NetworkResult.Error(null, "Service with this name already exists.")
            }

            val newServiceRef = db.collection(Constants.DB.SERVICE).document()
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
            db.collection(Constants.DB.USERS).document(firebaseUser.uid).set(user).await()

            NetworkResult.Success(user)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Registration failed")
        }
    }

    /*override suspend fun createGroup(data: GroupChatListingData): NetworkResult<GroupChatListingData> {

        return try {
            val groupCollection = FirebaseFirestore.getInstance().collection("groups")

            // Step 1: Check if a group with the same groupName, associatedServiceId, and associatedService exists
            val query = groupCollection
                .whereEqualTo("groupName", data.groupName)
                .whereEqualTo("associatedServiceId", data.associatedServiceId)
                .whereEqualTo("associatedService", data.associatedService)
                .get()
                .await()

            if (query.isEmpty) {
                // Step 2: If no such group exists, create the group document
                val groupRef = groupCollection.document()  // Generates a new document ID
                val groupId = groupRef.id  // Get the auto-generated group ID

                // Step 3: Update the group data with the generated groupId
                val groupData = data.copy(groupId = groupId)

                // Step 4: Save the updated group data in Firestore
                groupRef.set(groupData).await()

                // Step 5: Update each user's listOfGroupsAssigned with the new groupId
                val userCollection = FirebaseFirestore.getInstance().collection("users")

                data.listOfUsers?.forEach { userPair ->
                    val userId = userPair.userId  // Extract userId

                    val userRef = userCollection.document(userId)
                    userRef.get().await().toObject(User::class.java)?.let { user ->
                        val updatedGroups = user.listOfGroupsAssigned?.toMutableList() ?: mutableListOf()
                        if (!updatedGroups.contains(groupId)) {
                            updatedGroups.add(groupId)
                        }

                        // Step 6: Update user document with new groupId
                        userRef.update("listOfGroupsAssigned", updatedGroups).await()
                    }
                }

                // Step 7: Return success if everything went well
                NetworkResult.Success(groupData)
            } else {
                // Step 8: Return error if a group with the same name and service already exists
                NetworkResult.Error(null, "A group with this name and service already exists.")
            }

        } catch (e: Exception) {
            // Step 9: Handle any errors that occur
            NetworkResult.Error(null, e.message ?: "Group Creation failed")
        }
    }*/

    override suspend fun createGroup(data: GroupChatListingData, file: File): NetworkResult<Boolean> {
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
                // Step 2: If no such group exists, upload the image first
                val storageRef = storageRef
                val imagePath = "images/group_profile_images/${System.currentTimeMillis()}_${file.name}"
                val imageRef = storageRef.child(imagePath)

                // Upload the file
                val uploadTask = imageRef.putFile(Uri.fromFile(file))
                uploadTask.await()

                // Get the download URL
                val imageUrl = imageRef.downloadUrl.await().toString()

                // Step 3: Create the group document with the image URL
                val groupRef = groupCollection.document()  // Generates a new document ID
                val groupId = groupRef.id  // Get the auto-generated group ID

                // Step 4: Update the group data with the generated groupId and imageUrl
                val groupData = data.copy(groupId = groupId, groupImgUrl = imageUrl)

                // Step 5: Save the updated group data in Firestore
                groupRef.set(groupData).await()

                // Step 6: Update each user's listOfGroupsAssigned with the new groupId
                val userCollection = db.collection("users")

                data.listOfUsers?.forEach { userPair ->
                    val userId = userPair.userId  // Extract userId

                    val userRef = userCollection.document(userId)
                    userRef.get().await().toObject(User::class.java)?.let { user ->
                        val updatedGroups = user.listOfGroupsAssigned?.toMutableList() ?: mutableListOf()
                        if (!updatedGroups.contains(groupId)) {
                            updatedGroups.add(groupId)
                        }

                        // Step 7: Update user document with new groupId
                        userRef.update("listOfGroupsAssigned", updatedGroups).await()
                    }
                }

                // Step 8: Return success if everything went well
                NetworkResult.Success(true)
            } else {
                // Step 9: Return error if a group with the same name and service already exists
                NetworkResult.Error(null, "A group with this name and service already exists.")
            }

        } catch (e: Exception) {
            // Step 10: Handle any errors that occur
            NetworkResult.Error(null, e.message ?: "Group Creation failed")
        }
    }

    override suspend fun getCompanyList(): NetworkResult<ArrayList<Company>> {
        return try {
            val querySnapshot = db.collection(Constants.DB.COMPANY).get().await()
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
                .collection(GROUPS)
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
            val userCollection = db.collection(USERS)
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
            val userDoc = db.collection(Constants.DB.USERS).document(userId).get(Source.SERVER).await()
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
            val companyDoc = db.collection(Constants.DB.COMPANY).document(companyId).get().await()
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
            val querySnapshot = db.collection(Constants.DB.COMPANY)
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
                val userDoc = db.collection(Constants.DB.USERS).document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                user?.let { usersList.add(it) }
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
            val userCollection = FirebaseFirestore.getInstance().collection("groups")
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
            val groupData = db.collection(Constants.DB.GROUPS).document(groupId).get().await()
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
            val companyRef = db.collection(Constants.DB.COMPANY).document(companyId)

            // Update the company document with the new services list
            companyRef.update("listOfServices", services).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(false, e.message ?: "Failed to update company services")
        }
    }

    /*// New call-related method implementations
    override suspend fun startVoiceCall(groupId: String): NetworkResult<Call> {
        return try {
            val callHistoryRef = firebaseDatabase.getReference("callHistory")
            val callId = callHistoryRef.push().key ?: return NetworkResult.Error(null, "Failed to generate call ID")

            val userId = tokenManager.getUser()?.userId ?: return NetworkResult.Error(null, "User not authenticated")
            val user = getUserData(userId).data ?: return NetworkResult.Error(null, "Failed to get user data")

            // Create call participant
            val participant = CallParticipant(
                userId = userId,
                userName = user.userName ?: "Unknown User",
                profileImage = user.profileImage ?: "",
                joinedAt = System.currentTimeMillis()
            )

            // Create new call
            val call = Call(
                callId = callId,
                groupId = groupId,
                startTime = System.currentTimeMillis(),
                initiatedBy = userId,
                initiatorName = user.userName ?: "Unknown User",
                status = "active",
                type = "voice",
                participants = mapOf(userId to participant)
            )

            // Save call to database
            callHistoryRef.child(callId).setValue(call).await()

            // Add to active calls reference
            val activeCallsRef = firebaseDatabase.getReference("activeGroupCalls")
            activeCallsRef.child(groupId).setValue(callId).await()

            // Add the user to active participants
            val participantsRef = firebaseDatabase.getReference("callParticipants").child(callId)
            participantsRef.child(userId).setValue(participant).await()

            NetworkResult.Success(call)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to start call")
        }
    }

    override suspend fun endVoiceCall(callId: String): NetworkResult<Boolean> {
        return try {
            val callHistoryRef = firebaseDatabase.getReference("callHistory").child(callId)
            val snapshot = callHistoryRef.get().await()

            if (!snapshot.exists()) {
                return NetworkResult.Error(null, "Call not found")
            }

            val call = snapshot.getValue(Call::class.java) ?:
            return NetworkResult.Error(null, "Invalid call data")

            // Update call status
            callHistoryRef.child("status").setValue("completed").await()
            callHistoryRef.child("endTime").setValue(System.currentTimeMillis()).await()

            // Remove from active group calls
            val activeCallsRef = firebaseDatabase.getReference("activeGroupCalls").child(call.groupId)
            activeCallsRef.removeValue().await()

            // Remove all participants
            val participantsRef = firebaseDatabase.getReference("callParticipants").child(callId)
            participantsRef.removeValue().await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to end call")
        }
    }

    override suspend fun joinVoiceCall(callId: String): NetworkResult<Boolean> {
        return try {
            val userId = tokenManager.getUser()?.userId ?: return NetworkResult.Error(null, "User not authenticated")
            val user = getUserData(userId).data ?: return NetworkResult.Error(null, "Failed to get user data")

            // Check if call exists and is active
            val callRef = firebaseDatabase.getReference("callHistory").child(callId)
            val callSnapshot = callRef.get().await()

            if (!callSnapshot.exists() || callSnapshot.child("status").getValue(String::class.java) != "active") {
                return NetworkResult.Error(null, "Call not active or doesn't exist")
            }

            // Create participant object
            val participant = CallParticipant(
                userId = userId,
                userName = user.userName ?: "Unknown User",
                profileImage = user.profileImage ?: "",
                joinedAt = System.currentTimeMillis()
            )

            // Add to call participants
            val participantsRef = firebaseDatabase.getReference("callParticipants").child(callId)
            participantsRef.child(userId).setValue(participant).await()

            // Update call history with new participant
            val participantPath = "participants/$userId"
            callRef.child(participantPath).setValue(participant).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to join call")
        }
    }

    override suspend fun leaveVoiceCall(callId: String): NetworkResult<Boolean> {
        return try {
            val userId = tokenManager.getUser()?.userId ?: return NetworkResult.Error(null, "User not authenticated")

            // Remove from call participants
            val participantsRef = firebaseDatabase.getReference("callParticipants").child(callId).child(userId)
            participantsRef.removeValue().await()

            // Check if user is the initiator - if so, end the call for everyone
            val callRef = firebaseDatabase.getReference("callHistory").child(callId)
            val callSnapshot = callRef.get().await()

            if (callSnapshot.child("initiatedBy").getValue(String::class.java) == userId) {
                // If initiator is leaving, end the call
                return endVoiceCall(callId)
            } else {
                // Just update the participant status in call history
                callRef.child("participants/$userId").removeValue().await()
                NetworkResult.Success(true)
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to leave call")
        }
    }

    override suspend fun getActiveCallInGroup(groupId: String): NetworkResult<Call?> {
        return try {
            val activeCallsRef = firebaseDatabase.getReference("activeGroupCalls").child(groupId)
            val snapshot = activeCallsRef.get().await()

            if (!snapshot.exists()) {
                return NetworkResult.Success(null) // No active call
            }

            val callId = snapshot.getValue(String::class.java) ?:
            return NetworkResult.Error(null, "Invalid call reference")

            // Get call details
            val callRef = firebaseDatabase.getReference("callHistory").child(callId)
            val callSnapshot = callRef.get().await()

            if (!callSnapshot.exists()) {
                return NetworkResult.Success(null) // Call reference exists but call data doesn't
            }

            val call = callSnapshot.getValue(Call::class.java)
            NetworkResult.Success(call)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to get active call")
        }
    }

    override suspend fun updateParticipantStatus(callId: String, isMuted: Boolean): NetworkResult<Boolean> {
        return try {
            val userId = tokenManager.getUser()?.userId ?: return NetworkResult.Error(null, "User not authenticated")

            // Update mute status
            val participantRef = firebaseDatabase.getReference("callParticipants").child(callId).child(userId)
            participantRef.child("isMuted").setValue(isMuted).await()

            // Also update in call history
            val callRef = firebaseDatabase.getReference("callHistory").child(callId)
            callRef.child("participants/$userId/isMuted").setValue(isMuted).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to update status")
        }
    }

    override suspend fun getCallHistory(groupId: String, limit: Int): NetworkResult<List<Call>> {
        return try {
            val callHistoryRef = firebaseDatabase.getReference("callHistory")
            val query = callHistoryRef.orderByChild("groupId").equalTo(groupId).limitToLast(limit)
            val snapshot = query.get().await()

            val calls = mutableListOf<Call>()
            for (childSnapshot in snapshot.children) {
                val call = childSnapshot.getValue(Call::class.java)
                call?.let { calls.add(it) }
            }

            // Sort by start time descending (newest first)
            calls.sortByDescending { it.startTime }

            NetworkResult.Success(calls)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to get call history")
        }
    }*/

    // Call-related method implementations using Firestore
    override suspend fun startVoiceCall(user: User,groupId: String): NetworkResult<Call> {
        return try {
            // Create a document with auto-generated ID
            val callDocRef = db.collection("callHistory").document()
            val callId = callDocRef.id

            val userId = user.userId ?: return NetworkResult.Error(null, "User not authenticated")
            val user = getUserData(userId).data ?: return NetworkResult.Error(null, "Failed to get user data")

            // Create call participant
            val participant = CallParticipant(
                userId = userId,
                userName = user.userName ?: "Unknown User",
                userProfileImgUrl = user.userProfileImgUrl ?: "",
                joinedAt = System.currentTimeMillis()
            )

            // Create new call
            val call = Call(
                callId = callId,
                groupId = groupId,
                startTime = System.currentTimeMillis(),
                initiatedBy = userId,
                initiatorName = user.userName ?: "Unknown User",
                status = "active",
                type = "voice",
                participants = mapOf(userId to participant)
            )

            // Save call to Firestore
            callDocRef.set(call).await()

            // Add to active calls
            db.collection("activeGroupCalls").document(groupId).set(mapOf("callId" to callId)).await()

            // Add participant (could be managed within the call document already, but keeping separate collection for easier queries)
            db.collection("callParticipants").document(callId)
                .collection("users").document(userId).set(participant).await()

            NetworkResult.Success(call)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to start call")
        }
    }

    override suspend fun endVoiceCall(callId: String): NetworkResult<Boolean> {
        return try {
            val callDocRef = db.collection("callHistory").document(callId)
            val callDoc = callDocRef.get().await()

            if (!callDoc.exists()) {
                return NetworkResult.Error(null, "Call not found")
            }

            val call = callDoc.toObject(Call::class.java) ?:
            return NetworkResult.Error(null, "Invalid call data")

            // Update call status
            callDocRef.update(
                mapOf(
                    "status" to "completed",
                    "endTime" to System.currentTimeMillis()
                )
            ).await()

            // Remove from active group calls
            call.groupId?.let { db.collection("activeGroupCalls").document(it).delete().await() }

            // We'll keep the participants collection for history, but you could delete it if needed
            // This is different from the Realtime DB approach where you removed participants

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to end call")
        }
    }

    override suspend fun joinVoiceCall(user: User,callId: String): NetworkResult<Boolean> {
        return try {
            val userId = user.userId ?: return NetworkResult.Error(null, "User not authenticated")
            val user = getUserData(userId).data ?: return NetworkResult.Error(null, "Failed to get user data")

            // Check if call exists and is active
            val callDocRef = db.collection("callHistory").document(callId)
            val callDoc = callDocRef.get().await()

            if (!callDoc.exists() || callDoc.getString("status") != "active") {
                return NetworkResult.Error(null, "Call not active or doesn't exist")
            }

            // Create participant object
            val participant = CallParticipant(
                userId = userId,
                userName = user.userName ?: "Unknown User",
                userProfileImgUrl = user.userProfileImgUrl ?: "",
                joinedAt = System.currentTimeMillis()
            )

            // Add to call participants collection
            db.collection("callParticipants").document(callId)
                .collection("users").document(userId).set(participant).await()

            // Update call document with new participant
            callDocRef.update("participants.$userId", participant).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to join call")
        }
    }

    override suspend fun leaveVoiceCall(user: User,callId: String): NetworkResult<Boolean> {
        return try {
            val userId = user.userId ?: return NetworkResult.Error(null, "User not authenticated")

            // Remove from call participants collection
            db.collection("callParticipants").document(callId)
                .collection("users").document(userId).delete().await()

            // Check if user is the initiator - if so, end the call for everyone
            val callDocRef = db.collection("callHistory").document(callId)
            val callDoc = callDocRef.get().await()

            if (callDoc.getString("initiatedBy") == userId) {
                // If initiator is leaving, end the call
                return endVoiceCall(callId)
            } else {
                // Just update the participant status in call history
                callDocRef.update("participants.$userId", com.google.firebase.firestore.FieldValue.delete()).await()
                NetworkResult.Success(true)
            }
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to leave call")
        }
    }

    override suspend fun getActiveCallInGroup(groupId: String): NetworkResult<Call?> {
        return try {
            val activeCallDoc = db.collection("activeGroupCalls").document(groupId).get().await()

            if (!activeCallDoc.exists()) {
                return NetworkResult.Success(null) // No active call
            }

            val callId = activeCallDoc.getString("callId") ?:
            return NetworkResult.Error(null, "Invalid call reference")

            // Get call details
            val callDoc = db.collection("callHistory").document(callId).get().await()

            if (!callDoc.exists()) {
                return NetworkResult.Success(null) // Call reference exists but call data doesn't
            }

            val call = callDoc.toObject(Call::class.java)
            NetworkResult.Success(call)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to get active call")
        }
    }

    override suspend fun updateParticipantStatus(user: User,callId: String, isMuted: Boolean): NetworkResult<Boolean> {
        return try {
            val userId = user.userId ?: return NetworkResult.Error(null, "User not authenticated")

            // Update mute status in participants collection
            db.collection("callParticipants").document(callId)
                .collection("users").document(userId)
                .update("isMuted", isMuted).await()

            // Also update in call history
            val callDocRef = db.collection("callHistory").document(callId)
            callDocRef.update("participants.$userId.isMuted", isMuted).await()

            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to update status")
        }
    }

    override suspend fun getCallHistory(groupId: String, limit: Int): NetworkResult<List<Call>> {
        return try {
            val callHistoryQuery = db.collection("callHistory")
                .whereEqualTo("groupId", groupId)
                .orderBy("startTime")
                .limitToLast(limit.toLong())
                .get().await()

            val calls = mutableListOf<Call>()
            for (document in callHistoryQuery.documents) {
                val call = document.toObject(Call::class.java)
                call?.let { calls.add(it) }
            }

            // Sort by start time descending (newest first)
            calls.sortByDescending { it.startTime }

            NetworkResult.Success(calls)
        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Failed to get call history")
        }
    }

}