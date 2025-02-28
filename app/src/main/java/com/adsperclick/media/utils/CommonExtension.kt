package com.adsperclick.media.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.adsperclick.media.R

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.enable() {
    isEnabled = true
    isClickable = true
    alpha = 1.0f
}

fun View.disable(opacity: Float? = null) {
    isEnabled = false
    isClickable = false
    alpha = opacity ?: 0.6f
}

fun Context.inflate(res: Int, parent: ViewGroup? = null): View {
    return LayoutInflater.from(this).inflate(res, parent, false)
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: View) {
    if (view.requestFocus()) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Context.showToast(message: String?) {
    message?.let {
        Toast.makeText(
            this, it, Toast.LENGTH_LONG
        ).show()
    }
}

fun View.disableSubmitButton() {
    backgroundTintList = ContextCompat.getColorStateList(
        context, R.color.disabled_color
    )
    isEnabled = false
}

fun View.enableSubmitButton() {
    backgroundTintList = ContextCompat.getColorStateList(
        context, R.color.blue_common_button
    )
    isEnabled = true
}

fun View.disableHeaderButton() {
    if (this is TextView) {
        setTextColor(ContextCompat.getColor(context, R.color.disabled_color))
    }
    isEnabled = false
}

fun View.enableHeaderButton() {
    if (this is TextView) {
        setTextColor(ContextCompat.getColor(context, R.color.blue_common_button))
    }
    isEnabled = true
}

fun View.enableHeaderButton(value: Int) {
    if (this is TextView) {
        setTextColor(ContextCompat.getColor(context, value))
    }
    isEnabled = true
}