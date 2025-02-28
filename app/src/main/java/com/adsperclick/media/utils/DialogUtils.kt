package com.adsperclick.media.utils

import android.content.Context
import android.graphics.drawable.InsetDrawable
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

    interface DialogButtonClickListener {
        fun onPositiveButtonClicked()
        fun onNegativeButtonClicked()
        fun onCloseButtonClicked()
        fun onPositiveButtonClickedData(data:String)
    }
}