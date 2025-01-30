package com.adsperclick.media.views.user.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.wrapper.addOnPageChangeListener
import com.adsperclick.media.applicationCommonView.wrapper.setupWithViewPager
import com.adsperclick.media.databinding.FragmentUserListBinding
import com.adsperclick.media.databinding.TabViewBinding
import com.adsperclick.media.views.user.adapter.PagerAdapter
import com.adsperclick.media.views.user.viewmodel.UserViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserListFragment : Fragment() {

    private lateinit var binding: FragmentUserListBinding
    private lateinit var adapter: com.adsperclick.media.views.user.adapter.PagerAdapter
    private val tabsMapping = arrayListOf("Employee","Customer")

    @Inject
    lateinit var userViewModel:UserViewModel

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
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {
                    tabColorChange(tab.customView, R.color.RoyalBlue, false)
                }
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
            tabColorChange(
                userTabs.getTabAt(userViewModel.selectedTabPosition)?.customView,
                R.color.white, true)
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
        adapter = com.adsperclick.media.views.user.adapter.PagerAdapter(requireActivity())

        adapter.addFragment(
            CommonFragment.newInstance(getString(R.string.employee)),
            getString(R.string.employee)
        )
        adapter.addFragment(
            CommonFragment.newInstance(getString(R.string.customer)),
            getString(R.string.customer)
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
}