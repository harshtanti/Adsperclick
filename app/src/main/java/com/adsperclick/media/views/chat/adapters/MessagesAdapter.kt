package com.adsperclick.media.views.chat.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.databinding.ChatMsgItemIncomingBinding
import com.adsperclick.media.databinding.ChatMsgItemOutgoingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.ENDED_THE_CALL
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
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
import com.adsperclick.media.views.chat.adapters.ChatGroupListAdapter.OnGroupChatClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


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

class MessagesAdapter(private val currentUserId: String,
                      private val isClientOnRight: Boolean, val onMessageClickListener: OnMessageClickListener) :
    ListAdapter<Message, MessagesAdapter.ChatAdapterViewHolder>(DiffUtil()) {

    private var dateStamp :String? = null
    private var showUnreadStamp :Boolean = false
    private var lastVisitedTimestamp :Long? = null

    fun updateLastVisitedTimestamp(newTimestamp: Long?) {
        lastVisitedTimestamp = newTimestamp
    }

    interface OnMessageClickListener{
        fun onItemClick(message: Message)
    }

    inner class ChatAdapterViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(message: Message, msgRelativePosition: Int) {

            val formattedDate = formatMessageTimestamp(message.timestamp!!)
            var isExtraMarginRequired =false

            when (binding) {
                is ChatMsgItemIncomingBinding -> {
                    with(binding)
                    {
                        dateStamp?.let {
                            tvDateStamp.visible()
                            tvDateStamp.text = it
                            isExtraMarginRequired =true
                        }?: run{tvDateStamp.gone()}
                        if(showUnreadStamp){
                            binding.tvUnread.visible()
                            showUnreadStamp = false
                            isExtraMarginRequired =true
                        } else {binding.tvUnread.gone()}


                        fun setupText(){
                            // Show text message, hide media preview
                            textMessageIncoming.visible()
                            mediaPreviewContainer.gone()
                            groupImage.gone()
                            textMessageIncoming.text = message.message
                            tvTime.visible()
                            tvTime.text = formattedDate             // We'll show date in this tv
                        }

                        fun setupImage(){
                            textMessageIncoming.gone()
                            tvTime.gone()
                            mediaPreviewContainer.gone()
                            groupImage.visible()
                            tvTimeInsideImg.text = formattedDate    // We'll show date in this tv
                        }

                        fun setupDocAndVideo(){
                            textMessageIncoming.gone()
                            tvTime.gone()
                            mediaPreviewContainer.visible()
                            groupImage.gone()
                            mediaFileSize.text = formattedDate       // We'll show date in this tv
                        }


                        when(message.msgType){
                            Constants.MSG_TYPE.TEXT -> setupText()

                            Constants.MSG_TYPE.IMG_URL -> {
                                setupImage()
                                Glide.with(binding.imgSharedInGroup)
                                    .load(message.message)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Caches both full-size & resized images
                                    .override(200, 200)  // Resize image to exactly match ImageView dimensions + This prevents out-of-memory problem and makes app faster, as that particular size img is rendered now
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_image)
                                    .error(R.drawable.logout_red)
                                    .into(binding.imgSharedInGroup)     // .into()      For thread safety, glide internally uses Non-UI thread to load images
                                // and prevent UI from hanging
                            }

                            Constants.MSG_TYPE.VIDEO -> {
                                setupDocAndVideo()
                                binding.mediaTypeIcon.setImageResource(R.drawable.ic_image)
                                binding.mediaFileName.text = "Video"
                            }

                            Constants.MSG_TYPE.PDF_DOC -> {
                                setupDocAndVideo()
                                binding.mediaTypeIcon.setImageResource(R.drawable.ic_image)
                                binding.mediaFileName.text = "Document"
                            }

                            Constants.MSG_TYPE.CALL -> {
                                setupDocAndVideo()
                                binding.mediaTypeIcon.setImageResource(R.drawable.call_end_24px)
                                binding.mediaFileName.text = message.message
                            }
                        }



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
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item

                            }
                            LAST_MSG_LEFT -> {
                                viewIncomingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_grey_left)
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item
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
                        dateStamp?.let {
                            tvDateStamp.visible()
                            tvDateStamp.text = it
                            isExtraMarginRequired = true
                        } ?: run{tvDateStamp.gone()}

                        if(showUnreadStamp){
                            binding.tvUnread.visible()
                            isExtraMarginRequired = true
                            showUnreadStamp = false
                        } else {binding.tvUnread.gone()}


                        fun setupText(){
                            // Show text message, hide media preview
                            textMessageOutgoing.visible()
                            mediaPreviewContainer.gone()
                            groupImage.gone()
                            textMessageOutgoing.text = message.message
                            tvTimeOutgoing.visible()
                            tvTimeOutgoing.text = formattedDate             // We'll show date in this tv
                        }

                        fun setupImage(){
                            textMessageOutgoing.gone()
                            tvTimeOutgoing.gone()
                            mediaPreviewContainer.gone()
                            groupImage.visible()
                            tvTimeOutgoingInsideImg.text = formattedDate    // We'll show date in this tv
                        }

                        fun setupDocAndVideo(){
                            textMessageOutgoing.gone()
                            tvTimeOutgoing.gone()
                            mediaPreviewContainer.visible()
                            groupImage.gone()
                            mediaFileSize.text = formattedDate              // We'll show date in this tv
                        }


                        var drawable = 0
                        when(message.msgType){
                            Constants.MSG_TYPE.TEXT -> setupText()

                            Constants.MSG_TYPE.IMG_URL -> {
                                setupImage()
                                Glide.with(binding.imgSharedInGroup)
                                    .load(message.message)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Caches both full-size & resized images
                                    .override(200, 200)  // Resize image to exactly match ImageView dimensions + This prevents out-of-memory problem and makes app faster, as that particular size img is rendered now
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_image)
                                    .error(R.drawable.logout_red)
                                    .into(binding.imgSharedInGroup)     // .into()      For thread safety, glide internally uses Non-UI thread to load images
                                                                        // and prevent UI from hanging
                            }

                            Constants.MSG_TYPE.VIDEO -> {
                                setupDocAndVideo()
                                drawable = R.drawable.ic_image
                                binding.mediaFileName.text = "Video"
                            }

                            Constants.MSG_TYPE.PDF_DOC -> {
                                setupDocAndVideo()
                                drawable = R.drawable.ic_image
                                binding.mediaFileName.text = "Document"
                            }

                            Constants.MSG_TYPE.CALL -> {
                                setupDocAndVideo()
                                drawable = when(message.message){
                                    INITIATED_A_CALL -> R.drawable.ic_call_ongoing_drawable_start
                                    ENDED_THE_CALL -> R.drawable.ic_call_end
                                    else -> 0
                                }
                                binding.mediaFileName.text = message.message
                            }
                        }

                        binding.mediaTypeIcon.setImageResource(drawable)



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
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item
                            }
                            LAST_MSG_BY_CURRENT_USER -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_blue)
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item
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
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item
                            }
                            LAST_MSG_RIGHT-> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_last_msg_dark_grey_right)
                                rootLayoutParams.topMargin = dpToPx(if(isExtraMarginRequired) 10 else 0)  // Extra margin for entire item
                            }
                            SINGLE_MSG_RIGHT -> {
                                viewOutgoingMsgs.setBackgroundResource(R.drawable.bubble_single_msg_dark_grey)
                                rootLayoutParams.topMargin = dpToPx(8)  // Extra margin for entire item
                            }
                        }
                    }
                }
            }



            if(message.msgType != Constants.MSG_TYPE.TEXT)
            {
                binding.root.setOnClickListener{
                    onMessageClickListener.onItemClick(message)
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

        dateStamp = UtilityFunctions.processDateStamp(message.timestamp , prevMsg?.timestamp)

        if(message.senderId != currentUserId){
            lastVisitedTimestamp?.let { lastVisitedTimestamp->
                message.timestamp?.let { msgTimestamp->
                    prevMsg?.timestamp?.let { prevMsgTimestamp->
                        showUnreadStamp = (lastVisitedTimestamp in (prevMsgTimestamp + 1)..<msgTimestamp)
                    }
                }
            }
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