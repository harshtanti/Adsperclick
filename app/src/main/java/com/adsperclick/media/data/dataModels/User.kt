package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId : String? = null,
    val userProfileImgUrl : String? = null,
    val userName : String? = null,
    val email : String? = null,
    val password : String? = null,
    val role : Int = Constants.CLIENT,
    val isBlocked :Boolean= false,
    val userAdhaarNumber : String? = null,
    val listOfGroupsAssigned : List<String>? = null,            // List of groups this user is part of
    val listOfServicesAssigned : List<String>? = null,           // This field can be used for both
    val selfCompanyName : String? = null,                      // For clients only, name of company the client is associated with
    val associationDate : String? = null,
    val mobileNo : String? = null,
    val fcmTokenListOfDevices : List<String> ? = null,
    val lastNotificationSeenTime : Long ?= null
){
    constructor() : this(null, null, null, null)
}
