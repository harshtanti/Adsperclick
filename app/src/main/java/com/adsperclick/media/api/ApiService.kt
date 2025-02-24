package com.adsperclick.media.api

import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User

interface ApiService {
    suspend fun getServiceList(): NetworkResult<ArrayList<Service>>
    suspend fun getCompanyList(): NetworkResult<ArrayList<Company>>
    suspend fun registerCompany(data: Company): NetworkResult<Company>
    suspend fun registerService(data: Service): NetworkResult<Service>
    suspend fun registerUser(data: User): NetworkResult<User>
    suspend fun createGroup(data: GroupChatListingData): NetworkResult<GroupChatListingData>
}