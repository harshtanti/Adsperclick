package com.adsperclick.media.views.call.repository

import android.util.Log
import com.adsperclick.media.api.ApiService
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.Call
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.utils.Constants.DB.GROUPS
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class CallRepository @Inject constructor(private val apiService: ApiService) {

    /*// Observe call participants in real-time
    fun observeCallParticipants(callId: String): Flow<List<CallParticipant>> = callbackFlow {
        val participantsRef = firebaseDatabase.getReference("callParticipants").child(callId)

        val listener = participantsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val participants = mutableListOf<CallParticipant>()
                for (childSnapshot in snapshot.children) {
                    val participant = childSnapshot.getValue(CallParticipant::class.java)
                    participant?.let { participants.add(it) }
                }
                trySend(participants)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        awaitClose {
            participantsRef.removeEventListener(listener)
        }
    }

    // Observe call details in real-time
    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val callRef = firebaseDatabase.getReference("callHistory").child(callId)

        val listener = callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val call = snapshot.getValue(Call::class.java)
                trySend(call)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        awaitClose {
            callRef.removeEventListener(listener)
        }
    }*/

    suspend fun getUserCallToken(groupId: String) = apiService.getUserCallToken(groupId)
    suspend fun listenParticipantChanges(groupId: String): Flow<NetworkResult<Call>> = apiService.listenParticipantChanges(groupId)
    suspend fun removeUserFromCall(groupData: GroupChatListingData, userData: User) = apiService.removeUserFromCall(groupData, userData)
    suspend fun updateUserCallStatus(groupId: String, userId: String, isMuted: Boolean?, isSpeaking: Boolean?) = apiService.updateUserCallStatus(groupId, userId, isMuted, isSpeaking)
}