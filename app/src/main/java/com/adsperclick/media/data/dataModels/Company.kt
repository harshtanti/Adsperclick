package com.adsperclick.media.data.dataModels


import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val companyId : String? = null,
    val companyName : String? = null

){
    constructor() : this(null, null)
}
