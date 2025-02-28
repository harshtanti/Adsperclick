package com.adsperclick.media.views.user.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.views.user.pagingsource.CompanyListPagingSource
import com.adsperclick.media.views.user.pagingsource.ServiceListPagingSource
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
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

    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun getCompanyList() = apiService.getCompanyList()

    suspend fun deleteService(serviceId: String) = apiService.deleteService(serviceId)
}