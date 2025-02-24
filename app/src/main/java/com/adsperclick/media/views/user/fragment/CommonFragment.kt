package com.adsperclick.media.views.user.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.FragmentCommonBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.showToast
import com.adsperclick.media.views.user.adapter.CommonAdapter
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"

@AndroidEntryPoint
class CommonFragment : Fragment(), View.OnClickListener {
    // TODO: Rename and change types of parameters
    private lateinit var tabName: String
    private lateinit var binding: FragmentCommonBinding
    private var searchTxt = Constants.EMPTY
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: CommonAdapter
    private var listener: CommonAdapter.CommunityListener ?= null

    private val userViewModel: UserViewModel by viewModels()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabName = it.getString(ARG_PARAM1).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
        setUpAdapter()
        setUpSearching()

    }

    private fun setOnClickListener(){
        binding.addDetails.setOnClickListener(this)
    }

    private fun setUpAdapter(){
        adapter= CommonAdapter()
        listener = object :CommonAdapter.CommunityListener{
            override fun btnDelete(bucketName:String, id:String) {
                if(!(bucketName == "null" || id == "null")){
                    when(tabName){
                        Constants.SERVICES_SEMI_CAPS -> {
                            // Remove item from serviceList
//                            serviceList = serviceList.filter { it.id != id }
//                            adapter.submitList(serviceList)
                        }
                        Constants.COMPANIES_SEMI_CAPS -> {
                            // Remove item from companyList
//                            companyList = companyList.filter { it.id != id }
//                            adapter.submitList(companyList)
                        }
                        Constants.EMPLOYEES_SEMI_CAPS->{

                        }
                        Constants.CLIENTS_SEMI_CAPS -> {

                        }
                        else -> {
                            // Handle other cases if needed
                        }
                    }
                }
            }

            override fun btnInfo(bucketName:String, id:String, name:String) {
                if(!(bucketName == "null" || id == "null" || name == "null")){
                    when(tabName){
                        Constants.EMPLOYEES_SEMI_CAPS->{
                            val bundle = Bundle().apply {
                                putString(Constants.USER_TYPE_SEMI_CAPS, tabName)
                                putString(Constants.USER_NAME, name)
                            }
                            findNavController().navigate(R.id.action_navigation_user_to_userInfoFragment,bundle)
                        }
                        Constants.CLIENTS_SEMI_CAPS -> {
                            val bundle = Bundle().apply {
                                putString(Constants.USER_TYPE_SEMI_CAPS, tabName)
                                putString(Constants.USER_NAME, name)
                            }
                            findNavController().navigate(R.id.action_navigation_user_to_userInfoFragment,bundle)
                        }
                        else -> {
                            // Handle other cases if needed
                        }
                    }
                }
            }

        }
        adapter.setData(tabName,listener)
        binding.rvUser.adapter=adapter
        collectUiData(searchTxt,tabName)
    }

    private fun setUpSearching(){
        binding.etSearchBar.addTextChangedListener { text ->
            handler.removeCallbacksAndMessages(null) // Remove previous callbacks
            val query = text.toString().trim() // Trim spaces

            when {
                query.isNotEmpty() && query.length >= 3 -> {
                    handler.postDelayed({
                        collectUiData(query, tabName)
                    }, 500) // Delay search by 500ms
                }
                query.isEmpty() && query.length < 3 -> {
                    collectUiData(Constants.EMPTY, tabName) // Reset list if input is empty
                }
                else -> {
                    context?.showToast("Search must have at least 3 characters") // Show min char message
                }
            }
        }
    }

    private fun collectUiData(searchTxt:String,tabName:String) {
        lifecycleScope.launch {
            when(tabName) {
                Constants.EMPLOYEES_SEMI_CAPS -> {
                    userViewModel.getUserListData(searchTxt, 2).collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }

                Constants.CLIENTS_SEMI_CAPS -> {
                    userViewModel.getUserListData(searchTxt, 1).collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }

                Constants.SERVICES_SEMI_CAPS -> {
                    userViewModel.getServiceListData(searchTxt).collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }

                Constants.COMPANIES_SEMI_CAPS -> {
                    userViewModel.getCompanyListData(searchTxt).collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }

                else -> {
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.addDetails -> {
                val bundle = Bundle().apply {
                    putString(Constants.USER_TYPE_SEMI_CAPS, tabName) // Pass your data
                }
                findNavController().navigate(R.id.action_navigation_user_to_form_fragment, bundle)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            CommonFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}