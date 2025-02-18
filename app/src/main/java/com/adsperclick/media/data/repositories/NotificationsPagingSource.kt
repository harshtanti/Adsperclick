package com.adsperclick.media.data.repositories

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class NotificationsPagingSource() : PagingSource<QuerySnapshot, NotificationMsg>() {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, NotificationMsg> {
        return try {
            val query = db.collection(Constants.DB.NOTIFICATIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Newest first
                .limit(params.loadSize.toLong())

            val currentPage = params.key ?: query.get().await() // First page

            val lastDocument = currentPage.documents.lastOrNull()

            val nextPage = lastDocument?.let {
                query.startAfter(lastDocument).get().await()
            }

            val notifications = currentPage.toObjects(NotificationMsg::class.java)

            LoadResult.Page(
                data = notifications,
                prevKey = null,  // No previous page
                nextKey = nextPage  // Load next page
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, NotificationMsg>): QuerySnapshot? {
        return null  // Refresh starts from the beginning
    }
}