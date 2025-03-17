package com.adsperclick.media.views.chat.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.applicationCommonView.bottomsheet.UploadImageDocsBottomSheet
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.databinding.FragmentNewGroupBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.disableSubmitButton
import com.adsperclick.media.utils.enableSubmitButton
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.viewmodel.NewGroupViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class NewGroupFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentNewGroupBinding
    private val selectedTypeList = arrayListOf(Constants.CLOSE_VISIBLE,Constants.HEADING_VISIBLE,Constants.CAMERA_VISIBLE,Constants.GALLERY_VISIBLE,Constants.DELETE_VISIBLE)
    private var company:Company?=null

    @Inject
    lateinit var tokenManager : TokenManager

    private val viewModel: NewGroupViewModel by navGraphViewModels(R.id.new_group_navigation) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNewGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiDataCollect()
        setUpHeader()
        setUpHint()
        setUpInputType()
        setUpClickListener()
        setUpOnChangeListener()
        validateSubmitButton()
        setUpObserver()
        val drawable = UtilityFunctions.generateInitialsDrawable(
            binding.imgProfileDp.context, "A")
        binding.imgProfileDp.setImageDrawable(drawable)
    }

    private fun uiDataCollect(){
        if (viewModel.selectedUserSetTotal.isNotEmpty()) {
            viewModel.getCompanyNameData(viewModel.selectedUserSetTotal.first())
        }else{
            binding.serviceName.setDataItemList(viewModel.serviceList.mapNotNull { it.serviceName })
        }
    }

    private fun setUpHeader(){
        binding.header.tvTitle.text = getString(R.string.new_group)
        binding.header.btnSave.gone()
    }

    private fun setUpHint(){
        with(binding){
            groupName.setHint(R.string.enter_group_name)
            serviceName.setHint(R.string.select_service)
        }
    }

    private fun setUpInputType(){
        with(binding){
            groupName.setInputType(InputType.TYPE_CLASS_TEXT)
        }
    }

    private fun setUpClickListener(){
        binding.submitButton.setOnClickListener(this)
        binding.btnImage.setOnClickListener(this)
        binding.header.btnBack.setOnClickListener(this)
    }
    private fun setUpOnChangeListener(){
        binding.groupName.getEditView().doAfterTextChanged{
            viewModel.groupName = it.toString().trim()
            validateSubmitButton()
        }
        binding.serviceName.getSpinnerView().doAfterTextChanged {
            val enteredText = it.toString().trim()
            viewModel.selectedService = viewModel.serviceList.find { service ->
                service.serviceName.equals(enteredText, ignoreCase = true)
            }
            validateSubmitButton()
        }
    }

    private fun validateSubmitButton() {
        if (areFixedDetailsValid()) {
            binding.submitButton.enableSubmitButton()
        } else {
            binding.submitButton.disableSubmitButton()
        }
    }


    private fun areFixedDetailsValid(): Boolean {
        with(binding) {
            if (groupName.getText()?.isEmpty() == true){
                return false
            }
            if (serviceName.getSelectedItem()?.isEmpty() == true){
                return false
            }
            return true
        }
    }

    private fun saveUserDetails() {
        val user = tokenManager.getUser()

        if (user != null) {
            user.userId?.let { userId ->
                viewModel.selectedUserSet.add(userId)
            }
        }

        val userListWithRoles: List<GroupUser> = viewModel.selectedUserSet.map { userId ->
            GroupUser(userId, null)
        }

        val groupData = GroupChatListingData(
            groupId = null,
            groupName = viewModel.groupName,
            groupImgUrl = null,
            associatedServiceId = viewModel.selectedService?.serviceId,
            associatedService = viewModel.selectedService?.serviceName,
            listOfUsers = userListWithRoles,
            lastSentMsg = null
        )
        viewModel.selectedImageFile?.let { viewModel.createGroup(groupData, it) }
    }

    private fun setUpObserver(){
        viewModel.createGroupLiveData.observe(viewLifecycleOwner,createGroupObserver)
        viewModel.companyDataLiveData.observe(viewLifecycleOwner,companyDataObserver)
    }

    private val companyDataObserver = Observer<NetworkResult<Company>> { it ->
        when(it){

            is NetworkResult.Success ->{
                company = it.data
                if (viewModel.selectedUserSetTotal.isNotEmpty()) {
                    company?.listOfServices?.mapNotNull { it.serviceName }
                        ?.let { it1 -> binding.serviceName.setDataItemList(it1) }
                }else{
                    binding.serviceName.setDataItemList(viewModel.serviceList.mapNotNull { it.serviceName })
                }
                binding.progressBar.gone()
            }

            is NetworkResult.Error ->{
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.gone()
            }

            is NetworkResult.Loading ->{
                binding.progressBar.visible()

            }
        }
    }


    private val createGroupObserver = Observer<NetworkResult<Boolean>> {
        when(it){

            is NetworkResult.Success ->{
                successMessage()
                binding.progressBar.gone()
            }

            is NetworkResult.Error ->{
                failedMessage()
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.gone()
            }

            is NetworkResult.Loading ->{
                binding.progressBar.visible()

            }
        }
    }

    private fun successMessage(){
        binding.submitButton.apply {
            text = getString(R.string.success)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green_info)
            isEnabled = false
        }

        lifecycleScope.launch {
            delay(1000)
            findNavController().navigate(
                R.id.action_newGroupFragment_to_navigation_chat,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_chat, true) // Pops back to ChatFragment and clears UserListFragment
                    .build()
            )
        }
    }

    private fun failedMessage() {
        binding.submitButton.apply {
            text = getString(R.string.failed)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red_email)
            isEnabled = false
        }

        lifecycleScope.launch {
            delay(1000) // Show the error message for 1 second
            binding.submitButton.apply {
                text = getString(R.string.done) // Change back to original text
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue_common_button)
                isEnabled = true
            }
        }
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
                            viewModel.selectedImageFile = imageFile

                            // Load image preview
                            loadImageIntoView(imageFile)
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


    override fun onClick(v: View?) {
        when(v){
            binding.submitButton -> {
                if (areFixedDetailsValid()) {
                    saveUserDetails()
                }
            }
            binding.btnImage -> {
                val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                    uploadOnSelectListener,selectedTypeList,getString(R.string.group_profile))
                bottomSheet.show(childFragmentManager, bottomSheet.tag)
            }
            binding.header.btnBack ->{
                findNavController().popBackStack()
            }
        }
    }


}