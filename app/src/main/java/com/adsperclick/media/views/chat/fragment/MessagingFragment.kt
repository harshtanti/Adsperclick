package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentMessagingBinding
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.views.chat.adapters.MessagesAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.google.firebase.database.ChildEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject


@AndroidEntryPoint
class MessagingFragment : Fragment() {

    lateinit var binding: FragmentMessagingBinding
    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var adapter : MessagesAdapter

    private var messagesListener: ChildEventListener? = null

    lateinit var currentUser: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser = tokenManager.getUser()!!
        setupView()
        setupAdapter()
        listener()
    }

    fun setupView(){
        val groupChatObjAsString = arguments?.getString(CLICKED_GROUP)      // Passed from ChatFragment

        val groupChat :GroupChatListingData? =
            groupChatObjAsString?.let {
                Json.decodeFromString(GroupChatListingData.serializer(), it)
            }

        binding.includeTopBar.tvGroupName.text = groupChat?.groupName ?: "Group-Name"

        groupChat?.groupImgUrl?.let {  }?: run{
            val drawable = UtilityFunctions.generateInitialsDrawable(
                binding.includeTopBar.imgProfileDp.context, groupChat?.groupName ?: "A")

            binding.includeTopBar.imgProfileDp.setImageDrawable(drawable)
        }
    }

    private fun setupAdapter(){
        adapter = MessagesAdapter("ifjle")
        binding.rvChat.adapter = adapter
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            reverseLayout = true  // Reverse layout for better pagination
        }
    }


    fun listener(){
        binding.includeTextSender.btnSendMsg.setOnClickListener{

        }
    }

}