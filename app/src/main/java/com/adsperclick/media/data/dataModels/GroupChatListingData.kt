package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class GroupChatListingData(
    val groupId : String? = null,
    val groupName : String? = null,
    val groupImgUrl : String? = null,
    val associatedServiceId : String? = null,
    val associatedService : String? = null,
    val listOfUsers : List<GroupUser>? = null,        // people who r member of this group from the "client-company" side
    val lastSentMsg : Message? = null,
    val listOfCallingUsers : List<GroupUser>? = null ) {
    constructor() : this(null, null,null,null,null,null,null,null)
}

@Serializable
data class GroupUser(
    val userId: String,
    val lastSeenMsgId: String?,
    val userName: String? = null,
    val userProfileImgUrl: String? = null,
    val joinedAt: Long = 0,
    val isMuted: Boolean = false,
    val isActive: Boolean = true
){
    constructor() : this("", lastSeenMsgId = null)
}
