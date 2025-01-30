package com.adsperclick.media.data.dataModels

import com.adsperclick.media.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId : String? = null,
    val userName : String? = null,
    val email : String? = null,
    val password : String? = null,
    val designation : Int = Constants.CLIENT,
    val listOfCompaniesEmployeeWorkFor : List<String>? = null,      // This field is only for employee
    val listOfServicesCustomerUse : List<String>? = null            // This field is only for Clients/Customers
){
    constructor() : this(null, null, null, null)
}
