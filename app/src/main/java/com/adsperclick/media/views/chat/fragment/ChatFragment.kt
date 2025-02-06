package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.databinding.FragmentChatBinding
import com.adsperclick.media.views.chat.adapters.ChatGroupListAdapter
import com.adsperclick.media.views.chat.adapters.HorizontalCompanyListAdapter

class ChatFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentChatBinding
    private lateinit var horizontalCompanyListAdapter: HorizontalCompanyListAdapter
    private lateinit var chatGroupListAdapter: ChatGroupListAdapter

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

        val companyList = listOf(Company("1", "All"),
            Company("2", "Amazon"),
            Company("3", "Meesho"),
            Company("8", "companyNumber-8"),
            Company("3", "Flipkart"),
            Company("4", "Jumbo Tail"))
        val compId = "3"
        horizontalCompanyListAdapter = HorizontalCompanyListAdapter()
        horizontalCompanyListAdapter.submitList(companyList)
        binding.rvHorizontalForCompanyList.adapter = horizontalCompanyListAdapter



        val groupChatList = listOf(GroupChatListingData("1", "Harsh Company"),
            GroupChatListingData("2", "Sigma Bois and Furnitures", null, listOf("1", "2"), lastSentMsg = Message("69", "Hello Harsh", "1")),
            GroupChatListingData("3", "Saumya Coffee", null, listOf("1", "2"), lastSentMsg = Message("68", "Hello Wet ass pussy", "1")),
            GroupChatListingData("4", "Jay", null, listOf("1", "2"), lastSentMsg = Message("68", "Nigger Man", "1")),
            GroupChatListingData("5", "BholeShopper", null, listOf("1", "2"), lastSentMsg = Message("68", "DumbFuck", "1"))
        )

        chatGroupListAdapter = ChatGroupListAdapter(object : ChatGroupListAdapter.OnGroupChatClickListener{
            override fun onItemClick(gc : GroupChatListingData) {
                Toast.makeText(context, "You clicked ${gc.groupName}!", Toast.LENGTH_SHORT).show()
            }
        })
        chatGroupListAdapter.submitList(groupChatList)
        binding.rvGroupChatList.adapter = chatGroupListAdapter

        setUpListener()
    }

    private fun setUpListener(){
        binding.btnNotifications.setOnClickListener(this)
        binding.addDetails.setOnClickListener(this)
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
}

