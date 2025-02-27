package com.adsperclick.media.views.chat.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.databinding.ChatMsgItemIncomingBinding
import com.adsperclick.media.databinding.ChatMsgItemOutgoingBinding
import com.adsperclick.media.utils.Constants.ROLE.CLIENT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.FIRST_MSG_BY_CURRENT_USER
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.FIRST_MSG_LEFT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.FIRST_MSG_RIGHT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.LAST_MSG_BY_CURRENT_USER
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.LAST_MSG_LEFT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.LAST_MSG_RIGHT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.MIDDLE_MSG_BY_CURRENT_USER
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.MIDDLE_MSG_LEFT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.MIDDLE_MSG_RIGHT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.SINGLE_MSG_BY_CURRENT_USER
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.SINGLE_MSG_LEFT
import com.adsperclick.media.utils.Constants.TXT_MSG_TYPE.SINGLE_MSG_RIGHT
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.UtilityFunctions.formatMessageTimestamp
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible



// Nomenclature of XML :
// In our xml we have used names "chat_msg_item_incoming" and "chat_msg_item_outgoing"
// here, if user is a client, then "outgoing-msgs" mean messages sent by him and his fellow clients
// who are on the same side!

// And for him incoming messages mean messages coming from the other side( Which would include the employees or manager)

// For employee or admin it is the opposite :)

// So if the sender is an employee, all msgs sent by sender or other fellow employees will be displayed on the right
// If sender is a client, then all msgs sent by him and other fellow client will be displayed on the right
// So whoever you are , your's and your community member's messages will always be displyed on right.. Just like whatsapp!

// Am using a variable "isClientOnRight" to decide on which side client should be and on other side rest others will be

class MessagesAdapter(private val currentUserId: String, private val isClientOnRight: Boolean) :
    ListAdapter<Message, MessagesAdapter.ChatAdapterViewHolder>(DiffUtil()) {

    inner class ChatAdapterViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(message: Message, msgRelativePosition: Int) {

            val formattedDate = formatMessageTimestamp(message.timestamp!!)

            when (binding) {
                is ChatMsgItemIncomingBinding -> {
                    with(binding)
                    {
                        textMessageIncoming.text = message.message
                        tvTime.text = formattedDate

                        if(msgRelativePosition == SINGLE_MSG_LEFT || msgRelativePosition == FIRST_MSG_LEFT){
                            tvSenderName.text = message.senderName
                            tvSenderName.visible()
                            tvSenderName.setTextColor(UtilityFunctions.getSenderColor(message.senderId))
                        } else {
                            tvSenderName.gone()
                        }

                        val rootLayoutParams = root.layoutParams as ViewGroup.MarginLayoutParams

                        fun dpToPx(dp: Int): Int {
                            val scale = root.context.resources.displayMetrics.density
                            return (dp * scale + 0.5f).toInt()
                        }

                        when(msgRelativePosition){
                            FIRST_MSG_LEFT -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_first_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                            MIDDLE_MSG_LEFT -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_middle_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item

                            }
                            LAST_MSG_LEFT -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            SINGLE_MSG_LEFT -> {
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

                        if(msgRelativePosition == SINGLE_MSG_RIGHT || msgRelativePosition == FIRST_MSG_RIGHT){
                            tvSenderNameOutgoing.text = message.senderName
                            tvSenderNameOutgoing.setTextColor(UtilityFunctions.getSenderColor(message.senderId))
                            tvSenderNameOutgoing.visible()
                        } else {
                            tvSenderNameOutgoing.gone()
                        }

                        val textColor = when(msgRelativePosition){
                            SINGLE_MSG_RIGHT, FIRST_MSG_RIGHT, MIDDLE_MSG_RIGHT, LAST_MSG_RIGHT -> Color.parseColor("#000000")
                            else -> Color.parseColor("#ffffff")
                        }

                        textMessageOutgoing.setTextColor(textColor)
                        tvTimeOutgoing.setTextColor(textColor)


                        val rootLayoutParams = root.layoutParams as ViewGroup.MarginLayoutParams

                        fun dpToPx(dp: Int): Int {
                            val scale = root.context.resources.displayMetrics.density
                            return (dp * scale + 0.5f).toInt()
                        }

                        when (msgRelativePosition) {
                            FIRST_MSG_BY_CURRENT_USER -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_first_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                            MIDDLE_MSG_BY_CURRENT_USER -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_middle_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            LAST_MSG_BY_CURRENT_USER -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            SINGLE_MSG_BY_CURRENT_USER -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_single_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }

                            FIRST_MSG_RIGHT -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_first_msg_dark_grey_right)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                            MIDDLE_MSG_RIGHT-> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_middle_msg_dark_grey_right)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            LAST_MSG_RIGHT-> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_dark_grey_right)
                                rootLayoutParams.topMargin = dpToPx(0)  // Extra margin for entire item
                            }
                            SINGLE_MSG_RIGHT -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_single_msg_dark_grey)
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

        return if (message.senderId == currentUserId                // Current user should be on right
            || (isClientOnRight && message.senderRole == CLIENT)    // client should be on right and current user is client
            || (!isClientOnRight && message.senderRole != CLIENT)) {        // only client should be on left and current
            VIEW_TYPE_OUTGOING                                      // user is not client, so current on right
        } else{
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

        val keepThisMessageOnRight = (isClientOnRight && message.senderRole == CLIENT) || ((isClientOnRight.not() && message.senderRole != CLIENT))

        return when{
            isPrevMsgBySameSender && isNextMsgBySameSender -> {
                when{
                    message.senderId == currentUserId -> MIDDLE_MSG_BY_CURRENT_USER
                    keepThisMessageOnRight -> MIDDLE_MSG_RIGHT
                    else -> MIDDLE_MSG_LEFT
                }
            }
            isPrevMsgBySameSender -> {
                when{
                    message.senderId == currentUserId -> LAST_MSG_BY_CURRENT_USER
                    keepThisMessageOnRight -> LAST_MSG_RIGHT
                    else -> LAST_MSG_LEFT
                }
            }
            isNextMsgBySameSender -> {
                when{
                    message.senderId == currentUserId -> FIRST_MSG_BY_CURRENT_USER
                    keepThisMessageOnRight -> FIRST_MSG_RIGHT
                    else -> FIRST_MSG_LEFT
                }
            }
            else -> {
                when{
                    message.senderId == currentUserId -> SINGLE_MSG_BY_CURRENT_USER
                    keepThisMessageOnRight -> SINGLE_MSG_RIGHT
                    else -> SINGLE_MSG_LEFT
                }
            }
        }
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
}