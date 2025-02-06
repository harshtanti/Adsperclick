package com.adsperclick.media.utils

import java.util.Locale

object Validate {

    fun String.toInitials(): String {
        val charBuffer = StringBuilder()
        val s = this.split("(\\s)+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (values in s) {
            if (charBuffer.length < 2 && values.isNotEmpty()) {
                charBuffer.append(values[0])
            }
        }
        return charBuffer.toString().uppercase(Locale.ROOT)
    }
}