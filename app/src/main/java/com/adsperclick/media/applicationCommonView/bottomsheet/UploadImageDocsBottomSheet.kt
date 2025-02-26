package com.adsperclick.media.applicationCommonView.bottomsheet

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.LayoutUploadImageDocsBottomsheetBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.views.user.bottomsheet.ServiceBottomSheetFragment
import com.adsperclick.media.views.user.bottomsheet.ServiceBottomSheetFragment.MultiSelectListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

private const val ARG_PARAM1 = "param1"

class UploadImageDocsBottomSheet : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: LayoutUploadImageDocsBottomsheetBinding
    private var docUploadMethod: UploadMethod = UploadMethod.NOTSELECTED
    private var cameraImageUri: Uri? = null
    private var listener: OnSelectListener? = null
    private var selectedTypeList = arrayListOf<String>()

    companion object {
        @JvmStatic
        fun newInstance(param1: String,listener: OnSelectListener,selectedTypeList:ArrayList<String>) =
            UploadImageDocsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
                this.listener=listener
                this.selectedTypeList=selectedTypeList
            }
    }

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
        setUpClickListener()
    }

    private fun setUpClickListener() {
        binding.btnDoc.setOnClickListener(this)
        binding.btnClose.setOnClickListener(this)
        binding.btnCamera.setOnClickListener(this)
        binding.btnGallery.setOnClickListener(this)
        binding.btnDelete.setOnClickListener(this)
    }

    // File picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = Constants.APPLICATION_PDF
        fileLauncher.launch(intent)
    }

    // Gallery picker
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // Camera capture
    private fun openCamera() {
        val file = File(requireContext().cacheDir, "temp_photo.jpg")
        cameraImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        cameraLauncher.launch(intent)
    }

    // Permission check & request
    private fun requestPermissions(requiredPermissions: Array<String>, action: () -> Unit) {
        val notGrantedPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isEmpty()) {
            action()
        } else {
            permissionLauncher.launch(notGrantedPermissions.toTypedArray())
        }
    }

    // Handle file selection
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = UtilityFunctions.saveFileFromUri(requireContext(), uri) ?: return@let
                listener?.onSelect(file.path, UploadMethod.PDF)
                dismiss()
            }
        }
    }

    // Handle gallery selection
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = UtilityFunctions.saveFileFromUri(requireContext(), uri) ?: return@let
                listener?.onSelect(file.path, UploadMethod.GALLERY)
                dismiss()
            }
        }
    }

    // Handle camera capture
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraImageUri?.let { uri ->
                val file = UtilityFunctions.saveFileFromUri(requireContext(), uri) ?: return@let
                listener?.onSelect(file.path, UploadMethod.CAMERA)
                dismiss()
            }
        }
    }

    // Handle permissions
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            when (docUploadMethod) {
                UploadMethod.CAMERA -> openCamera()
                UploadMethod.GALLERY -> openGallery()
                UploadMethod.PDF -> openFilePicker()
                else -> {}
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnDoc -> {
                docUploadMethod = UploadMethod.PDF
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) { openFilePicker() }
            }
            binding.btnGallery -> {
                docUploadMethod = UploadMethod.GALLERY
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) { openGallery() }
            }
            binding.btnCamera -> {
                docUploadMethod = UploadMethod.CAMERA
                requestPermissions(arrayOf(Manifest.permission.CAMERA)) { openCamera() }
            }
            binding.btnClose -> dismiss()
            binding.btnDelete -> {
                // Handle delete if required
            }
        }
    }


    interface OnSelectListener {
        fun onSelect(option: String, type: UploadMethod)
    }

    enum class UploadMethod {
        CAMERA, GALLERY, PDF, NOTSELECTED
    }


}
