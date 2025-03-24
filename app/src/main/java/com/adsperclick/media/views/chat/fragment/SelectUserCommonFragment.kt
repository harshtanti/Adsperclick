package com.adsperclick.media.views.chat.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.FragmentSelectUserCommonBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.showToast
import com.adsperclick.media.utils.toTitleCase
import com.adsperclick.media.views.chat.adapters.SelectUserCommonAdapter
import com.adsperclick.media.views.chat.viewmodel.NewGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val ARG_PARAM1 = "param1"


@AndroidEntryPoint
class SelectUserCommonFragment : Fragment() {

    private lateinit var binding: FragmentSelectUserCommonBinding
    private lateinit var tabName: String
    private var searchTxt = Constants.EMPTY
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: SelectUserCommonAdapter

    private val viewModel: NewGroupViewModel by navGraphViewModels(R.id.new_group_navigation) {
        defaultViewModelProviderFactory
    }

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
        binding = FragmentSelectUserCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()
        observeUsers(searchTxt = searchTxt, tabName = tabName)
        setUpSearching()
    }

    private fun setUpAdapter() {
        adapter = SelectUserCommonAdapter()

        // Handle selection changes
        val listener = object : SelectUserCommonAdapter.GroupListener {
            override fun btnCheck(bucketName: String, data: CommonData) {
                updateSelectedUsers(data)
            }
        }

        adapter.setData(tabName, listener)
        binding.rvUser.adapter = adapter
    }

    private fun setUpSearching(){
        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null) // Remove previous callbacks
                val query = s.toString().trim().toTitleCase() // Trim spaces

                when {
                    query.isNotEmpty() && query.length >= 3 -> {
                        handler.postDelayed({
                            observeUsers(query, tabName)
                        }, 200) // Delay search by 200ms
                        binding.etSearchBar.error = null
                    }
                    query.isNotEmpty() && query.length < 3 -> {
                        binding.etSearchBar.error = "Search must have at least 3 characters"
                    }
                    query.isEmpty() -> {
                        observeUsers(Constants.EMPTY, tabName)
                        binding.etSearchBar.error = null
                    }
                    else -> {

                    }
                }
            }
        })
    }

    private fun observeUsers(searchTxt:String,tabName:String) {
        lifecycleScope.launch {
            val role = when(tabName){
                Constants.EMPLOYEES_SEMI_CAPS -> 2
                Constants.CLIENTS_SEMI_CAPS -> 1
                else -> 1
            }
            viewModel.getUserListData(searchTxt,role).collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    private fun updateSelectedUsers(data: CommonData) {
        data.id?.let {
            if (data.isSelected) {
                viewModel.selectedUserSet.add(it)
                data.tagName?.let { it1 -> if(it1!="Employee"){viewModel.selectedUserSetTotal.add(it1)} }
            } else {
                viewModel.selectedUserSet.remove(it)
                viewModel.selectedUserSetTotal.remove(data.tagName)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            SelectUserCommonFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }


}