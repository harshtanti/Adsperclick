package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.applicationCommonView.bottomsheet.UploadImageDocsBottomSheet
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentGroupProfileBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.DialogUtils
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.disableHeaderButton
import com.adsperclick.media.utils.enableHeaderButton
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.adapters.GroupMemberAdapter
import com.adsperclick.media.views.chat.adapters.SelectUserCommonAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class GroupProfileFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentGroupProfileBinding
    private lateinit var adapter: GroupMemberAdapter
    private var isAdmin:Boolean = false
    private lateinit var currentUser: User
    private var groupId:String? = null
    private var groupChat : GroupChatListingData? = null
    private var userList = listOf<CommonData>()
    private var groupName:String?=null
    private var groupCompanyName:String?=null
    private var selectedImageFile: File? = null
    private val selectedTypeList = arrayListOf(Constants.CAMERA_VISIBLE,Constants.GALLERY_VISIBLE,Constants.DELETE_VISIBLE)

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString(CLICKED_GROUP)
        currentUser = tokenManager.getUser()!!
        isAdmin = currentUser.role == Constants.ROLE.ADMIN
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       binding = FragmentGroupProfileBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectUiData()
        setUpView()
        setUpVisibility()
        setUpAdapter()
        validateSubmitButton()
        setUpListener()
        setupObservers()

    }

    private fun collectUiData(){
        groupId?.let {
            viewModel.getGroupDetails(it)
        }
    }
    private fun setUpVisibility(){
        if (isAdmin){
            binding.header.btnSave.visible()
            binding.imageAddUser.visible()
            binding.btnImage.visible()
        }else{
            binding.header.btnSave.gone()
            binding.imageAddUser.gone()
            binding.btnImage.visible()
        }
    }
    private fun setUpView(){
        binding.header.tvTitle.text = getString(R.string.group_profile)
        binding.tvName.text = groupChat?.groupName ?: "Group-Name"

        groupChat?.groupImgUrl?.let { imageUrl ->
            UtilityFunctions.loadImageWithGlide(
                binding.imgProfileDp.context,
                binding.imgProfileDp,
                imageUrl
            )
        } ?: run {
            UtilityFunctions.setInitialsDrawable(
                binding.imgProfileDp,
                groupChat?.groupName
            )
        }

        binding.tvCount.text = binding.tvCount.context.getString(R.string.members,groupChat?.listOfUsers?.size)


    }

    private fun setUpAdapter() {
        adapter = GroupMemberAdapter()

        // Handle selection changes
        val listener = object : GroupMemberAdapter.AddMemberListener{
            override fun btnDelete(data: CommonData) {
                removeUser(data)
            }
        }

        adapter.setData(isAdmin, listener)
        binding.rvUser.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.usersListLiveData.observe(viewLifecycleOwner,userListObserver)
        viewModel.updateGroupLiveData.observe(viewLifecycleOwner,groupUpdateObserver)
        viewModel.leaveGroupResult.observe(viewLifecycleOwner,leaveGroupObserver)
        viewModel.groupDetailResult.observe(viewLifecycleOwner,groupDetailObserver)
    }

    private val groupDetailObserver = Observer<NetworkResult<GroupChatListingData>> { it ->
        when(it){
            is NetworkResult.Loading -> {
                binding.progressBar.visible()
            }
            is NetworkResult.Success -> {
                binding.progressBar.gone()
                it.data?.let {
                    groupChat = it
                    setUpView()
                    viewModel.fetchGroupUsers(it.listOfUsers)
                }
            }
            is NetworkResult.Error -> {
                binding.progressBar.gone()
                Toast.makeText(context, it.message ?: "Group Fetch failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val leaveGroupObserver = Observer<NetworkResult<String>> { it ->
        when (it) {
            is NetworkResult.Loading -> {
                binding.progressBar.visible()
            }
            is NetworkResult.Success -> {
                binding.progressBar.gone()

                if (!it.data.isNullOrEmpty()) {
                    groupId?.let {
                        viewModel.getGroupDetails(it)
                    }
                    Toast.makeText(context, "User removed successfully",Toast.LENGTH_SHORT).show()
                }
            }
            is NetworkResult.Error -> {
                binding.progressBar.gone()
                Toast.makeText(context, it.message ?: "Update failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val groupUpdateObserver = Observer<NetworkResult<Boolean>> { it ->
        when (it) {
            is NetworkResult.Loading -> {
                // Show loading indicator
                binding.progressBar.visible()
            }
            is NetworkResult.Success -> {
                // Hide loading indicator
                binding.progressBar.gone()

                // Update the UI to reflect changes
                if (it.data == true) {
                    // Update was successful
                    Toast.makeText(context, "Group Profile updated successfully",Toast.LENGTH_SHORT).show()

                    // Reset the selectedImageFile since it's been uploaded
                    selectedImageFile = null

                    // Revalidate the submit button state
                    validateSubmitButton()
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

    private val userListObserver = Observer<NetworkResult<List<User>>> { it ->
        when (it) {
            is NetworkResult.Success -> {
                binding.progressBar.gone()
                binding.rvUser.visible()
                it.data?.let { users ->
                    userList = users.map { user ->
                        if (user.role == Constants.ROLE.EMPLOYEE){
                            CommonData(
                                id = user.userId ?: "",
                                name = user.userName ?: "",
                                tagName = Constants.EMPLOYEE_SINGULAR,
                                imgUrl = user.userProfileImgUrl
                            )
                        }else{
                                CommonData(
                                    id = user.userId ?: "",
                                    name = user.userName ?: "",
                                    tagName = user.selfCompanyName,
                                    imgUrl = user.userProfileImgUrl
                                )
                        }
                    }
                    userList = userList.sortedBy { it.name }
                    groupCompanyName = userList
                        .firstOrNull { it.tagName != Constants.EMPLOYEE_SINGULAR }
                        ?.tagName
                    adapter.submitList(userList)
                }
            }
            is NetworkResult.Error -> {
                binding.progressBar.gone()
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
            is NetworkResult.Loading -> {
                binding.progressBar.visible()
            }
        }
    }

    private fun removeUser(data: CommonData){
        if (data.id == currentUser.userId){
            Toast.makeText(context, getString(R.string.you_cannot_remove_yourself), Toast.LENGTH_SHORT).show()
            return
        }
        val dialogListener = object : DialogUtils.DialogButtonClickListener {
            override fun onPositiveButtonClickedData(data: String) {
            }

            override fun onPositiveButtonClicked() {
                groupChat?.groupId?.let { data.id?.let { it1 -> viewModel.leaveGroup(it1, it) } }
            }

            override fun onNegativeButtonClicked() {

            }

            override fun onCloseButtonClicked() {

            }
        }

        DialogUtils.showDeleteDetailsDialog(
            requireContext(),
            dialogListener,
            getString(R.string.confirm_message_for_remove_details_dialog,data.name),
            getString(R.string.yes_remove),
            getString(R.string.no_cancel)
        )
    }

    private fun setUpListener(){
        binding.header.btnBack.setOnClickListener(this)
        binding.header.btnSave.setOnClickListener(this)
        binding.btnImage.setOnClickListener(this)
        binding.tvName.setOnClickListener(this)
        binding.imageAddUser.setOnClickListener(this)
    }

    private fun changeGroupName(){
        val dialogListener = object : DialogUtils.DialogButtonClickListener {
            override fun onPositiveButtonClickedData(data: String) {
                // Save phone number and update UI
                groupName = data
                binding.tvName.text = data
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
        DialogUtils.showEditTextDialog(
            requireContext(),
            dialogListener,
            getString(R.string.update_group_name),
            getString(R.string.enter_group_name),
            getString(R.string.yes_change),
            getString(R.string.cancel)
        )
    }

    private fun validateSubmitButton() {
        if (areFixedDetailsChanged()) {
            binding.header.btnSave.enableHeaderButton()
        } else {
            binding.header.btnSave.disableHeaderButton()
        }
    }

    private fun areFixedDetailsChanged():Boolean{
        if(selectedImageFile!=null){
            return true
        }
        if (groupName!=null){
            return true
        }
        return false
    }

    private fun updateGroupProfile(){
        groupChat?.groupId?.let { viewModel.updateGroupProfile(it,groupName,selectedImageFile) }

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

    override fun onClick(v: View?) {
        when(v){
            binding.tvName ->{
                if (isAdmin){
                    changeGroupName()
                }
            }
            binding.header.btnSave -> {
                if(areFixedDetailsChanged()){
                    binding.header.btnSave.disableHeaderButton()
                    updateGroupProfile()
                }
            }
            binding.btnImage -> {
                if(isAdmin){
                    val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                        uploadOnSelectListener,selectedTypeList)
                    bottomSheet.show(childFragmentManager, bottomSheet.tag)
                }
            }
            binding.header.btnBack ->{
                findNavController().popBackStack()
            }
            binding.imageAddUser -> {
                val bundle = Bundle()
                bundle.putBoolean(Constants.GROUP_PROFILE, true)
                bundle.putString(Constants.COMPANY_SINGULAR,groupCompanyName)
                bundle.putString(Constants.GROUP_ID,groupId)
                findNavController().navigate(R.id.action_groupProfileFragment_to_selectUserFragment, bundle)
            }
        }
    }

}