package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentFormBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible


class FormFragment : Fragment() {

    private lateinit var binding: FragmentFormBinding
    private lateinit var userType:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userType = it.getString(Constants.USER_TYPE_SEMI_CAPS).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFormBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTitle()
        setUpHint()
        setUpPlaceholder()
        setUpVisibility()
        setUpInputType()
        setUpDrawable()
    }

    private fun setUpTitle(){
        binding.title.text=userType
    }

    private fun setUpHint(){
        with(binding){
            firstName.setHint(R.string.first_name)
            lastName.setHint(R.string.last_name)
            companyName.setHint(R.string.company_name)
            gst.setHint(R.string.gst_number)
            email.setHint(R.string.email)
            aadharNumber.setHint(R.string.aadhar_card)
            password.setHint(R.string.password)
            confirmPassword.setHint(R.string.confirm_password)
            services.setHint(R.string.services_subscribed)
            serviceName.setHint(R.string.services_name)
            email.setPlaceHolderText(getString(R.string.example_gmail))
        }
    }

    private fun setUpPlaceholder(){
        with(binding){
            email.setPlaceHolderText(getString(R.string.example_gmail))
        }
    }

    private fun setUpVisibility(){
        with(binding){
            when(userType) {
                Constants.EMPLOYEES_SEMI_CAPS ->{
                    employeeGroup.visible()
                }
                Constants.CLIENTS_SEMI_CAPS ->{
                    clientGroup.visible()
                }
                Constants.SERVICES_SEMI_CAPS ->{
                    serviceName.visible()
                }
                Constants.COMPANIES_SEMI_CAPS ->{
                    companyName.visible()
                }
                else -> {

                }
            }
        }
    }

    private fun setUpInputType() {
        with(binding) {
            firstName.setInputType(InputType.TYPE_CLASS_TEXT)
            lastName.setInputType(InputType.TYPE_CLASS_TEXT)
            companyName.setInputType(InputType.TYPE_CLASS_TEXT)
            gst.setInputType(InputType.TYPE_CLASS_TEXT)
            email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            aadharNumber.setInputType(InputType.TYPE_CLASS_NUMBER)
            password.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            services.setInputType(InputType.TYPE_CLASS_TEXT)
            serviceName.setInputType(InputType.TYPE_CLASS_TEXT)
        }
    }

    private fun setUpDrawable(){
        with(binding) {
            password.setStartIcon(password.context,R.drawable.ic_lock)
            password.enablePasswordToggle()

            confirmPassword.setStartIcon(confirmPassword.context,R.drawable.ic_lock)
            confirmPassword.enablePasswordToggle()

            email.setStartIcon(email.context,R.drawable.ic_email)
        }
    }
}