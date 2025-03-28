package com.adsperclick.media.views.newGroup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.wrapper.addOnPageChangeListener
import com.adsperclick.media.applicationCommonView.wrapper.setupWithViewPager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.databinding.FragmentSelectUserBinding
import com.adsperclick.media.databinding.TabViewBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.newGroup.viewmodel.NewGroupViewModel
import com.adsperclick.media.views.user.adapter.PagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectUserFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentSelectUserBinding
    private lateinit var adapter: PagerAdapter
    private var tabName:String=""
    private val tabsMapping = arrayListOf(
        Constants.EMPLOYEES_SEMI_CAPS,
        Constants.CLIENTS_SEMI_CAPS
    )

    private val viewModel: NewGroupViewModel by navGraphViewModels(R.id.new_group_navigation) {
        defaultViewModelProviderFactory
    }
    private var isFromGroupProfile = false
    private var groupCompanyName:String?=null
    private var groupId:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFromGroupProfile = arguments?.getBoolean(Constants.GROUP_PROFILE) == true
        groupCompanyName = arguments?.getString(Constants.COMPANY_SINGULAR)
        groupId = arguments?.getString(Constants.GROUP_ID)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding= FragmentSelectUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpHeader()
        fetchData()
        setupViewPagerAdapter()
        setupTabLayout()
        setOnClickListener()
        setUpObservers()
    }

    private fun fetchData(){
        viewModel.getServiceList()
    }

    private fun setUpHeader(){
        binding.header.tvTitle.text = getString(R.string.select_user)
        if(isFromGroupProfile){
            binding.header.btnSave.text = getString(R.string.add_members)
        }else{
            binding.header.btnSave.text = getString(R.string.new_group)
        }
    }

    private fun setOnClickListener(){
        binding.header.btnSave.setOnClickListener(this)
        binding.header.btnBack.setOnClickListener(this)
    }

    private fun setupTabLayout() {
        with(binding) {
            userTabs.setupWithViewPager(viewPager)

            for (position in 0 until tabsMapping.size) {
                val tabBinding = TabViewBinding.inflate(
                    LayoutInflater.from(binding.userTabs.context),
                    binding.userTabs, false
                )
                tabBinding.textTabTitle.text = tabsMapping[position]
                userTabs.getTabAt(position)?.customView = tabBinding.root
            }

            userTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tabColorChange(tab.customView, R.color.white, true)
                    tabName = tabsMapping[tab.position]
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {
                    tabColorChange(tab.customView, R.color.RoyalBlue, false)
                }
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
            tabColorChange(
                userTabs.getTabAt(viewModel.selectedTabPosition)?.customView,
                R.color.white, true)
            tabName = tabsMapping[viewModel.selectedTabPosition]
        }
    }


    private fun tabColorChange(view: View?, tabTextColorId: Int, isSelected: Boolean) {

        try{
            val tabLayoutToShip = view?.findViewById<ViewGroup>(R.id.tabLayoutContainer) as LinearLayout
            val tabTextToShip = view.findViewById<View>(R.id.textTabTitle) as MaterialTextView

            if (isSelected) {
                tabLayoutToShip.setBackgroundResource(R.drawable.selected_tab)
            } else {
                tabLayoutToShip.background = null
            }
            tabTextToShip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    tabTextColorId
                )
            )
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViewPagerAdapter() {
        adapter = PagerAdapter(requireActivity())

        adapter.addFragment(
            SelectUserCommonFragment.newInstance(getString(R.string.employees)),
            getString(R.string.employees)
        )
        adapter.addFragment(
            SelectUserCommonFragment.newInstance(getString(R.string.clients)),
            getString(R.string.clients)
        )
        with(binding) {
            viewPager.adapter = adapter
            viewPager.addOnPageChangeListener(
                TabLayout.TabLayoutOnPageChangeListener(
                    userTabs
                )
            )
        }
        binding.viewPager.currentItem = viewModel.selectedTabPosition
    }

    private fun setUpObservers(){
        viewModel.listServiceLiveData.observe(viewLifecycleOwner,serviceListObserver)
        viewModel.addGroupMemberLiveData.observe(viewLifecycleOwner,addGroupMemberObserver)
    }

    private val addGroupMemberObserver = Observer<NetworkResult<Boolean>> {
        when(it){

            is NetworkResult.Success ->{
                if (it.data==true){
                    findNavController().popBackStack()
                }
            }

            is NetworkResult.Error ->{
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }
    }

    private val serviceListObserver = Observer<NetworkResult<ArrayList<Service>>> {
        when(it){

            is NetworkResult.Success ->{
                it.data?.let { it1 -> viewModel.serviceList = it1 }
            }

            is NetworkResult.Error ->{
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

            is NetworkResult.Loading ->{}
        }
    }

    private fun singleCompany():Boolean{
        return viewModel.selectedUserSetTotal.size<=1
    }

    private fun addMembersInGroup(){
        groupId?.let { viewModel.addGroupMember(it,viewModel.selectedUserSet) }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.header.btnSave -> {
                when {
                    isFromGroupProfile && !groupCompanyName.isNullOrEmpty() && viewModel.selectedUserSetTotal.size == 1 && viewModel.selectedUserSetTotal.first() != groupCompanyName -> {
                        Toast.makeText(
                            context,
                            "Please! Select clients of only $groupCompanyName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isFromGroupProfile && viewModel.selectedUserSet.size == 0 ->{
                        Toast.makeText(context, "Please! Select Members", Toast.LENGTH_SHORT).show()
                    }
                    isFromGroupProfile && singleCompany() -> {
                        addMembersInGroup()
                    }
                    isFromGroupProfile -> {
                        Toast.makeText(
                            context,
                            "Selected Clients can be of only one company",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    singleCompany() -> {
                        findNavController().navigate(R.id.action_selectUserFragment_to_newGroupFragment)
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            "Selected Clients can be of only one company",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            binding.header.btnBack ->{
                findNavController().popBackStack()
            }
        }
    }
}