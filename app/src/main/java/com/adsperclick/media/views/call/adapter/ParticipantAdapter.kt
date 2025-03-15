package com.adsperclick.media.views.call.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.utils.UtilityFunctions

class ParticipantAdapter : ListAdapter<CallParticipant, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgUserAvatar: ImageView = itemView.findViewById(R.id.imgUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val imgMicStatus: ImageView = itemView.findViewById(R.id.imgMicStatus)
        private val participantLayout: View = itemView.findViewById(R.id.participantLayout)

        fun bind(participant: CallParticipant) {
            tvUserName.text = participant.userName

            // Display user avatar
            participant.userProfileImgUrl?.let { url ->
                UtilityFunctions.loadImageWithGlide(
                    imgUserAvatar.context,
                    imgUserAvatar,
                    url
                )
            } ?: run {
                UtilityFunctions.setInitialsDrawable(
                    imgUserAvatar,
                    participant.userName
                )
            }

            // Update mic status icon
            imgMicStatus.setImageResource(
                if (participant.isMuted) R.drawable.ic_mic_off
                else R.drawable.ic_mic
            )

            // Highlight if speaking
            if (participant.isSpeaking) {
                participantLayout.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_speaking_participant
                )
            } else {
                participantLayout.background = ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.bg_participant
                )
            }
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<CallParticipant>() {
        override fun areItemsTheSame(oldItem: CallParticipant, newItem: CallParticipant): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: CallParticipant, newItem: CallParticipant): Boolean {
            return oldItem == newItem
        }
    }
}