package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId : String? = null,
    val userName : String? = null,
    val email : String? = null,
    val password : String? = null,
    val designation : Int = Constants.CLIENT
){
    constructor() : this(null, null, null, null)
}
