package com.adsperclick.media.views.user.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.DB.USERS
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.views.user.pagingsource.CompanyListPagingSource
import com.adsperclick.media.views.user.pagingsource.ServiceListPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class CommunityRepository @Inject constructor(
    private val apiService: ApiService,
    private val db: FirebaseFirestore) {

    suspend fun registerCompany(data:Company) = apiService.registerCompany(data)

    suspend fun registerService(data: Service) = apiService.registerService(data)

    suspend fun registerUser(data: User) = apiService.registerUser(data)

    fun getUserListData(searchQuery: String, userRole: Int): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { UserCommunityPagingSource(db, searchQuery, userRole) }
        ).flow
    }

    fun getCompanyListData(searchQuery: String): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { CompanyListPagingSource(db, searchQuery) }
        ).flow
    }

    fun getServiceListData(searchQuery: String): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ServiceListPagingSource(db, searchQuery) }
        ).flow
    }

    private val _userBlockedStatusLiveData = MutableLiveData<ConsumableValue<NetworkResult<Boolean>>>()
    val userBlockedStatusLiveData : LiveData<ConsumableValue<NetworkResult<Boolean>>> = _userBlockedStatusLiveData

    suspend fun changeUserBlockedStatus(shouldBlock:Boolean, userId: String){       // If should block is true we'll block user else we'll unblock
        try {                                                                       // irrespective of the fact that current user status is blocked or unblocked
            if(shouldBlock){
                db.collection(USERS).document(userId).update("blocked", true).await()
            } else{
                db.collection(USERS).document(userId).update("blocked", false).await()
            }
            _userBlockedStatusLiveData.postValue(ConsumableValue(NetworkResult.Success(shouldBlock)))
        } catch (ex : Exception){
            _userBlockedStatusLiveData.postValue(ConsumableValue(NetworkResult.Error(null, ex.message?:"Unknown error")))
        }
    }

    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun getCompanyList() = apiService.getCompanyList()

    suspend fun deleteService(serviceId: String) = apiService.deleteService(serviceId)

    suspend fun updateUser(userId:String,phoneNumber:String?, file: File?) = apiService.updateUser(userId,phoneNumber,file)

    suspend fun getUserData(userId: String) = apiService.getUserData(userId)

    suspend fun getCompanyData(companyId: String) = apiService.getCompanyData(companyId)

    suspend fun updateCompanyServices(companyId: String, services: List<Service>) = apiService.updateCompanyServices(companyId, services)

}