package com.adsperclick.media.views.chat.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.databinding.ChatMsgItemIncomingBinding
import com.adsperclick.media.databinding.ChatMsgItemOutgoingBinding
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.FIRST_MSG
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.LAST_MSG
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.MIDDLE_MSG
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.SINGLE_MSG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue


// Nomenclature of XML :
// In our xml we have used names "chat_msg_item_incoming" and "chat_msg_item_outgoing"
// here, if user is a client, then "outgoing-msgs" mean messages sent by him and his fellow clients
// who are on the same side!

// And for him incoming messages mean messages coming from the other side( Which would include the employees or manager)

// For employee or admin it is the opposite :)

class MessagesAdapter(private val currentUserId: String) :
    ListAdapter<Message, MessagesAdapter.ChatAdapterViewHolder>(DiffUtil()) {

    inner class ChatAdapterViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(message: Message, msgRelativePosition: Int) {

            val formattedDate = formatTimestamp(message.timestamp!!)

            when (binding) {
                is ChatMsgItemIncomingBinding -> {
                    with(binding)
                    {
                        textMessageIncoming.text = message.message
                        tvTime.text = formattedDate

                        if(msgRelativePosition == SINGLE_MSG || msgRelativePosition == FIRST_MSG){
                            tvSenderName.text = message.senderName
                            tvSenderName.visibility = View.VISIBLE
                            tvSenderName.setTextColor(getSenderColor(message.senderId))
                        } else {
                            tvSenderName.visibility = View.GONE
                        }

                        val rootLayoutParams = root.layoutParams as ViewGroup.MarginLayoutParams

                        fun dpToPx(dp: Int): Int {
                            val scale = root.context.resources.displayMetrics.density
                            return (dp * scale + 0.5f).toInt()
                        }

                        when(msgRelativePosition){
                            FIRST_MSG -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_first_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                            MIDDLE_MSG -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_middle_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item

                            }
                            LAST_MSG -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            SINGLE_MSG -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_single_msg_grey)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                        }
                    }
                }

                is ChatMsgItemOutgoingBinding -> {
                    with(binding){
                        textMessageOutgoing.text = message.message
                        tvTimeOutgoing.text = formattedDate

                        if(msgRelativePosition == SINGLE_MSG || msgRelativePosition == FIRST_MSG){
                            tvSenderNameOutgoing.text = message.senderName
                            tvSenderNameOutgoing.visibility = View.VISIBLE
                        } else {
                            tvSenderNameOutgoing.visibility = View.GONE
                        }

                        val rootLayoutParams = root.layoutParams as ViewGroup.MarginLayoutParams

                        fun dpToPx(dp: Int): Int {
                            val scale = root.context.resources.displayMetrics.density
                            return (dp * scale + 0.5f).toInt()
                        }

                        when (msgRelativePosition) {
                            FIRST_MSG -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_first_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                            MIDDLE_MSG -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_middle_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            LAST_MSG -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            SINGLE_MSG -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_single_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatAdapterViewHolder {
        val binding = when (viewType) {
            VIEW_TYPE_INCOMING -> ChatMsgItemIncomingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            VIEW_TYPE_OUTGOING -> ChatMsgItemOutgoingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw IllegalArgumentException("Invalid view type")
        }
        return ChatAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatAdapterViewHolder, position: Int) {
        val message = getItem(position)
        val prevMsg = if (position > 0) getItem(position - 1) else null
        val nextMsg = if (position < itemCount - 1) getItem(position + 1) else null
        val msgRelativePosition = setPosition(message, prevMsg, nextMsg)
        holder.bind(getItem(position), msgRelativePosition)
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        return if (message.senderId == currentUserId) {
            VIEW_TYPE_OUTGOING
        } else {
            VIEW_TYPE_INCOMING
        }
    }

    fun setPosition(message: Message, prevMsg:Message?, nextMsg:Message?): Int{

        val isPrevMsgBySameSender = prevMsg?.let {
            it.senderId == message.senderId
        } ?: run {
            false
        }
        val isNextMsgBySameSender = nextMsg?.let {
            it.senderId == message.senderId
        } ?: run {
            false
        }

        return when{
            isPrevMsgBySameSender && isNextMsgBySameSender -> MIDDLE_MSG
            isPrevMsgBySameSender -> /*LAST_MSG*/FIRST_MSG
            isNextMsgBySameSender -> /*FIRST_MSG*/LAST_MSG
            else -> SINGLE_MSG
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Example: 01:45 PM
        return formatter.format(date)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.msgId == newItem.msgId
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_INCOMING = 1
        private const val VIEW_TYPE_OUTGOING = 2
    }

    val senderColors = listOf(
        Color.parseColor("#137333"), // Dark Green
        Color.parseColor("#174EA6"), // Dark Blue
        Color.parseColor("#B06000"), // Dark Orange
        Color.parseColor("#5E35B1"), // Dark Purple
        Color.parseColor("#C5221F")  // Dark Red
    )

    fun getSenderColor(userId: String?): Int {
        val index = userId.hashCode().absoluteValue % senderColors.size
        return senderColors[index]
    }
}