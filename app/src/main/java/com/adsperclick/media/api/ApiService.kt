package com.adsperclick.media.api

import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import java.io.File

interface ApiService {
    suspend fun getServiceList(): NetworkResult<ArrayList<Service>>
    suspend fun getCompanyList(): NetworkResult<ArrayList<Company>>
    suspend fun registerCompany(data: Company): NetworkResult<Company>
    suspend fun registerService(data: Service): NetworkResult<Service>
    suspend fun registerUser(data: User): NetworkResult<User>
    suspend fun createGroup(data: GroupChatListingData,file: File): NetworkResult<Boolean>
    suspend fun deleteService(serviceId: String): NetworkResult<Boolean>
    suspend fun updateUser(userId:String,phoneNumber:String?, file: File?): NetworkResult<Boolean>
    suspend fun getUserData(userId: String): NetworkResult<User>
    suspend fun getCompanyData(companyId: String): NetworkResult<Company>
    suspend fun updateCompanyServices(companyId: String, services: List<Service>): NetworkResult<Boolean>
    suspend fun getCompanyNameData(companyName: String): NetworkResult<Company>
    suspend fun getMultipleUsers(userIds: List<String>): NetworkResult<List<User>>
    suspend fun updateGroupProfile(groupId:String,groupName:String?, file: File?): NetworkResult<Boolean>
    suspend fun removeUserFromGroup(userId: String, groupId: String): NetworkResult<String>
    suspend fun getGroupDetails(groupId: String): NetworkResult<GroupChatListingData>
    suspend fun addGroupMember(groupId: String, userSet: MutableSet<String>): NetworkResult<Boolean>

}