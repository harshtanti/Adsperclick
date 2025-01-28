package com.example.adsperclick.data.dataModels

import com.example.adsperclick.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val companyId : String? = null,
    val companyName : String? = null

){
    constructor() : this(null, null)
}
