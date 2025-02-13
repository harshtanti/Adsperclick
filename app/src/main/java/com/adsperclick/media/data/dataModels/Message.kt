package com.adsperclick.media.data.dataModels


import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val msgId : String? = null,
    val message: String? = null,
    val senderId : String? = null,    // userId of sender
    val senderName: String? = null,
    val senderRole: Int? = null,
    val msgType :Int ?= null,
    val timestamp: Long = System.currentTimeMillis()
){
    constructor() : this(
        null, null, null, null,
        null, null, System.currentTimeMillis()
    )
}
