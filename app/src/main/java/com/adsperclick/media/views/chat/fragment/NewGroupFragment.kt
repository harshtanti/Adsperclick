package com.adsperclick.media.views.chat.fragment

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
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.databinding.FragmentNewGroupBinding
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.chat.viewmodel.NewGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class NewGroupFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentNewGroupBinding

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
            serviceName.setDataItemList(viewModel.serviceList.mapNotNull { it.serviceName })
        }
    }

    private fun setUpClickListener(){
        binding.submitButton.setOnClickListener(this)
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
            enableSubmitButton()
        } else {
            disableSubmitButton()
        }
    }

    private fun disableSubmitButton() {
        binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), R.color.disabled_color
        )
        binding.submitButton.isEnabled = false
    }

    private fun enableSubmitButton() {
        binding.submitButton.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), R.color.blue_common_button
        )
        binding.submitButton.isEnabled = true
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
        viewModel.createGroup(groupData)
    }

    private fun setUpObserver(){
        viewModel.createGroupLiveData.observe(viewLifecycleOwner,createGroupObserver)
    }

    private val createGroupObserver = Observer<NetworkResult<GroupChatListingData>> {
        when(it){

            is NetworkResult.Success ->{
                successMessage()
            }

            is NetworkResult.Error ->{
                failedMessage()
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }
    }

    private fun successMessage(){
        binding.submitButton.apply {
            text = getString(R.string.success)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green_info)
            isEnabled = false
        }

        lifecycleScope.launch {
            delay(2000)
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


    override fun onClick(v: View?) {
        when(v){
            binding.submitButton -> {
                if (areFixedDetailsValid()) {
                    saveUserDetails()
                }
            }
        }
    }


}