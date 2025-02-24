package com.adsperclick.media.applicationCommonView.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.databinding.LayoutUploadImageDocsBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UploadImageDocsBottomSheet(val listener: OnSelectListener): BottomSheetDialogFragment() {

    private lateinit var binding: LayoutUploadImageDocsBottomsheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutUploadImageDocsBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    interface OnSelectListener {
        fun onSelect(option: String, type: UploadMethod)
    }

    enum class UploadMethod {
        CAMERA,GALLERY,PDF,NOTSELECTED
    }
}