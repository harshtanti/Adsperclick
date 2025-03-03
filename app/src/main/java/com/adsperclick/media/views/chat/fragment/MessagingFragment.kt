package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentMessagingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.views.chat.adapters.MessagesAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.google.firebase.database.ChildEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject


@AndroidEntryPoint
class MessagingFragment : Fragment(),View.OnClickListener {

    lateinit var binding: FragmentMessagingBinding
    private val chatViewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var adapter : MessagesAdapter

    private var messagesListener: ChildEventListener? = null

    private lateinit var currentUser: User
    private var groupChat :GroupChatListingData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupChatObjAsString = arguments?.getString(CLICKED_GROUP)
        groupChat = groupChatObjAsString?.let {
                Json.decodeFromString(GroupChatListingData.serializer(), it)
            }
        currentUser = tokenManager.getUser()!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        setupAdapter()
        setUpListener()
        setupObservers()
    }

    private fun setUpView(){
             // Passed from ChatFragment



        binding.includeTopBar.tvGroupName.text = groupChat?.groupName ?: "Group-Name"

        groupChat?.groupImgUrl?.let { imageUrl ->
            UtilityFunctions.loadImageWithGlide(
                binding.includeTopBar.imgProfileDp.context,
                binding.includeTopBar.imgProfileDp,
                imageUrl
            )
        } ?: run {
            UtilityFunctions.setInitialsDrawable(
                binding.includeTopBar.imgProfileDp,
                groupChat?.groupName
            )
        }

        groupChat?.groupId?.let { groupId->
            chatViewModel.setGroupId(groupId)   // Group Id is set, "messages" live-data gets active, and it starts listening for realtime updates in room-db, realtime room-db updates are sent to adapter via observer here
            chatViewModel.fetchAllNewMessages(groupId)      // To fetch all the unread messages for this group in one single go!
        }
    }

    private fun setupAdapter(){

        val isClientOnRight = currentUser.role == Constants.ROLE.CLIENT
        adapter = currentUser.userId?.let { MessagesAdapter(it, isClientOnRight) }!!
        binding.rvChat.adapter = adapter
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            /*reverseLayout = true*/  // Reverse layout for better pagination
        }
    }



    private fun setUpListener(){
        binding.includeTextSender.btnSendMsg.setOnClickListener{
            val text = binding.includeTextSender.etTypeMessage.text.toString()      // here "text" will have the text msg
            if(text.isNotEmpty()){
                binding.includeTextSender.etTypeMessage.text.clear()
                groupChat?.let {gc->
                    chatViewModel.sendMessage(text, gc.groupId ?: "", currentUser)
                }
            }
        }
        binding.includeTopBar.container.setOnClickListener(this)
        binding.includeTopBar.btnBack.setOnClickListener(this)
    }

    private fun setupObservers(){

        // To update adapter with new messages, whenever Room-DB is updated
        chatViewModel.messages.observe(viewLifecycleOwner) { response ->
//            adapter.submitList(response) {
//                binding.rvChat.scrollToPosition(0)
//
//                // Update previous last message bubble
//                if (adapter.itemCount > 1) {
//                    adapter.notifyItemChanged(1)
//                }
//            }

            val oldLastMessagePosition = adapter.itemCount - 1

            adapter.submitList(response) {
                binding.rvChat.scrollToPosition(adapter.itemCount - 1)

                // Ensure previous last message updates (To update the chat bubble of previous message
                if (oldLastMessagePosition >= 0) {
                    adapter.notifyItemChanged(oldLastMessagePosition)
                }
            }
        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        chatViewModel.stopRealtimeListening()
    }

    override fun onClick(v: View?) {
        when(v){
            binding.includeTopBar.container ->{
                groupChat?.let {
                    val bundle = Bundle()
                    bundle.putString(CLICKED_GROUP, it.groupId)
                    findNavController().navigate(R.id.action_messagingFragment_to_groupProfileFragment, bundle)
                }
            }
            binding.includeTopBar.btnBack -> {
                findNavController().popBackStack()
            }
        }
    }

}