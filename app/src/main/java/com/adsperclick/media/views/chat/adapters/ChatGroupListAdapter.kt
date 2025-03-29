package com.adsperclick.media.views.chat.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.databinding.ChatGroupListItemBinding
import com.adsperclick.media.utils.Constants.EMPTY
import com.adsperclick.media.utils.Constants.ENDED_THE_CALL
import com.adsperclick.media.utils.Constants.INITIATED_A_CALL
import com.adsperclick.media.utils.Constants.MSG_TYPE
import com.adsperclick.media.utils.Constants.ROLE
import com.adsperclick.media.utils.Constants.SPACE
import com.adsperclick.media.utils.Constants.Time.ONE_HOUR
import com.adsperclick.media.utils.Constants.Time.TWO_HOUR
import com.adsperclick.media.utils.Utils
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible


// If "currentTime" parameter is "-1" we won't show any ring (currently ring visible for Admin only)
class ChatGroupListAdapter(val onGroupChatClickListener: OnGroupChatClickListener,
                           val currentTime : Long, val userId: String,
                           val lastSeenTimeForEachUserEachGroup : Map<String, Map<String, Long?>>?) :
    ListAdapter<GroupChatListingData, ChatGroupListAdapter.GroupChatListViewHolder>(DiffUtil())
{
    interface OnGroupChatClickListener{
        fun onItemClick(chatGroup: GroupChatListingData)
    }

    inner class GroupChatListViewHolder(val binding: ChatGroupListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(chatGroup: GroupChatListingData)
        {
            val lastMessage = chatGroup.lastSentMsg?.let { lastMsg->
                var drawable = 0                // "0" Means no drawable
                // to setup drawable for last message, we show last msg with drawables if last msg is call or sth

                val msgText = when(lastMsg.msgType){
                    MSG_TYPE.IMG_URL -> {
                        drawable = R.drawable.ic_image_drawable_start
                        "Photo"
                    }
                    MSG_TYPE.PDF_DOC -> {
                        drawable = R.drawable.ic_pdf_drawable_start
                        "PDF"
                    }
                    MSG_TYPE.VIDEO -> {
                        drawable = R.drawable.ic_video_drawable_start
                        "Video"
                    }
                    MSG_TYPE.CALL ->{
                        drawable = when(lastMsg.message){
                            INITIATED_A_CALL -> R.drawable.ic_call_ongoing_drawable_start
                            ENDED_THE_CALL -> R.drawable.ic_call_end_drawable_start
                            else -> 0
                        }
                        lastMsg.message
                    }
                    else -> lastMsg.message
                }
                binding.tvLastMsg.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
                (lastMsg.senderName?.split(SPACE)?.get(0) ?: EMPTY) + ": " + msgText
            } ?: run {
                binding.tvLastMsg.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)   // Because! For several null
                EMPTY                           // groups it will start showing drawables coz of recycling
            }

            val lastMsgTime = chatGroup.lastSentMsg?.timestamp?.let {
                Utils.gcListDateFormat(it)
            } ?: run { EMPTY }

            with(binding){
                tvGroupName.text = chatGroup.groupName
                tvLastMsg.text= lastMessage
                tvLastMsgDateTime.text = lastMsgTime
            }


            setGroupDP(chatGroup)

            // Handling rings around group-Dp
            handleRingColors(chatGroup)
            readStatus(chatGroup)

            binding.root.setOnClickListener {
                onGroupChatClickListener.onItemClick(chatGroup)
            }
        }


        private fun handleRingColors(chatGroup: GroupChatListingData){

            if(currentTime != -1L){
                chatGroup.lastSentMsg?.let { msg->

                    val senderRole = msg.senderRole

                    msg.timestamp?.let {timestamp->
                        if(senderRole == ROLE.CLIENT && timestamp + ONE_HOUR < currentTime){
                            // Red ring
                            binding.imgProfileDp.strokeColor = ColorStateList.valueOf(
                                ContextCompat.getColor(binding.imgProfileDp.context, R.color.red_email)
                            )
                            return
                        }

                        else if((senderRole == ROLE.EMPLOYEE || senderRole == ROLE.ADMIN) && timestamp + TWO_HOUR < currentTime){
                            // Purple ring
                            binding.imgProfileDp.strokeColor = ColorStateList.valueOf(
                                ContextCompat.getColor(binding.imgProfileDp.context, R.color.DarkMagenta)
                            )
                            return
                        }
                    }
                }
            }

            // In case the "if" or "else if" was not triggered, the fun will come here, and it means no need to show any ring, so white ring
            binding.imgProfileDp.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(binding.imgProfileDp.context, R.color.white)
            )
        }

        private fun readStatus(chatGroup: GroupChatListingData){
            val userLastSeenTime = lastSeenTimeForEachUserEachGroup?.get(chatGroup.groupId)?.get(userId) ?: 0L
            val lastMsgSentTime = chatGroup.lastSentMsg?.timestamp ?: userLastSeenTime
            msgSeenStatus(userLastSeenTime >= lastMsgSentTime)
        }

        private fun msgSeenStatus(hasUserSeenLastMsg : Boolean){
            if(hasUserSeenLastMsg){
                binding.icBlueDot.gone()
                binding.tvLastMsgDateTime.typeface = ResourcesCompat.getFont(binding.tvLastMsgDateTime.context, R.font.inter_300)
                binding.tvLastMsgDateTime.setTextColor(Color.parseColor("#666666"))
            } else{
                binding.icBlueDot.visible()
                binding.tvLastMsgDateTime.typeface = ResourcesCompat.getFont(binding.tvLastMsgDateTime.context, R.font.inter_500)
                binding.tvLastMsgDateTime.setTextColor(Color.parseColor("#2196F3"))
            }
        }

        private fun setGroupDP(chatGroup: GroupChatListingData){
            chatGroup.groupImgUrl?.let { imageUrl ->
                Utils.loadChatListingImgWithGlide(
                    binding.imgProfileDp.context,
                    binding.imgProfileDp,
                    imageUrl
                )
            } ?: run {
                Utils.setInitialsDrawable(
                    binding.imgProfileDp,
                    chatGroup.groupName
                )
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
