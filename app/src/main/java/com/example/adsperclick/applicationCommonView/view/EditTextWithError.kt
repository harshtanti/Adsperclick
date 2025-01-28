package com.example.adsperclick.applicationCommonView.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.example.adsperclick.databinding.EditTextWithErrorLayoutBinding

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

    fun setErrorText(value: String, view: Int) {
        binding.error.text = value
        setVisibilityOfError(view)
    }

    fun setVisibilityOfError(visibility: Int) {
        binding.error.visibility = visibility
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

    fun getEditView(): AppCompatEditText {
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


}