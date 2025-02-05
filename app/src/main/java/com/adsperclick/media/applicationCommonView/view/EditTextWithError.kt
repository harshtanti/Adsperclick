package com.adsperclick.media.applicationCommonView.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.adsperclick.media.databinding.EditTextWithErrorLayoutBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditeTextWithError : ConstraintLayout {

    private lateinit var binding: EditTextWithErrorLayoutBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init() {
        binding = EditTextWithErrorLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    }


    fun setErrorText(value: String?) {
        binding.inputLayout.error = value
    }

    fun removeErrorText(){
        binding.inputLayout.error = null
    }


    fun setText(value: String) {
        binding.edittext.setText(value)
    }

    fun getText(): String? {
        return binding.edittext.text?.toString()
    }

    fun setHint(value: Int) {
        binding.inputLayout.setHint(value)
    }

    fun getEditView(): TextInputEditText {
        return binding.edittext
    }

    fun setInputType(inputType: Int) {
        binding.edittext.inputType = inputType
    }

    fun setTextLimit(value: Int) {
        binding.edittext.addTextChangedListener {
            if (it.toString().length > value) {
                binding.edittext.setText(binding.edittext.text.toString().substring(0, value))
                binding.edittext.setSelection(value)
            }
        }
    }

    fun setEditTextEnable(value: Boolean) {
        binding.edittext.isEnabled = value
    }

    fun setPlaceHolderText(value: String){
        binding.inputLayout.placeholderText = value
    }

    fun setStartIcon(context: Context, iconRes: Int) {
        binding.inputLayout.startIconDrawable = AppCompatResources.getDrawable(context,iconRes)
    }

    fun enablePasswordToggle() {
        binding.inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        binding.edittext.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }


}