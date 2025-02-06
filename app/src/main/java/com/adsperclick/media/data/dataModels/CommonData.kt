package com.adsperclick.media.data.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class CommonData(
    val id : String? = null,
    val name : String? = null,
    val tagName: String? = null,
    val imgUrl: String? = null
)
