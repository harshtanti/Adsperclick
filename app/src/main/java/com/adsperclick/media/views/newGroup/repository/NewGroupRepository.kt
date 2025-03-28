package com.adsperclick.media.views.newGroup.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.views.user.pagingsource.UserCommunityPagingSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class NewGroupRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestore: FirebaseFirestore
){

    fun getUserListData(searchQuery: String, userRole: Int): Flow<PagingData<CommonData>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { UserCommunityPagingSource(firestore, searchQuery, userRole) }
        ).flow
    }

    suspend fun getServiceList() = apiService.getServiceList()

    suspend fun createGroup(data: GroupChatListingData, file: File) = apiService.createGroup(data,file)

    suspend fun getCompanyNameData(companyId: String) = apiService.getCompanyNameData(companyId)

    suspend fun addGroupMember(groupId:String,userSet: MutableSet<String>) = apiService.addGroupMember(groupId,userSet)
}