package com.adsperclick.media.views.setting.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions

class SettingFragment : Fragment() {

    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userName = "Setting"
        val drawable = UtilityFunctions.generateInitialsDrawable(
            binding.imgProfileDp.context,  userName)
        binding.imgProfileDp.setImageDrawable(drawable)
    }
}