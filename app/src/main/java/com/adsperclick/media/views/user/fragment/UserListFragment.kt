package com.adsperclick.media.views.user.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.wrapper.addOnPageChangeListener
import com.adsperclick.media.applicationCommonView.wrapper.setupWithViewPager
import com.adsperclick.media.databinding.FragmentUserListBinding
import com.adsperclick.media.databinding.TabViewBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.user.adapter.PagerAdapter
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserListFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentUserListBinding
    private lateinit var adapter: PagerAdapter
    private var tabName:String=""
    private val tabsMapping = arrayListOf(Constants.EMPLOYEES_SEMI_CAPS,Constants.CLIENTS_SEMI_CAPS,Constants.SERVICES_SEMI_CAPS,Constants.COMPANIES_SEMI_CAPS)

    private val userViewModel:UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPagerAdapter()
        setupTabLayout()
        setOnClickListener()
    }

    private fun setOnClickListener(){
//        binding.addDetails.setOnClickListener(this)
    }


    private fun setupTabLayout() {
        with(binding) {
            userTabs.setupWithViewPager(viewPager)

            for (position in 0 until tabsMapping.size) {
                val tabBinding = TabViewBinding.inflate(
                    LayoutInflater.from(binding.userTabs.context),
                    binding.userTabs, false
                )
                //binding custom tab view
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
                userTabs.getTabAt(userViewModel.selectedTabPosition)?.customView,
                R.color.white, true)
            tabName = tabsMapping[userViewModel.selectedTabPosition]
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
            CommonFragment.newInstance(getString(R.string.employees)),
            getString(R.string.employees)
        )
        adapter.addFragment(
            CommonFragment.newInstance(getString(R.string.clients)),
            getString(R.string.clients)
        )
        adapter.addFragment(
            CommonFragment.newInstance(getString(R.string.services)),
            getString(R.string.services)
        )
        adapter.addFragment(
            CommonFragment.newInstance(getString(R.string.companies)),
            getString(R.string.companies)
        )
        with(binding) {
            viewPager.adapter = adapter
            viewPager.addOnPageChangeListener(
                TabLayout.TabLayoutOnPageChangeListener(
                    userTabs
                )
            )
        }
        binding.viewPager.currentItem = userViewModel.selectedTabPosition
    }

    override fun onClick(v: View?) {
        when(v){

        }
    }
}