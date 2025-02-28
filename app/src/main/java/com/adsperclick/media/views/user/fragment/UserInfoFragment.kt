package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentSettingBinding
import com.adsperclick.media.databinding.FragmentUserInfoBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.user.viewmodel.UserViewModel

class UserInfoFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var userType:String
    private lateinit var userName:String
    private var userImgUrl:String?=null
    private var phoneNumber:String?=null

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(Constants.USER_TYPE_SEMI_CAPS).toString()
            userName = it.getString(Constants.USER_NAME).toString()
            userImgUrl = it.getString(Constants.USER_IMAGE)
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
        binding.header.btnSave.gone()
        userImgUrl?.let { imageUrl ->
            UtilityFunctions.loadImageWithGlide(
                binding.imgProfileDp.context,
                binding.imgProfileDp,
                imageUrl
            )
        } ?: run {
            UtilityFunctions.setInitialsDrawable(
                binding.imgProfileDp,
                userName
            )
        }
        binding.tvName.text=userName
        setUpVisibility()
        setUpClickListener()
    }

    private fun setUpVisibility(){
        when(userType){
            Constants.EMPLOYEES_SEMI_CAPS,Constants.CLIENTS_SEMI_CAPS->{
                binding.cvServices.gone()
            }
            Constants.COMPANIES_SEMI_CAPS->{
                binding.cvEmail.gone()
                binding.btnBlock.gone()
                binding.cvPhone.gone()
            }
        }
    }

    private fun setUpClickListener(){
        binding.header.btnBack.setOnClickListener(this)
        binding.cvServices.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v){
            binding.header.btnBack -> {
                findNavController().popBackStack()
            }
            binding.cvServices -> {

            }
        }
    }

}