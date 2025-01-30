package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.databinding.FragmentChatBinding
import com.adsperclick.media.views.chat.adapters.HorizontalCompanyListAdapter

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: HorizontalCompanyListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val companyList = listOf( Company("1", "All"),
            Company("2", "Amazon"),
            Company("3", "Meesho"),
            Company("8", "companyNumber-8"),
            Company("3", "Flipkart"),
            Company("4", "Jumbo Tail"))
        val compId = "3"
        adapter = HorizontalCompanyListAdapter(companyList,null){

        }
        binding.rvHorizontalForCompanyList.adapter = adapter
    }
}

