package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.databinding.ChatGroupListItemBinding
import com.adsperclick.media.utils.Constants.EMPTY
import com.adsperclick.media.utils.Constants.MSG_TYPE.IMG_URL
import com.adsperclick.media.utils.Constants.MSG_TYPE.PDF_DOC
import com.adsperclick.media.utils.Constants.MSG_TYPE.VIDEO
import com.adsperclick.media.utils.Constants.SPACE
import com.adsperclick.media.utils.UtilityFunctions



class ChatGroupListAdapter(val onGroupChatClickListener: OnGroupChatClickListener) : ListAdapter<GroupChatListingData, ChatGroupListAdapter.GroupChatListViewHolder>(DiffUtil())
{
    interface OnGroupChatClickListener{
        fun onItemClick(chatGroup: GroupChatListingData)
    }

    inner class GroupChatListViewHolder(val binding: ChatGroupListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(chatGroup: GroupChatListingData)
        {
            val lastMessage = chatGroup.lastSentMsg?.let { lastMsg->
                var drawable = 0
                val msgText = when(lastMsg.msgType){
                    IMG_URL -> {
                        drawable = R.drawable.ic_image_drawable_start
                        "Photo"
                    }
                    PDF_DOC -> {
                        drawable = R.drawable.ic_pdf_drawable_start
                        "PDF"
                    }
                    VIDEO -> {
                        drawable = R.drawable.ic_video_drawable_start
                        "Video"
                    }
                    else -> lastMsg.message
                }
                binding.tvLastMsg.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
                (lastMsg.senderName?.split(SPACE)?.get(0) ?: EMPTY) + ": " + msgText
            } ?: run { EMPTY}

            val lastMsgTime = chatGroup.lastSentMsg?.timestamp?.let {
                UtilityFunctions.gcListDateFormat(it)
            } ?: run { EMPTY }

            with(binding){
                tvGroupName.text = chatGroup.groupName
                tvLastMsg.text= lastMessage
                tvLastMsgDateTime.text = lastMsgTime
            }

            chatGroup.groupImgUrl?.let { imageUrl ->
                UtilityFunctions.loadChatListingImgWithGlide(
                    binding.imgProfileDp.context,
                    binding.imgProfileDp,
                    imageUrl
                )
            } ?: run {
                UtilityFunctions.setInitialsDrawable(
                    binding.imgProfileDp,
                    chatGroup.groupName
                )
            }

            binding.root.setOnClickListener {
                onGroupChatClickListener.onItemClick(chatGroup)
            }
        }
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatListViewHolder {
        val binding= ChatGroupListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupChatListViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<GroupChatListingData>()
    {
        override fun areItemsTheSame(oldItem: GroupChatListingData, newItem: GroupChatListingData): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: GroupChatListingData, newItem: GroupChatListingData): Boolean {
            return oldItem == newItem
        }
    }
}
