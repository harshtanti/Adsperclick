package com.adsperclick.media.views.setting.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.applicationCommonView.bottomsheet.UploadImageDocsBottomSheet
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.disableHeaderButton
import com.adsperclick.media.utils.disableSubmitButton
import com.adsperclick.media.utils.enableHeaderButton
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.login.MainActivity
import com.adsperclick.media.views.login.viewModels.AuthViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentSettingBinding
    private var selectedImageFile: File? = null
    private val selectedTypeList = arrayListOf(Constants.CAMERA_VISIBLE,Constants.GALLERY_VISIBLE,Constants.DELETE_VISIBLE)
    private var user:User?=null
    private val phoneNumber:String?=null


    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = tokenManager.getUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpHeader()
        setUpVisibility()
        setUserDetails()
        validateSubmitButton()
        setUpListeners()
    }

    private fun setUpHeader(){
        binding.header.tvTitle.text = "Setting"
    }

    private fun areFixedDetailsChanged():Boolean{
        if(selectedImageFile!=null){
            return true
        }
        if (phoneNumber!=null){
            return true
        }
        return false
    }

    private fun validateSubmitButton() {
        if (areFixedDetailsChanged()) {
            binding.header.btnSave.enableHeaderButton()
        } else {
            binding.header.btnSave.disableHeaderButton()
        }
    }

    private fun setUpVisibility(){
        binding.cvServices.gone()
    }

    private fun setUserDetails(){
        user?.let {
            it.userProfileImgUrl?.let { imageUrl ->
                UtilityFunctions.loadImageWithGlide(
                    binding.imgProfileDp.context,
                    binding.imgProfileDp,
                    imageUrl
                )
            } ?: run {
                UtilityFunctions.setInitialsDrawable(
                    binding.imgProfileDp,
                    user?.userName
                )
            }
            it.associationDate?.let { data->
                binding.tvAssociationDate.text = data
            } ?: run {
                binding.tvAssociationDate.text = "N.A."
            }
        }


    }

    private fun setUpListeners(){
        binding.btnLogout.setOnClickListener(this)
        binding.btnImage.setOnClickListener(this)
        binding.cvPhone.setOnClickListener(this)
        binding.header.btnSave.setOnClickListener(this)
        binding.header.btnBack.setOnClickListener(this)
    }

    private val uploadOnSelectListener = object : UploadImageDocsBottomSheet.OnSelectListener{
        override fun onSelect(option: String, type: UploadImageDocsBottomSheet.UploadMethod) {
            when (type) {
                UploadImageDocsBottomSheet.UploadMethod.CAMERA,
                UploadImageDocsBottomSheet.UploadMethod.GALLERY -> {
                    // Check if the file path is valid
                    if (option.isNotEmpty()) {
                        val imageFile = File(option)
                        if (imageFile.exists()) {
                            // Just store the file for later upload
                            selectedImageFile = imageFile

                            // Load image preview
                            loadImageIntoView(imageFile)
                            validateSubmitButton()
                        }
                    }
                }
                else -> {
                    // Reset the image if NOTSELECTED or for error cases
                    binding.imgProfileDp.setImageResource(R.drawable.baseline_person_24) // Replace with your default image
                }
            }
        }

    }

    // Helper method to load the image into the ShapeableImageView
    private fun loadImageIntoView(imageFile: File) {
        try {
            // Option 1: Using Bitmap (simple but may cause OutOfMemoryError for large images)
            /*val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            binding.imgProfileDp.setImageBitmap(bitmap)*/

            // Option 2: Using Glide (recommended for better memory management)
            // Uncomment the below code if you're using Glide
            Glide.with(requireContext())
                .load(imageFile)
                .centerCrop()
                .placeholder(R.drawable.baseline_person_24) // Replace with your placeholder
                .error(R.drawable.baseline_person_24) // Replace with your error image
                .into(binding.imgProfileDp)
            /*context?.let { UtilityFunctions.setImageOnImageViewWithGlide(it,imageFile,binding.imgProfileDp) }*/

            // Option 3: Using Picasso
            /*
            Picasso.get()
                .load(imageFile)
                .centerCrop()
                .fit()
                .placeholder(R.drawable.default_profile) // Replace with your placeholder
                .error(R.drawable.default_profile) // Replace with your error image
                .into(binding.imgProfileDp)
            */

            // Save the image path to your data model or preferences if needed
            // For example: viewModel.setProfileImagePath(imageFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the error case
            binding.imgProfileDp.setImageResource(R.drawable.baseline_person_24) // Replace with your default image
        }
    }

    private fun updateProfile(){
//        val user = User(
//            profileImagePath = selectedImageFile?.absolutePath,
//        )
    }

    override fun onClick(v: View?) {
        when(v){
            binding.header.btnSave -> {
                if(areFixedDetailsChanged()){
                    updateProfile()
                }
            }
            binding.btnImage -> {
                val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                    uploadOnSelectListener,selectedTypeList)
                bottomSheet.show(childFragmentManager, bottomSheet.tag)
            }
            binding.btnLogout -> {
                authViewModel.signOut()

                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
            binding.header.btnBack -> {
                findNavController().popBackStack()
            }
        }
    }
}