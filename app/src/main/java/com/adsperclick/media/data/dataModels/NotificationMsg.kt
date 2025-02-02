package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class NotificationMsg(
    val notificationId : String? = null,
    val notificationTitle : String? = null,
    val notificationDescription : String? = null,
    val isRead : Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
){
    constructor() : this(null, null, null, false, System.currentTimeMillis())
}
