package com.adsperclick.media.data.dataModels


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable


// NOTE!!!! WHEN MAKING ANY CHANGES IN MESSAGE OBJECT, ADDING OR REMOVING FIELDS,
// ALSO UPDATE "    fun DocumentSnapshot.toMessage(): Message? {". in ChatRepository
// Otherwise no change will be reflected!!! Also don't forget to update the "toMapForFirestore" here,
@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey var msgId : String = "null string",
    var message: String? = null,
    var senderId : String? = null,    // userId of sender
    var senderName: String? = null,
    var senderRole: Int? = null,
    var msgType :Int ?= null,
    var groupId: String ?= null,
    var timestamp: Long ?= null
){
    constructor() : this(
        "null string", null, null, null,
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

// shared pref = serverTime - currentTimeOfDevice

// serverSideTime = pre + currentTimeOfDevice