package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val serviceId :String ?= null,
    val serviceName :String ?=null
){
    constructor() : this(null, null)
}