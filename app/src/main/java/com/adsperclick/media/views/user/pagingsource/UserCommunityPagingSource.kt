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

class UserCommunityPagingSource @Inject constructor(
    private val db: FirebaseFirestore,
    private val searchQuery: String, // Search term for userName or email or others
    private val userRole: Int // User role filter (e.g., 4)
) : PagingSource<QuerySnapshot, CommonData>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, CommonData> {
        return try {
            // Construct the basic query for filtering by role and blocked status
            var query: Query = db.collection("users")
                .whereEqualTo("role", userRole)  // Filter by role
                .orderBy("userName")  // Ordering by userId
                .limit(params.loadSize.toLong())

            // Apply search query filtering if it's provided (for userName or email)
            if (searchQuery.isNotEmpty()) {
                query = query
                    .startAt(searchQuery)  // Match start of userName
                    .endAt(searchQuery + "\uf8ff")  // Match up to the end of userName (for prefix search)
            }

            // Get the current page data
            val currentPage = params.key ?: query.get().await()

            val lastDocument = currentPage.documents.lastOrNull()

            // Prepare for next page query
            val nextPage = lastDocument?.let {
                query.startAfter(lastDocument).get().await()
            }

            val userList = currentPage.documents
                .filter { it.get("accountDeleted") == null }
                .map { document ->
                if (userRole==1){
                    CommonData(
                        id = document.getString("userId"),
                        name = document.getString("userName"),
                        tagName = document.getString("selfCompanyName"),
                        imgUrl = document.getString("userProfileImgUrl")
                    )
                }else {
                    CommonData(
                        id = document.getString("userId"),
                        name = document.getString("userName"),
                        tagName = "Employee",
                        imgUrl = document.getString("userProfileImgUrl")
                    )
                }
            }

            LoadResult.Page(
                data = userList,
                prevKey = null,  // No previous page
                nextKey = nextPage  // Load next page
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