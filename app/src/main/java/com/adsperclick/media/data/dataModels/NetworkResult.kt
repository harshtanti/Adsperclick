package com.adsperclick.media.data.dataModels

sealed class NetworkResult<T>(val data: T? = null, val message: String? = null) {

    class Success<T>(data: T) : NetworkResult<T>(data)
    class Error<T>(data:T? = null, msg: String) : NetworkResult<T>(data, msg)
    class Loading<Nothing> : NetworkResult<Nothing>()
}

