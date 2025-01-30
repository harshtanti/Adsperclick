package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentNotificationListingBinding

class NotificationListingFragment : Fragment() {

    private lateinit var binding: FragmentNotificationListingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationListingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }



}

