package com.adsperclick.media.data.repositories

import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CommunityRepository @Inject constructor() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDb = FirebaseFirestore.getInstance()

    suspend fun registerCompany(data:Company): NetworkResult<Company> {
        return try {

            var companyId: String? = null
            val companyNameInDataBase: String?

            val companyQuery = firebaseDb.collection(Constants.DB.COMPANY)
                .whereEqualTo("gstNumber", data.gstNumber)
                .get()
                .await()

            if (!companyQuery.isEmpty) {
                // Company exists, fetch its ID
                companyId = companyQuery.documents.first().id
                companyNameInDataBase = companyQuery.documents.first().getString("companyName")
                if(companyNameInDataBase!=data.companyName){
                    return NetworkResult.Error(null, "GST number is already registered with $companyNameInDataBase. Please Try Again")
                }
            }

            val newCompanyRef = firebaseDb.collection(Constants.DB.COMPANY).document()
            companyId = newCompanyRef.id

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

    suspend fun registerService(data:Service): NetworkResult<Service> {
        return try {

            val newCompanyRef = firebaseDb.collection(Constants.DB.SERVICE).document()
            val serviceId = newCompanyRef.id

            val service = Service(
                serviceId = serviceId,
                serviceName = data.serviceName
            )

            // Save company details in Firestore
            newCompanyRef.set(service).await()

            NetworkResult.Success(service)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Service registration failed")
        }
    }

    suspend fun register(data: User): NetworkResult<User> {
        return try {
            var companyId: String? = null
            val companyNameInDataBase: String?

            // 1️⃣ Check if the company GST exists (only if a GST number is provided)
            if (!data.selfCompanyGstNumber.isNullOrEmpty()) {
                val companyQuery = firebaseDb.collection(Constants.DB.COMPANY)
                    .whereEqualTo("gstNumber", data.selfCompanyGstNumber)
                    .get()
                    .await()

                if (!companyQuery.isEmpty) {
                    // Company exists, fetch its ID
                    companyId = companyQuery.documents.first().id
                    companyNameInDataBase = companyQuery.documents.first().getString("companyName")
                    if(companyNameInDataBase!=data.selfCompanyName){
                        return NetworkResult.Error(null, "GST number is already registered with $companyNameInDataBase. Please Try Again")
                    }
                } else {
                    // Company does not exist, create a new company
                    val newCompanyRef = firebaseDb.collection(Constants.DB.COMPANY).document()
                    companyId = newCompanyRef.id

                    val company = Company(
                        companyId = companyId,
                        companyName = data.selfCompanyName,
                        gstNumber = data.selfCompanyGstNumber
                    )

                    newCompanyRef.set(company).await()
                }
            }

            // 2️⃣ Create the user in Firebase Auth
            val result = data.email?.let { email ->
                data.password?.let { password ->
                    firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                }
            }

            val firebaseUser = result?.user ?: return NetworkResult.Error(null, "User authentication failed")

            // 3️⃣ Create User Object with fetched/created company ID
/*            val user = User(
                userId = firebaseUser.uid,
                userName = data.userName,
                email = data.email,
                password = data.password,
                userProfileImgUrl = data.userProfileImgUrl,
                role = data.role,
                isBlocked = data.isBlocked,
                userAdhaarNumber = data.userAdhaarNumber,
                listOfGroupsAssigned = data.listOfGroupsAssigned,
                listOfServicesAssigned = data.listOfServicesAssigned,
                selfCompanyId = companyId,  // Assigned the fetched/created company ID
                selfCompanyName = data.selfCompanyName,
                selfCompanyGstNumber = data.selfCompanyGstNumber,
                associationDate = data.associationDate,
                mobileNo = data.mobileNo,
                fcmTokenListOfDevices = data.fcmTokenListOfDevices,
                lastNotificationSeenTime = data.lastNotificationSeenTime
            )*/

            val user = data.copy(userId = firebaseUser.uid, selfCompanyId = companyId)

            // 4️⃣ Save User in Firestore
            firebaseDb.collection(Constants.DB.USERS).document(firebaseUser.uid).set(user).await()

            NetworkResult.Success(user)

        } catch (e: Exception) {
            NetworkResult.Error(null, e.message ?: "Registration failed")
        }
    }
}