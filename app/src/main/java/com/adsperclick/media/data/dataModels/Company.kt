package com.adsperclick.media.data.dataModels


import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val companyId : String? = null,
    val companyName : String? = null,
    val gstNumber : String? = null,
    /*val listOfCompanyMembers : List<String>? = null,*/        // "id" of people who are member of this company
    val listOfServices : List<String>? = null
){
    constructor() : this(null, null)
}
