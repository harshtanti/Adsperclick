package com.adsperclick.media.api

import android.net.Uri
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class ApiServiceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storageRef: StorageReference
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
            val groupCollection = FirebaseFirestore.getInstance().collection("groups")

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
                val userCollection = FirebaseFirestore.getInstance().collection("users")

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
            val groupsWithService = FirebaseFirestore.getInstance()
                .collection("groups")
                .whereEqualTo("associatedServiceId", serviceId)
                .get()
                .await()

            if (!groupsWithService.isEmpty) {
                return NetworkResult.Error(false, "Cannot delete service: It is being used by one or more groups")
            }

            // If not used, delete the service
            FirebaseFirestore.getInstance()
                .collection("services")
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
            val userCollection = FirebaseFirestore.getInstance().collection("users")
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

}