package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Group
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.databinding.FragmentMessagingBinding
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.UtilityFunctions
import kotlinx.serialization.json.Json

class MessagingFragment : Fragment() {

    lateinit var binding: FragmentMessagingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
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

    fun listener(){
        binding.includeTextSender.btnSendMsg.setOnClickListener{

        }
    }

}