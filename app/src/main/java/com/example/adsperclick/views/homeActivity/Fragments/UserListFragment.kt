package com.example.adsperclick.views.homeActivity.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.adsperclick.R
import com.example.adsperclick.databinding.FragmentUserListBinding

class UserListFragment : Fragment() {

    private lateinit var binding: FragmentUserListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }
}