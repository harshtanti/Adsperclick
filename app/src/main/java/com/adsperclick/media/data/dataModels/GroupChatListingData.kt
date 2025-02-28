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
    val lastSentMsg : Message? = null) {
    constructor() : this(null, null,null,null,null,null,null)
}

@Serializable
data class GroupUser(
    val userId: String,
    val lastSeen: Long?
){
    constructor() : this("", lastSeen = null)
}
