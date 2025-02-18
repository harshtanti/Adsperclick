package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.databinding.FragmentNotificationListingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.chat.adapters.NotificationsPagingAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListingFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentNotificationListingBinding
    private var isAdmin = false

    private val chatViewModel: ChatViewModel by viewModels()

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
        adapter.refresh() // Forces Paging to re-fetch data
    }

    private fun setUpAdapter(){
        adapter = NotificationsPagingAdapter()
        binding.rvNotificationList.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            chatViewModel.notificationsPager.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }
}

