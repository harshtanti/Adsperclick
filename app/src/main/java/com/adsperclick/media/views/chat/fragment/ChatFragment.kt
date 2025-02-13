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
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.databinding.FragmentChatBinding
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.views.chat.adapters.ChatGroupListAdapter
import com.adsperclick.media.views.chat.adapters.HorizontalServiceListAdapter
import kotlinx.serialization.json.Json

class ChatFragment : Fragment(),View.OnClickListener {

    private lateinit var binding: FragmentChatBinding
    private lateinit var horizontalServiceListAdapter: HorizontalServiceListAdapter
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

        val companyList = listOf(Service("1", "All"),
            Service("2", "Amazon"),
            Service("3", "Meesho"),
            Service("8", "companyNumber-8"),
            Service("3", "Flipkart"),
            Service("4", "Jumbo Tail"))

        horizontalServiceListAdapter = HorizontalServiceListAdapter(
            object : HorizontalServiceListAdapter.OnServiceClickListener{
                override fun onItemClick(service: Service) {
                    Toast.makeText(context, "Selected : ${service.serviceName}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        horizontalServiceListAdapter.submitList(companyList)
        binding.rvHorizontalForServiceList.adapter = horizontalServiceListAdapter



        val groupChatList = listOf(GroupChatListingData("1", "Harsh Company"),
            GroupChatListingData("2", "Sigma Bois and Furnitures", null, null, listOf(Pair("1", 2)), lastSentMsg = Message("69", "Hello Harsh", "1")),
            GroupChatListingData("3", "Saumya Coffee", null, null, listOf(Pair("1", 2)), lastSentMsg = Message("68", "Hello Wet ass pussy", "1")),
            GroupChatListingData("4", "Jay", null, null, listOf(Pair("1", 2)), lastSentMsg = Message("68", "Nigger Man", "1")),
            GroupChatListingData("5", "BholeShopper", null, null, listOf(Pair("1", 2)), lastSentMsg = Message("68", "DumbFuck", "1"))
        )

        chatGroupListAdapter = ChatGroupListAdapter(object : ChatGroupListAdapter.OnGroupChatClickListener{
            override fun onItemClick(groupChat : GroupChatListingData) {
                Toast.makeText(context, "You clicked ${groupChat.groupName}!", Toast.LENGTH_SHORT).show()

                // We'll convert the "GroupChatListingData" object to "String" using the below function,
                // later this "String" will be converted back to object of "GroupChatListingData"
                val groupChatObjToString = Json.encodeToString(GroupChatListingData.serializer(), groupChat)

                val bundle = Bundle()
                bundle.putString(CLICKED_GROUP, groupChatObjToString)
                findNavController().navigate(R.id.action_navigation_chat_to_messagingFragment, bundle)
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

