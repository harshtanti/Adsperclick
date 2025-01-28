package com.adsperclick.media.data.dataModels


import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val msgId : String? = null,
    val message : String? = null,
    val senderId : String? = null,
    val senderName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
){
    constructor() : this(null, null, null, null, System.currentTimeMillis())
}