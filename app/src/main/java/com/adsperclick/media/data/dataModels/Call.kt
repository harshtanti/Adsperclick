package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants.CALL
import kotlinx.serialization.Serializable

@Serializable
data class Call(
    val callId: String? = null,     // document ID, ID of this particular call in DB :)
    val groupId: String? = null,    // group id
    val startTime: Long? = null,
    val endTime: Long? = null,
    val initiatorId: String? = null,
    val initiatorName: String? = null,
    val status: String = CALL.STATUS.ONGOING, // "active", "completed"
    val type: String = CALL.TYPE.VOICE, // "voice" , "videoCall"
    val participants: Map<String, CallParticipant> = emptyMap() // Map of userIds to CallParticipant objects
)
