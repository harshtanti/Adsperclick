package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.FragmentNotificationCreationBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationCreationFragment : Fragment() {

    private lateinit var binding: FragmentNotificationCreationBinding

    private val chatViewModel : ChatViewModel by viewModels()

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

        listeners()
        observer()
    }

    private fun listeners(){
        binding.btnSendNotification.setOnClickListener{
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()

            val sentTo = when{
                binding.cbSelectClients.isChecked && binding.cbSelectEmployees.isChecked ->{
                    Constants.SEND_TO.BOTH
                }
                binding.cbSelectClients.isChecked ->{
                    Constants.SEND_TO.CLIENT
                }
                binding.cbSelectEmployees.isChecked ->{
                    Constants.SEND_TO.EMPLOYEE
                }

                else ->{
                    Toast.makeText(context, "Select client/employee to send them", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val notification = NotificationMsg(null, title, description, sentTo)
            binding.etTitle.text?.clear()
            binding.etDescription.text?.clear()
            binding.cbSelectClients.isChecked = false
            binding.cbSelectEmployees.isChecked = false

            chatViewModel.createNotification(notification)
        }

        binding.btnBack.setOnClickListener{
            findNavController().navigateUp()
        }
    }

    private fun observer(){

        chatViewModel.createNotificationLiveData.observe(viewLifecycleOwner){ response ->
            when(response){
                is NetworkResult.Success ->{
                    Toast.makeText(context, "Notification Sent!", Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading ->{}

                is NetworkResult.Error ->{
                    Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

