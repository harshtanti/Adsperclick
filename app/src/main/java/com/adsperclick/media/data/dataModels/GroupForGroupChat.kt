package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class GroupForGroupChat(
    val groupId : String? = null,
    val groupName : String? = null,
    val groupDpUrl : String? = null,
    val listOfCompanyMembers : List<String>? = null,        // people who r member of this group from the "client-company" side
    val listOfEmployeeMembers : List<String>? = null,       // people who r member of this group from the "Employee" side
    val lastSentMsg : Message? = null) {
    constructor() : this(null, null)
}
