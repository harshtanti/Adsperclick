package com.adsperclick.media.views.chat.adapters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.GroupForGroupChat
import com.adsperclick.media.databinding.ChatGroupListItemBinding
import com.adsperclick.media.utils.Constants.EMPTY
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.Validate.toInitials
import kotlin.math.abs


class ChatGroupListAdapter() : ListAdapter<GroupForGroupChat, ChatGroupListAdapter.GroupChatListViewHolder>(DiffUtil())
{
    inner class GroupChatListViewHolder(val binding: ChatGroupListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(chatGroup: GroupForGroupChat)
        {
            binding.tvGroupName.text = chatGroup.groupName
            binding.tvLastMsg.text= chatGroup.lastSentMsg?.message ?: ""
            binding.tvLastMsgDateTime.text = chatGroup.lastSentMsg?.timestamp.toString()

            chatGroup.groupDpUrl?.let {  }?: run{
                val drawable = UtilityFunctions.generateInitialsDrawable(
                    binding.imgProfileDp.context, chatGroup.groupName ?: "A")

                binding.imgProfileDp.setImageDrawable(drawable)
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

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<GroupForGroupChat>()
    {
        override fun areItemsTheSame(oldItem: GroupForGroupChat, newItem: GroupForGroupChat): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: GroupForGroupChat, newItem: GroupForGroupChat): Boolean {
            return oldItem == newItem
        }
    }
}
