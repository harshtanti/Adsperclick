package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding

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
        horizontalCompanyListAdapter = HorizontalCompanyListAdapter(companyList, compId){
            // Handle item click here
        }
        binding.rvHorizontalForCompanyList.apply {
            adapter = horizontalCompanyListAdapter
        }
    }
}

