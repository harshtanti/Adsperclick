package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.NotificationListItemBinding


class NotificationListAdapter() : ListAdapter<NotificationMsg, NotificationListAdapter.NotificationListViewHolder>(DiffUtil())
{
    inner class NotificationListViewHolder(val binding: NotificationListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(notif: NotificationMsg)
        {
            binding.tvTitle.text = notif.notificationTitle
            binding.tvDescription.text= notif.notificationDescription
            binding.tvDateTime.text = notif.timestamp.toString()

            if(notif.isRead.not()){
                binding.tvNewIcon.visibility = View.VISIBLE
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationListViewHolder {
        val binding= NotificationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationListViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<NotificationMsg>()
    {
        override fun areItemsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }

        override fun areContentsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean {
            return oldItem == newItem
        }
    }
}

