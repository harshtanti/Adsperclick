package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.databinding.FragmentNotificationListingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.chat.adapters.NotificationsPagingAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.homeActivity.SharedHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListingFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentNotificationListingBinding
    private var isAdmin = false

    private val chatViewModel: ChatViewModel by viewModels()
    private val sharedViewModel: SharedHomeViewModel by activityViewModels()

    private lateinit var adapter : NotificationsPagingAdapter

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

        updateLastNotificationSeenTime()
        setUpVisibility()
        setUpListener()
        setUpAdapter()
    }

    private fun setUpVisibility(){
        isAdmin = tokenManager.getUser()?.role == Constants.ROLE.ADMIN
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

    override fun onResume() {
        super.onResume()
        adapter.refresh() // Forces Paging to re-fetch data (When admin comes back to this frag
        // from notification creation fragment, it loads previous notifications only, to load
        // newer one's we need to refresh the paging-adapter)
    }

    private fun setUpAdapter(){
        adapter = NotificationsPagingAdapter(chatViewModel.lastTimeWhenNotificationsWereLoaded)
        binding.rvNotificationList.adapter = adapter

        val userRole = sharedViewModel.userData?.role
        lifecycleScope.launch {
            chatViewModel.getNotificationsPager(userRole).collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    private fun updateLastNotificationSeenTime(){
        chatViewModel.lastTimeWhenNotificationsWereLoaded = sharedViewModel.userData?.lastNotificationSeenTime ?: 0L

        // Update last seen time To "CURRENT_TIME" because at the moment we enter, we only see notifications
        // which were sent before that moment, the new incoming notifications will not be shown
        // unless user opens this fragment again, that's why we're saving current time while
        // entering the fragment
        sharedViewModel.userData?.lastNotificationSeenTime = UtilityFunctions.getTime()
        sharedViewModel.userData?.userId?.let { chatViewModel.updateLastNotificationSeenTime(it) }
    }
}

