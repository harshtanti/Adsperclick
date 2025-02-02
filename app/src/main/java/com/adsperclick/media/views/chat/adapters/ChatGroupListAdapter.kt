package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.GroupForGroupChat
import com.adsperclick.media.databinding.ChatGroupListItemBinding

class ChatGroupListAdapter() : ListAdapter<GroupForGroupChat, ChatGroupListAdapter.GroupChatListViewHolder>(DiffUtil())
{
    inner class GroupChatListViewHolder(val binding: ChatGroupListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(chatGroup: GroupForGroupChat)
        {
            binding.tvGroupName.text = chatGroup.groupName
            binding.tvLastMsg.text= chatGroup.lastSentMsg?.message ?: ""
            binding.tvLastMsgDateTime.text = chatGroup.lastSentMsg?.timestamp.toString()

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
