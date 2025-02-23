package com.adsperclick.media.views.user.pagingsource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adsperclick.media.data.dataModels.CommonData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ServiceListPagingSource @Inject constructor(
    private val db: FirebaseFirestore,
    private val searchQuery: String, // Search term for userName or email or others
) : PagingSource<QuerySnapshot, CommonData>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, CommonData> {
        return try {

            var query: Query = db.collection("services")
                .orderBy("serviceName")  // Ordering by userId
                .limit(params.loadSize.toLong())

            // Apply search query filtering if it's provided (for userName or email)
            if (searchQuery.isNotEmpty()) {
                query = query
                    .startAt(searchQuery)  // Match start of userName
                    .endAt(searchQuery + "\uf8ff")  // Match up to the end of userName (for prefix search)
            }

            val currentPage = params.key ?: query.get().await()

            val lastDocument = currentPage.documents.lastOrNull()

            val nextPage = lastDocument?.let {
                query.startAfter(lastDocument).get().await()
            }

            val userList = currentPage.documents.map { document ->
                CommonData(
                    id = document.getString("serviceId"),
                    name = document.getString("serviceName"),
                )
            }

            // Return the loaded page
            LoadResult.Page(
                data = userList,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)  // Return error result
        }
    }

    // To refresh the data, return null for now (you can customize later if needed)
    override fun getRefreshKey(state: PagingState<QuerySnapshot, CommonData>): QuerySnapshot? {
        return null
    }
}