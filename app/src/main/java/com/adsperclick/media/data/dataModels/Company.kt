package com.adsperclick.media.data.dataModels


import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val companyId : String? = null,
    val companyName : String? = null,
    val gstNumber : String? = null,
    val listOfServices : List<Service>? = null
    /*val listOfCompanyMembers : List<String>? = null,*/        // "id" of people who are member of this company

){
    constructor() : this(null, null,null,null)
}

