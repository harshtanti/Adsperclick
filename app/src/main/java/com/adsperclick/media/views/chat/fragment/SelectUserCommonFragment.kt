package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.adsperclick.media.databinding.FragmentSelectUserCommonBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.chat.adapters.SelectUserCommonAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


private const val ARG_PARAM1 = "param1"


@AndroidEntryPoint
class SelectUserCommonFragment : Fragment() {

    private lateinit var binding: FragmentSelectUserCommonBinding
    private var tabName: String? = null
    private lateinit var adapter: SelectUserCommonAdapter
    private var listener: SelectUserCommonAdapter.GroupListener ?= null

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabName = it.getString(ARG_PARAM1)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSelectUserCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()

    }

    private fun setUpAdapter(){
        adapter= SelectUserCommonAdapter()
        listener = object : SelectUserCommonAdapter.GroupListener{
            override fun btnCheck(bucketName: String, id: String, isSelected: Boolean) {
                if (bucketName != "null" && id != "null") {
                    when (bucketName) {
                        Constants.EMPLOYEES_SEMI_CAPS -> {
                            chatViewModel.employeeList.map { item ->
                                if (item.id == id) item.copy(isSelected = isSelected) else item
                            }
                        }

                        Constants.CLIENTS_SEMI_CAPS -> {
                            chatViewModel.clientList.map { item ->
                                if (item.id == id) item.copy(isSelected = isSelected) else item
                            }
                        }
                    }
                }
            }

        }
        adapter.setData(tabName,listener)
        binding.rvUser.adapter=adapter
        when (tabName) {
            Constants.EMPLOYEES_SEMI_CAPS -> {
                adapter.submitList(chatViewModel.employeeList)
            }

            Constants.CLIENTS_SEMI_CAPS -> {
                adapter.submitList(chatViewModel.clientList)
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CommonFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            SelectUserCommonFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }


}