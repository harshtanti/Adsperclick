package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class Call(
    val callId: String? = null,
    val groupId: String? = null,
    val startTime: Long = 0,
    val endTime: Long? = null,
    val initiatedBy: String? = null,
    val initiatorName: String? = null,
    val status: String = "active", // "active", "completed", "missed", "declined"
    val type: String = "voice",
    val participants: Map<String, CallParticipant> = emptyMap()
)
