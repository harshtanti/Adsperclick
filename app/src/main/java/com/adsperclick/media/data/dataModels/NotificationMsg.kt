package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants.EMPLOYEE
import kotlinx.serialization.Serializable

@Serializable
data class NotificationMsg(
    val notificationId : String? = null,
    val notificationTitle : String? = null,
    val notificationDescription : String? = null,
    val sentTo : Int = EMPLOYEE,
    val timestamp: Long = System.currentTimeMillis()
){
    constructor() : this( null, null, null, EMPLOYEE, System.currentTimeMillis())
}
