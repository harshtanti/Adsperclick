package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.FragmentNotificationCreationBinding

class NotificationCreationFragment : Fragment() {

    private lateinit var binding: FragmentNotificationCreationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  FragmentNotificationCreationBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendNotification.setOnClickListener{
            val id = "2323"
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()

            val notification = NotificationMsg(id, title, description)
            binding.etTitle.text.clear()
            binding.etDescription.text.clear()
        }
    }
}

