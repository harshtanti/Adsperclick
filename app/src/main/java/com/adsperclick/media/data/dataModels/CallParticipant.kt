package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class CallParticipant(
    val userId: String? = null,
    val userName: String? = null,
    val userProfileImgUrl: String? = null,
    val joinedAt: Long = 0,
    val muteOn: Boolean = false,
    val speakerOn: Boolean = false
)