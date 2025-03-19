package com.adsperclick.media.applicationCommonView.bottomsheet

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.datatypes.units.Length
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.adsperclick.media.databinding.LayoutUploadImageDocsBottomsheetBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.visible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class UploadImageDocsBottomSheet : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: LayoutUploadImageDocsBottomsheetBinding
    private var docUploadMethod: UploadMethod = UploadMethod.NOTSELECTED
    private var cameraImageUri: Uri? = null
    private var listener: OnSelectListener? = null
    private var selectedTypeList = listOf<String>()
    private var title:String? = null

    companion object {
        @JvmStatic
        fun createBottomsheet(listener: OnSelectListener,selectedTypeList: ArrayList<String>,title:String?) =
            UploadImageDocsBottomSheet().apply {
                this.listener = listener
                this.selectedTypeList = selectedTypeList
                this.title=title
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
        setUpView()
        setUpClickListener()
        initializeViewAndDismissListener()
    }

    private fun setUpView() {
        binding.tvTitle.text = title?:"Profile Picture"
        val visibilityMap = mapOf(
            Constants.CAMERA_VISIBLE to binding.groupCamera,
            Constants.GALLERY_VISIBLE to binding.groupGallery,
            Constants.PDF_VISIBLE to binding.groupDoc,
            Constants.DELETE_VISIBLE to binding.btnDelete,
            Constants.VIDEO_VISIBLE to binding.groupVideo,
            Constants.CLOSE_VISIBLE to binding.btnClose,
            Constants.HEADING_VISIBLE to binding.tvTitle
        )

        visibilityMap.forEach { (type, view) ->
            if (selectedTypeList.contains(type)) {
                view.visible()
            }
        }
    }

    private fun initializeViewAndDismissListener() {
        val behavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(binding.root.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setUpClickListener() {
        binding.btnDoc.setOnClickListener(this)
        binding.btnClose.setOnClickListener(this)
        binding.btnCamera.setOnClickListener(this)
        binding.btnGallery.setOnClickListener(this)
        binding.btnDelete.setOnClickListener(this)
        binding.btnVideo.setOnClickListener(this)
    }

    // File picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = Constants.APPLICATION_PDF
        fileLauncher.launch(intent)
    }

    // Gallery picker
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.type = Constants.IMAGE
        galleryLauncher.launch(intent)
    }

    // Camera capture
    private fun openCamera() {
        try {
            val file = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                // Grant temporary read/write permissions to the intent
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            // Check if there's a camera app available to handle the intent
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                cameraLauncher.launch(intent)
            } else {
                // No camera app available
                // You can show a toast or dialog here
                Toast.makeText(context,"No Camera app available",Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle exception gracefully
            // You can show a toast or dialog here
        }
    }

    // Video gallery picker
    private fun openVideoGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.type = "video/*"
        videoGalleryLauncher.launch(intent)
    }

    // Video recording
    private fun recordVideo() {
        try {
            val file = File(requireContext().cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1) // High quality
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                videoRecordLauncher.launch(intent)
            } else {
                Toast.makeText(context, "No video recording app available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error starting video recording", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle video gallery selection
    private val videoGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val file = UtilityFunctions.saveFileFromUri(requireContext(), uri) ?: return@let
                listener?.onSelect(file.path, UploadMethod.VIDEO_GALLERY)
                dismiss()
            }
        }
    }

    // Handle video recording
    private val videoRecordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraImageUri?.let { uri ->
                val file = UtilityFunctions.saveFileFromUri(requireContext(), uri) ?: return@let
                listener?.onSelect(file.path, UploadMethod.VIDEO_CAMERA)
                dismiss()
            }
        }
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
                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                requestPermissions(requiredPermissions) { openFilePicker() }

            }
            binding.btnGallery -> {
                docUploadMethod = UploadMethod.GALLERY
                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                requestPermissions(requiredPermissions) { openGallery() }
            }
            binding.btnCamera -> {
                docUploadMethod = UploadMethod.CAMERA
                requestPermissions(arrayOf(Manifest.permission.CAMERA)) { openCamera() }
            }
            binding.btnClose -> dismiss()
            binding.btnDelete -> {
                // Handle delete if needed
                listener?.onSelect("", UploadMethod.NOTSELECTED)
                dismiss()
            }
            binding.btnVideo -> {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Video Source")
                    .setItems(arrayOf("Record Video", "Choose from Gallery")) { _, which ->
                        when (which) {
                            0 -> { // Record Video
                                docUploadMethod = UploadMethod.VIDEO_CAMERA
                                requestPermissions(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        else
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                ) { recordVideo() }
                            }
                            1 -> { // Choose from Gallery
                                docUploadMethod = UploadMethod.VIDEO_GALLERY
                                val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                requestPermissions(requiredPermissions) { openVideoGallery() }
                            }
                        }
                    }
                    .show()
            }
        }
    }

    interface OnSelectListener {
        fun onSelect(option: String, type: UploadMethod)
    }

    enum class UploadMethod {
        CAMERA, GALLERY, PDF, VIDEO_GALLERY, VIDEO_CAMERA, NOTSELECTED
    }
}