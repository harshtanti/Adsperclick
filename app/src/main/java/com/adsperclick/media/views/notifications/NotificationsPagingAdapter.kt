package com.adsperclick.media.views.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.NotificationListItemBinding
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible

class NotificationsPagingAdapter (private val lastNotificationSeenTime : Long): PagingDataAdapter<NotificationMsg, NotificationsPagingAdapter.NotificationViewHolder>(
    DIFF_CALLBACK
) {

    inner class NotificationViewHolder(private val binding: NotificationListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notif: NotificationMsg) {
            binding.tvTitle.text = notif.notificationTitle
            binding.tvDescription.text = notif.notificationDescription
            binding.tvDateTime.text = UtilityFunctions.formatNotificationTimestamp(notif.timestamp)

            if(UtilityFunctions.timestampToLong(notif.timestamp) > lastNotificationSeenTime){
                binding.tvNewIcon.visible()
            } else {
                binding.tvNewIcon.gone()
            }
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


/*
// With feature to "insertNotificationsAtTop" ... not working ..
class NotificationsPagingAdapter() : PagingDataAdapter<NotificationMsg, NotificationsPagingAdapter.NotificationViewHolder>(DiffUtil()) {

    private val newNotifications = mutableListOf<NotificationMsg>()

    fun insertNotificationsAtTop(notifications: List<NotificationMsg>) {
        newNotifications.addAll(0, notifications) // Add new notifications at the top
        notifyItemRangeInserted(0, notifications.size) // Notify RecyclerView
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + newNotifications.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = if (position < newNotifications.size) {
            newNotifications[position] // Show new notifications first
        } else {
            getItem(position - newNotifications.size) // Show paged old notifications
        }
        if (notification != null) {
            holder.bind(notification)
        }
    }

    inner class NotificationViewHolder(private val binding: NotificationListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: NotificationMsg) {
            binding.tvTitle.text = notification.notificationTitle
            binding.tvDescription.text = notification.notificationDescription
            binding.tvDateTime.text = UtilityFunctions.formatNotificationTimestamp(notification.timestamp)
        }
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<NotificationMsg>() {
        override fun areItemsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }
        override fun areContentsTheSame(oldItem: NotificationMsg, newItem: NotificationMsg): Boolean {
            return oldItem == newItem
        }
    }
}

*/
