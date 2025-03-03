package com.adsperclick.media.views.chat.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.GroupUser
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.databinding.FragmentChatBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.Constants.DEFAULT_SERVICE
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.chat.adapters.ChatGroupListAdapter
import com.adsperclick.media.views.chat.adapters.HorizontalServiceListAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.login.MainActivity
import com.adsperclick.media.views.login.viewModels.AuthViewModel
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

    @Inject
    lateinit var tokenManager: TokenManager

    private var listOfGroupChat = listOf<GroupChatListingData>()
    private var selectedService = DEFAULT_SERVICE

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

        syncUser()              // To fetch latest "User" object from DB (In case some changes were made regarding this user, like he maybe blocked by admin or maybe assigned some new services!)
        setUpVisibility()
        setUpAdapters()
        setUpListener()
        setUpObservers()
    }

    private fun syncUser(){
        chatViewModel.syncUser()
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

        chatGroupListAdapter = ChatGroupListAdapter(object : ChatGroupListAdapter.OnGroupChatClickListener{
            override fun onItemClick(groupChat : GroupChatListingData) {

                // We'll convert the "GroupChatListingData" object to "String" using the below function,
                // later this "String" will be converted back to object of "GroupChatListingData"
                val groupChatObjToString = Json.encodeToString(GroupChatListingData.serializer(), groupChat)

                val bundle = Bundle()
                bundle.putString(CLICKED_GROUP, groupChatObjToString)
                findNavController().navigate(R.id.action_navigation_chat_to_messagingFragment, bundle)
            }
        })
        binding.rvGroupChatList.adapter = chatGroupListAdapter
    }

    private fun setUpVisibility(){
        isAdmin = tokenManager.getUser()?.role == Constants.ROLE.ADMIN
        if (!isAdmin) {
            binding.addDetails.gone()
        }

        val isClient = tokenManager.getUser()?.role == Constants.ROLE.CLIENT
        if(isClient){
            binding.rvHorizontalForServiceList.gone()       // List of services an employee is assigned,
                                                            // makes no sense for a client
        }
    }

    private fun setUpListener(){
        binding.btnNotifications.setOnClickListener(this)
        binding.addDetails.setOnClickListener(this)
    }

    private fun setUpObservers(){
        chatViewModel.userLiveData.observe(viewLifecycleOwner){response->
            when(response){
                is NetworkResult.Error ->{
                    Toast.makeText(context, "${response.message}" , Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Success ->{
                    val user = response.data
                    if(user?.isBlocked == true){
                        chatViewModel.signOut()

                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else{
                        user?.listOfGroupsAssigned?.let { chatViewModel.startListeningToGroups(it) }
                        user?.listOfServicesAssigned?.let {
                            horizontalServiceListAdapter.submitList(it)
                        }
                    }
                }
                else ->{}
            }
        }

        chatViewModel.listOfGroupChatLiveData.observe(viewLifecycleOwner){response->
            when(response){
                is NetworkResult.Success ->{
                    response.data?.let {
                        listOfGroupChat = it
                        updateAdapterWithListOfGroupsHavingSelectedService()
//                        listOfDesiredGroupChats = listOfGroupChat
//                        if(it.isNotEmpty()) chatGroupListAdapter.submitList(listOfGroupChat)
                    }
                }

                is NetworkResult.Error -> {
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Loading -> {}
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
        }
    }

    fun updateAdapterWithListOfGroupsHavingSelectedService(){
        val listOfDesiredGroupChats = when(selectedService){
            DEFAULT_SERVICE -> listOfGroupChat
            else -> listOfGroupChat.filter { it.associatedServiceId == selectedService.serviceId }
        }

        chatGroupListAdapter.submitList(listOfDesiredGroupChats){
            binding.rvGroupChatList.scrollToPosition(0)
        }
    }
}

