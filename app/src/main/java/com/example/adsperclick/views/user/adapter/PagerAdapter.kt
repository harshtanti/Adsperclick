package com.example.adsperclick.views.user.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.adsperclick.applicationCommonView.wrapper.FragmentStateAdapterWrapper

class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapterWrapper(fragmentActivity) {

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getPageTitle(position: Int): CharSequence {
        return mFragmentTitleList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    override fun getItemCount(): Int  = mFragmentList.size


    override fun createFragment(position: Int): Fragment {
        return mFragmentList[position]
    }
}