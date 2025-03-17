package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class Call(
    val callId: String? = null, // channel name
    val groupId: String? = null, // group id
    val startTime: Long = 0,
    val endTime: Long? = null,
    val initiatedBy: String? = null,
    val initiatorName: String? = null,
    val status: String = "active", // "active", "completed"
    val type: String = "voice", // "voice" , "videoCall"
    val participants: Map<String, CallParticipant> = emptyMap() // Map of userIds to CallParticipant objects
)
