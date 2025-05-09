package com.adsperclick.media.data.dataModels

import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId : String? = null,
    val userName : String? = null,
    val email : String? = null,
    val password : String? =null,
    val userProfileImgUrl : String? = null,
    val role : Int? = null,
    var blocked :Boolean ?= null,
    val userAdhaarNumber : String? = null,
    val listOfGroupsAssigned : List<String>? = null,            // List of groups this user is part of
    var listOfServicesAssigned : List<Service>? = null,           // This field can be used for both
    val selfCompanyId : String? = null,
    val selfCompanyName : String? = null,
    val selfCompanyGstNumber : String? = null,      // For clients only, name of company the client is associated with
    val associationDate : String? = null,
    val mobileNo : String? = null,
    var fcmTokenListOfDevices : List<String> ? = null,
    var lastNotificationSeenTime : Long ?= null,
    val userPhoneNumber : String ?= null,
    val agoraUserId : Int ?= null,
    var accountDeleted :Boolean ?= null
){
    constructor() : this(null, null, null, null,
        null, null, null, null, null,
        null, null, null, null,
        null, null,null,null,
        null, null, null)
}

