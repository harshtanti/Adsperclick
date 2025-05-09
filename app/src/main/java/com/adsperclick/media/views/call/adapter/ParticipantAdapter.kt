package com.adsperclick.media.views.call.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.databinding.ItemCallParticipantBinding
import com.adsperclick.media.utils.Utils
import com.adsperclick.media.utils.visible

class ParticipantAdapter : ListAdapter<CallParticipant, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemCallParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(val binding: ItemCallParticipantBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: CallParticipant) {
            with(binding) {
                tvUserName.text = participant.userName

                // Display user avatar
                if (participant.userProfileImgUrl.isNullOrEmpty().not()){
                    Utils.loadImageWithGlide(
                        imgUserAvatar.context,
                        imgUserAvatar,
                        participant.userProfileImgUrl
                    )
                }else{
                    Utils.setInitialsDrawable(
                        imgUserAvatar,
                        participant.userName
                    )
                }

                // Update mic status icon
                imgMicStatus.setImageResource(
                    if (participant.muteOn) R.drawable.ic_mic_off
                    else R.drawable.ic_mic
                )

                // Highlight if speaking with blue border and show audio wave indicator
                if (participant.speakerOn) {
                    // Apply speaking background (blue border)
                    participantLayout.background = ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_speaking_participant
                    )

                    // Show audio wave indicator
                    audioWaveIndicator.visible()
                } else {
                    // Apply regular background (no border)
                    participantLayout.background = ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_participant
                    )

                    // Hide audio wave indicator
                    audioWaveIndicator.visibility = View.INVISIBLE
                }
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