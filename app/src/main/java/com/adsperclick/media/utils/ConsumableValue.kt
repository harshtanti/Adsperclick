package com.adsperclick.media.utils

import androidx.annotation.UiThread

class ConsumableValue<T>(private val data: T) {
    private var isConsumed = false

    @UiThread
    fun handle(block: ConsumableValue<T>.(T) -> Unit){
        val wasConsumed = isConsumed
        isConsumed = true
        if(!wasConsumed){
            this.block(data)
        }
    }
    @UiThread
    fun ConsumableValue<T>.markUnhandled() {
        isConsumed = false
    }

}