package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.TimestampSerializer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable

@Serializable
data class NotificationMsg(
    val notificationId : String? = null,
    val notificationTitle : String? = null,
    val notificationDescription : String? = null,
    val sentTo : Int = Constants.SEND_TO.EMPLOYEE,
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Timestamp ?=null
){
    constructor() : this( null, null, null, Constants.SEND_TO.EMPLOYEE, null)

    fun mapifyForFirestoreTimestamp():Map<String, Any?>{
        return hashMapOf(
            "notificationId" to this.notificationId,
            "notificationTitle" to this.notificationTitle,
            "notificationDescription" to this.notificationDescription,
            "sentTo" to sentTo,
            "timestamp" to FieldValue.serverTimestamp()
        )
    }
}
