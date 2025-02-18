package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.NotificationListItemBinding
import com.adsperclick.media.utils.UtilityFunctions

class NotificationsPagingAdapter : PagingDataAdapter<NotificationMsg, NotificationsPagingAdapter.NotificationViewHolder>(DIFF_CALLBACK) {

    inner class NotificationViewHolder(private val binding: NotificationListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notif: NotificationMsg) {
            binding.tvTitle.text = notif.notificationTitle
            binding.tvDescription.text = notif.notificationDescription
            binding.tvDateTime.text = UtilityFunctions.formatNotificationTimestamp(notif.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        notification?.let { holder.bind(it) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationMsg>() {
            override fun areItemsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean =
                oldItem.notificationId == newItem.notificationId

            override fun areContentsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean =
                oldItem == newItem
        }
    }
}