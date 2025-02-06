package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.databinding.FragmentUserInfoBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions

class UserInfoFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var userType:String
    private lateinit var userName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(Constants.USER_TYPE_SEMI_CAPS).toString()
            userName = it.getString(Constants.USER_NAME).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.tvTitle.text=userType
        val drawable = UtilityFunctions.generateInitialsDrawable(
            binding.imgProfileDp.context,  userName)
        binding.imgProfileDp.setImageDrawable(drawable)
        binding.tvName.text=userName
        setUpClickListener()
    }

    private fun setUpClickListener(){
        binding.header.btnBack.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v){
            binding.header.btnBack -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

}