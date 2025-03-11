package com.adsperclick.media.views.setting.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.applicationCommonView.bottomsheet.UploadImageDocsBottomSheet
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.ConsumableValue
import com.adsperclick.media.utils.DialogUtils
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.disableHeaderButton
import com.adsperclick.media.utils.disableSubmitButton
import com.adsperclick.media.utils.enableHeaderButton
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.login.MainActivity
import com.adsperclick.media.views.login.viewModels.AuthViewModel
import com.adsperclick.media.views.setting.viewmodel.SettingViewModel
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
    private var phoneNumber:String?=null


    private val authViewModel: AuthViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()

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
        observeViewModel()
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
            it.userName?.let { name ->
                binding.tvName.text = name
            } ?: run {
                binding.tvName.text = "N.A."
            }
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
            it.userPhoneNumber?.let { data->
                binding.tvPhone.text = data
            } ?: run {
                binding.tvPhone.text = "N.A."
            }
            it.email?.let { data->
                binding.tvEmail.text = data
            } ?: run {
                binding.tvEmail.text = "N.A."
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
        user?.userId?.let { settingViewModel.updateUser(it,phoneNumber,selectedImageFile) }
    }

    private fun observeViewModel() {
        // Observe the update user result
        settingViewModel.updateUserLiveData.observe(viewLifecycleOwner,updateUserObserver)
        authViewModel.signoutLiveData.observe(viewLifecycleOwner, signoutObserver)
    }

    private val updateUserObserver = androidx.lifecycle.Observer<ConsumableValue<NetworkResult<Boolean>>> {it1 ->
        it1.handle {
                when (it) {
                    is NetworkResult.Loading -> {
                        // Show loading indicator
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is NetworkResult.Success -> {
                        // Hide loading indicator
                        binding.progressBar.visibility = View.GONE

                        // Update the UI to reflect changes
                        if (it.data == true) {
                            // Update was successful
                            Toast.makeText(context, "Profile updated successfully",Toast.LENGTH_SHORT).show()

                            // Update the stored user data with the new image URL
                            val updatedUser = tokenManager.getUser()?.copy(
                                // Update any fields that were changed
                                userPhoneNumber = phoneNumber ?: tokenManager.getUser()?.userPhoneNumber
                            )
                            updatedUser?.let { tokenManager.saveUser(it) }

                            // Reset the selectedImageFile since it's been uploaded
                            selectedImageFile = null

                            // Revalidate the submit button state
                            validateSubmitButton()
                        }
                    }
                    is NetworkResult.Error -> {
                        // Hide loading indicator
                        binding.progressBar.visibility = View.GONE

                        // Show error message
                        Toast.makeText(context, it.message ?: "Update failed",Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }



    private val signoutObserver = androidx.lifecycle.Observer<ConsumableValue<NetworkResult<Boolean>>> {it1 ->
        it1.handle {
            when (it) {
                is NetworkResult.Loading -> {
                }

                is NetworkResult.Success -> {
                    binding.progressBar.gone()

                    // Update the UI to reflect changes
                    if (it.data == true) {
                        // Signout-user
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }

                is NetworkResult.Error -> {
                    // Hide loading indicator
                    binding.progressBar.gone()
                    // Show error message
                    Toast.makeText(context, it.message ?: "Update failed",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    private fun showPhoneNumberDialog() {
        // Create a listener implementation for the dialog
        val dialogListener = object : DialogUtils.DialogButtonClickListener {
            override fun onPositiveButtonClickedData(data: String) {
                // Save phone number and update UI
                phoneNumber = data
                binding.tvPhone.text = data
                validateSubmitButton()
            }

            override fun onPositiveButtonClicked() {
                TODO("Not yet implemented")
            }

            override fun onNegativeButtonClicked() {
                // Do nothing on cancel
            }

            override fun onCloseButtonClicked() {
                TODO("Not yet implemented")
            }
        }

        // Use the modified utility function to show the dialog
        DialogUtils.showPhoneTextDialog(
            requireContext(),
            dialogListener,
            getString(R.string.update_phone_number),
            getString(R.string.enter_ten_digit_phone_number),
            getString(R.string.yes_change),
            getString(R.string.cancel)
        )
    }


    override fun onClick(v: View?) {
        when(v){
            binding.header.btnSave -> {
                if(areFixedDetailsChanged()){
                    binding.header.btnSave.disableHeaderButton()
                    updateProfile()
                }
            }
            binding.btnImage -> {
                val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                    uploadOnSelectListener,selectedTypeList)
                bottomSheet.show(childFragmentManager, bottomSheet.tag)
            }
            binding.btnLogout -> {
                binding.progressBar.visible()
                authViewModel.signOut()
            }
            binding.header.btnBack -> {
                findNavController().popBackStack()
            }
            binding.cvPhone->{
                showPhoneNumberDialog()

            }
        }
    }


}