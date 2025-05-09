package com.adsperclick.media.views.notifications.pagingsource


import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.ROLE.CLIENT
import com.adsperclick.media.utils.Constants.ROLE.EMPLOYEE
import com.adsperclick.media.utils.Constants.ROLE.ADMIN
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class NotificationsPagingSource @Inject constructor(private val db: FirebaseFirestore, private val userRole: Int?) : PagingSource<QuerySnapshot, NotificationMsg>() {

//    @Inject                 // this code won't work  // In this file we can't directly use "Dependency Injection"
//    lateinit var db : FirebaseFirestore   // That's why we fetch FirebaseFirestore instance from ChatRepository


    //    val db = FirebaseFirestore.getInstance()      // This will work but we don't encourage this, we use FirebaseModule
                                                    // to provide Firestore instance, so all our settings is at one place :)

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, NotificationMsg> {
        return try {
            val sentToList = mutableListOf(Constants.SEND_TO.BOTH)
            when(userRole){
                CLIENT, EMPLOYEE -> sentToList.add(userRole)
                ADMIN -> {
                    sentToList.add(CLIENT)
                    sentToList.add(EMPLOYEE)
                }
            }

            val query = db.collection(Constants.DB.NOTIFICATIONS)
                .whereIn("sentTo", sentToList)
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

    fun refreshSource() {
        invalidate() // ✅ Force the PagingSource to refresh when new notifications arrive
    }
}