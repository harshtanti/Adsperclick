package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentNewGroupBinding
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class NewGroupFragment : Fragment() {

    private lateinit var binding: FragmentNewGroupBinding

    @Inject
    lateinit var chatViewModel: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNewGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpHeader()
        setUpHint()
        setUpInputType()
        getSelectedUser()
    }

    private fun setUpHeader(){
        binding.header.tvTitle.text = getString(R.string.new_group)
        binding.header.btnSave.gone()
    }

    private fun setUpHint(){
        with(binding){
            groupName.setHint(R.string.enter_group_name)
            serviceName.setHint(R.string.select_service)
        }
    }

    private fun setUpInputType(){
        with(binding){
            groupName.setInputType(InputType.TYPE_CLASS_TEXT)
            serviceName.setInputType(InputType.TYPE_CLASS_TEXT)
        }
    }

    private fun getSelectedUser(){
        val (selectedEmployees, selectedClients) = chatViewModel.getSelectedUsers()
        val name = if(selectedEmployees.isNotEmpty()){selectedEmployees[0].name} else "ZZ"
        val drawable = UtilityFunctions.generateInitialsDrawable(
            binding.imgProfileDp.context, name ?: "A")
        binding.imgProfileDp.setImageDrawable(drawable)
    }

}