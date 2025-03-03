package com.adsperclick.media.utils

import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.adsperclick.media.R
import com.adsperclick.media.databinding.DialogChangeFieldBinding
import com.adsperclick.media.databinding.DialogDeleteDetailsBinding

object DialogUtils {

    const val SHOULD_SHOW_BOTH_DIALOG_BUTTONS = 0
    const val SHOULD_SHOW_POSITIVE_DIALOG_BUTTON_ONLY = 1
    const val SHOULD_SHOW_NEGATIVE_DIALOG_BUTTON_ONLY = 2
    const val SHOULD_HIDE_ALL_DIALOG_BUTTONS = 3

    fun showEditTextDialog(
        context: Context,
        dialogButtonClickListener: DialogButtonClickListener,
        tvTitle: String? = null,
        etHint:String? = null,
        firstButton: String? = null,
        secondButton: String? = null
    ) {
        val binding = DialogChangeFieldBinding.inflate(LayoutInflater.from(context))
        val builder = context.let {
            AlertDialog.Builder(it)
                .setView(binding.root)
                .setCancelable(false)
        }

        tvTitle?.let {
            binding.tvTitle.text = it
        }

        etHint?.let {
            binding.etMessage.setHint(it)
        }

        firstButton?.let {
            binding.btnDelete.text = it
        }

        secondButton?.let {
            binding.btnCancel.text = it
        }

        binding.etMessage.setInputType(InputType.TYPE_CLASS_TEXT)

        binding.btnDelete.disableHeaderButton()

        binding.etMessage.getEditView().addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Enable button only if length is 10 and all characters are digits
                val text = s.toString()
                if (text.isNotEmpty()){
                    binding.btnDelete.enableHeaderButton(R.color.Red)
                }else{
                    binding.btnDelete.disableHeaderButton()
                }
            }
        })

        val dialog = builder?.show()
        dialog?.window?.setBackgroundDrawable(
            InsetDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.dialog_background_white
                ), UtilityFunctions.dp2px(context,24)
            )
        )

        binding.btnDelete.setOnClickListener {
            dialog?.dismiss()
            binding.etMessage.getText()?.let { it1 -> dialogButtonClickListener.onPositiveButtonClickedData(it1) }
        }

        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
            dialogButtonClickListener.onNegativeButtonClicked()
        }
    }

    fun showDeleteDetailsDialog(
        context: Context,
        dialogButtonClickListener: DialogButtonClickListener,
        message: String? = null,
        firstButton: String? = null,
        secondButton: String? = null
    ) {
        val binding = DialogDeleteDetailsBinding.inflate(LayoutInflater.from(context))
        val builder = context.let {
            AlertDialog.Builder(it)
                .setView(binding.root)
                .setCancelable(false)
        }

        message?.let {
            binding.tvTitle.text = message
        }

        firstButton?.let {
            binding.btnDelete.text = it
        }

        secondButton?.let {
            binding.btnCancel.text = it
        }

        val dialog = builder?.show()
        dialog?.window?.setBackgroundDrawable(
            InsetDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.dialog_background_white
                ), UtilityFunctions.dp2px(context,24)
            )
        )

        binding.btnDelete.setOnClickListener {
            dialog?.dismiss()
            dialogButtonClickListener.onPositiveButtonClicked()
        }

        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
            dialogButtonClickListener.onNegativeButtonClicked()
        }
    }

    fun showPhoneTextDialog(
        context: Context,
        dialogButtonClickListener: DialogButtonClickListener,
        tvTitle: String? = null,
        etHint: String? = null,
        firstButton: String? = null,
        secondButton: String? = null
    ) {
        val binding = DialogChangeFieldBinding.inflate(LayoutInflater.from(context))
        val builder = context.let {
            AlertDialog.Builder(it)
                .setView(binding.root)
                .setCancelable(false)
        }

        tvTitle?.let {
            binding.tvTitle.text = it
        }

        etHint?.let {
            binding.etMessage.setHint(it)
        }

        firstButton?.let {
            binding.btnDelete.text = it
        }

        secondButton?.let {
            binding.btnCancel.text = it
        }

        // Set input type to phone and max length to 10
        binding.etMessage.setInputType(InputType.TYPE_CLASS_PHONE)
        binding.etMessage.getEditView().filters = arrayOf(InputFilter.LengthFilter(10))

        // Initially disable OK button
        binding.btnDelete.disableHeaderButton()

        // Add text watcher to enable OK button only when 10 digits are entered
        binding.etMessage.getEditView().addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Enable button only if length is 10 and all characters are digits
                val text = s.toString()
                if (text.length == 10 && text.all { it.isDigit() }){
                    binding.btnDelete.enableHeaderButton(R.color.Red)
                }else{
                    binding.btnDelete.disableHeaderButton()
                }
            }
        })

        val dialog = builder?.show()
        dialog?.window?.setBackgroundDrawable(
            InsetDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.dialog_background_white
                ), UtilityFunctions.dp2px(context, 24)
            )
        )

        binding.btnDelete.setOnClickListener {
            dialog?.dismiss()
            binding.etMessage.getText()?.let { it1 -> dialogButtonClickListener.onPositiveButtonClickedData("+91${it1}") }
        }

        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
            dialogButtonClickListener.onNegativeButtonClicked()
        }
    }

    interface DialogButtonClickListener {
        fun onPositiveButtonClicked()
        fun onNegativeButtonClicked()
        fun onCloseButtonClicked()
        fun onPositiveButtonClickedData(data:String)
    }
}