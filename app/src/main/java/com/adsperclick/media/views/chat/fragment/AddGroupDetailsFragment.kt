package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentAddGroupDetailsBinding
import com.adsperclick.media.databinding.FragmentSelectUserCommonBinding

class AddGroupDetailsFragment : Fragment() {

    private lateinit var binding: FragmentAddGroupDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddGroupDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

}