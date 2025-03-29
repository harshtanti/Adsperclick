package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentChatBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.Constants.EMPTY
import com.adsperclick.media.utils.Constants.LAST_SEEN_GROUP_TIME
import com.adsperclick.media.utils.Utils
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.adapters.ChatGroupListAdapter
import com.adsperclick.media.views.chat.adapters.HorizontalServiceListAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.homeActivity.SharedHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentChatBinding
    private var isAdmin = false
    private lateinit var horizontalServiceListAdapter: HorizontalServiceListAdapter
    private lateinit var chatGroupListAdapter: ChatGroupListAdapter

    private val chatViewModel : ChatViewModel by viewModels()
    private val sharedViewModel : SharedHomeViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private var listOfGroupChat = listOf<GroupChatListingData>()
    private var selectedService = DEFAULT_SERVICE
    private var searchText = EMPTY

    var user: User? = null

//    @Inject
//    lateinit var cloudFunc : FirebaseFunctions



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedService = DEFAULT_SERVICE
        searchText = EMPTY
        user = sharedViewModel.userData
        setUpVisibility()
        setUpAdapters()
        setUpListener()
        setUpObservers()
    }

    private fun setUpAdapters(){
        horizontalServiceListAdapter = HorizontalServiceListAdapter(
            object : HorizontalServiceListAdapter.OnServiceClickListener{
                override fun onItemClick(service: Service) {
                    selectedService = service
                    updateAdapterWithListOfGroupsHavingSelectedService()
                }
            }
        )
        binding.rvHorizontalForServiceList.adapter = horizontalServiceListAdapter
        horizontalServiceListAdapter.submitList(user?.listOfServicesAssigned)


        val currentTime = if(user?.role == Constants.ROLE.ADMIN){
            Utils.getTime()
        } else {-1L}

        chatGroupListAdapter = ChatGroupListAdapter(onClickingGroupChatItem,
            currentTime, user?.userId ?: "", sharedViewModel.lastSeenTimeForEachUserEachGroup)

        binding.rvGroupChatList.adapter = chatGroupListAdapter
        chatViewModel.startListeningToGroups(user?.listOfGroupsAssigned ?: listOf())
    }

    private fun setUpVisibility(){
        isAdmin = user?.role == Constants.ROLE.ADMIN
        if (!isAdmin) {
            binding.addDetails.gone()
        }

        val isClient = user?.role == Constants.ROLE.CLIENT
        if(isClient){
            binding.rvHorizontalForServiceList.gone()       // List of services an employee is assigned,
                                                            // makes no sense for a client
        } else {binding.rvHorizontalForServiceList.visible()}
    }

    private fun setUpListener(){
        binding.btnNotifications.setOnClickListener(this)
        binding.addDetails.setOnClickListener(this)
        binding.btnTesting.setOnClickListener(this)
    }


    private fun setUpObservers(){

        chatViewModel.listOfGroupChatLiveData.observe(viewLifecycleOwner){ it ->
            it.handle { response->
                when(response){
                    is NetworkResult.Success ->{
                        response.data?.let {listOfGroups->
                            listOfGroupChat = listOfGroups
                            /*testingGroupId = listOfGroups[0].groupId.toString()*/
                            updateAdapterWithListOfGroupsHavingSelectedService()

                            sharedViewModel.idOfGroupToOpen?.let { groupId ->         // If no arguments r passed it will come as null (normal scenario, when not coming via notification)
                                val groupData = listOfGroups.find { it.groupId == groupId }
                                sharedViewModel.idOfGroupToOpen=null
                                groupData?.let { openGroup(it) }
                            }
                        }
                    }

                    is NetworkResult.Error -> {
                        Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {}
                }
            }
        }

    }

    override fun onClick(v: View?) {
        when(v){
            binding.btnNotifications -> {
                findNavController().navigate(R.id.action_navigation_chat_to_notificationListingFragment)
            }
            binding.addDetails -> {
                findNavController().navigate(R.id.action_navigation_chat_to_selectUserFragment)
            }

            binding.btnTesting -> {
            }
        }
    }


    fun updateAdapterWithListOfGroupsHavingSelectedService(){
        var listOfDesiredGroupChats = when(selectedService){
            DEFAULT_SERVICE -> listOfGroupChat
            else -> listOfGroupChat.filter { it.associatedServiceId == selectedService.serviceId }
        }

        if(searchText.isNotEmpty()){
            listOfDesiredGroupChats = listOfDesiredGroupChats.filter {
                it.groupName?.startsWith(searchText, true) ?: false }
        }

        chatGroupListAdapter.submitList(listOfDesiredGroupChats){
            binding.rvGroupChatList.scrollToPosition(0)
            if(listOfDesiredGroupChats.isEmpty()){
                binding.groupNoGroups.visible()
            } else{
                binding.groupNoGroups.gone()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.etSearchBar.setText(EMPTY)
        sharedViewModel.pageNo=null
        textWatcher()
//        setupKeyboardVisibilityListener()
    }

    private fun textWatcher(){
        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Called after the text has been changed
                // This is usually where you want to perform actions based on the new text
                searchText = s.toString()
                updateAdapterWithListOfGroupsHavingSelectedService()
            }
        })
    }



    // For triggering keyboard visibility : When keyboard is visible the bottomNav Menu should be gone
    // and vice-versa
/*    private var windowInsetsCallback: OnApplyWindowInsetsListener? = null

    private fun setupKeyboardVisibilityListener() {
        val rootView = requireActivity().window.decorView.rootView

        // Create the listener
        windowInsetsCallback = OnApplyWindowInsetsListener { _, insets ->
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomNav = (activity as? HomeActivity)?.binding?.bottomNavigation

            if (isKeyboardVisible) {
                bottomNav?.gone()
            } else {
                // Only show if we're on a main navigation destination
                val navController = findNavController()
                val currentDestinationId = navController.currentDestination?.id
                if (currentDestinationId == R.id.navigation_chat ||
                    currentDestinationId == R.id.navigation_user ||
                    currentDestinationId == R.id.navigation_setting) {
                    bottomNav?.visible()
                }
            }
            insets
        }

        // Set the listener
        ViewCompat.setOnApplyWindowInsetsListener(rootView, windowInsetsCallback)
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the keyboard listener when fragment view is destroyed
/*        if (windowInsetsCallback != null) {
            ViewCompat.setOnApplyWindowInsetsListener(
                requireActivity().window.decorView.rootView,
                null
            )
            windowInsetsCallback = null
        }*/
    }

    private val onClickingGroupChatItem = object : ChatGroupListAdapter.OnGroupChatClickListener{
        override fun onItemClick(chatGroup : GroupChatListingData) {
            openGroup(chatGroup)
        }
    }


    fun openGroup(groupData : GroupChatListingData){

        // We'll convert the "GroupChatListingData" object to "String" using the below function,
        // later this "String" will be converted back to object of "GroupChatListingData"
        val groupChatObjToString = Json.encodeToString(GroupChatListingData.serializer(), groupData)

        val currentUserLastSeen = sharedViewModel.lastSeenTimeForEachUserEachGroup?.get(groupData.groupId.toString())?.get(user?.userId)

        val bundle = Bundle()
        bundle.putString(CLICKED_GROUP, groupChatObjToString)
        if (currentUserLastSeen != null) {
            bundle.putLong(LAST_SEEN_GROUP_TIME, currentUserLastSeen)
        }
        findNavController().navigate(R.id.action_navigation_chat_to_messagingFragment, bundle)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

}

