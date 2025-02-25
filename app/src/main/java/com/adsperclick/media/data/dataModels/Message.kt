package com.adsperclick.media.data.dataModels


import androidx.room.Entity
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    val msgId : String? = null,
    val message: String? = null,
    val senderId : String? = null,    // userId of sender
    val senderName: String? = null,
    val senderRole: Int? = null,
    val msgType :Int ?= null,
    val groupId: String ?= null,
    val timestamp: Long ?= null
){
    constructor() : this(
        null, null, null, null,
        null, null, null, null
    )

    fun toMapForFirestore() : Map<String, Any> {        // We converting a Message class
        return mapOf(                                   // object to hashMap and then storing
            "msgId" to (msgId ?: ""),                   // it on server because we want to use
            "message" to (message ?: ""),               // "Server time-stamp" and not device's timestamp
            "senderId" to (senderId ?: ""),
            "senderName" to (senderName ?: ""),
            "senderRole" to (senderRole ?: -1),
            "msgType" to (msgType ?: -1),
            "groupId" to (groupId ?: ""),
            "timestamp" to FieldValue.serverTimestamp() // 🔹 Firestore timestamp (This field will be filled on server when data is being saved there
        )
    }
}
