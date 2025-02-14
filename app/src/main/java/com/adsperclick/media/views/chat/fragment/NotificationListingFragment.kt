package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NotificationMsg
import com.adsperclick.media.databinding.FragmentNotificationListingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.chat.adapters.NotificationListAdapter
import com.adsperclick.media.views.homeActivity.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListingFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentNotificationListingBinding
    private var isAdmin = false

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationListingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpVisibility()
        setUpListener()

        val adapter = NotificationListAdapter()
        binding.rvNotificationList.adapter = adapter

        val myList = arrayListOf(
            NotificationMsg("1", "Title1", "Description1 can be a very long description like can be about explaining a task to all the employees or for making a client deal with a task"),
            NotificationMsg("2", "Title2", "Description2"),
            NotificationMsg("3", "Title3", "Description3"),
            NotificationMsg("4", "Title4", "Description4"),
            NotificationMsg("5", "Title5", "Description1 can be a very long description like can be about explaining a task to all the employees or for making a client deal with a task"),
            NotificationMsg("6", "Title6", "Description6"),
            NotificationMsg("7", "Title7", "\"Description1 can be a very long description like can be about explaining a task to all the employees or for making a client deal with a task"),
            NotificationMsg("8", "Title8", "Description8"),
            NotificationMsg("9", "Title9", "Description9")
        )

        adapter.submitList(myList)

    }

    private fun setUpVisibility(){
        isAdmin = tokenManager.getUser()?.role == Constants.ADMIN
        if (!isAdmin) {
            binding.btnAddNotifications.gone()
        }
    }

    private fun setUpListener(){
        binding.btnBack.setOnClickListener(this)
        binding.btnAddNotifications.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v){
            binding.btnBack -> {
                findNavController().popBackStack()
            }
            binding.btnAddNotifications -> {
                findNavController().navigate(R.id.action_notificationListingFragment_to_notificationCreationFragment)
            }
        }
    }

}

