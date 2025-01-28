package com.adsperclick.media.applicationCommonView.wrapper

import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

abstract class FragmentStateAdapterWrapper(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private var addOnPageChangeListener: ViewPager.OnPageChangeListener? = null

    abstract fun getPageTitle(position: Int): CharSequence?

    fun attachTabLayout(tabLayout: TabLayout, viewPager: ViewPager2) {
        TabLayoutMediator(
            tabLayout, viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = getPageTitle(position)
        }.attach()
    }

    fun addOnPageChangeListener(viewPager: ViewPager2, pageChangeListener: ViewPager.OnPageChangeListener) {
        if(this.addOnPageChangeListener == null) {
            //Listener is not null. Already registered
            viewPager.registerOnPageChangeCallback(onChangeCallback)
        }
        this.addOnPageChangeListener = pageChangeListener

    }

    private val onChangeCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            addOnPageChangeListener?.onPageScrolled(position, positionOffset, positionOffsetPixels)
        }

        override fun onPageScrollStateChanged(state: Int) {
            addOnPageChangeListener?.onPageScrollStateChanged(state)
        }

        override fun onPageSelected(position: Int) {
            addOnPageChangeListener?.onPageSelected(position)
        }
    }
}

fun TabLayout.setupWithViewPager(viewPager: ViewPager2) {
    (viewPager.adapter as? FragmentStateAdapterWrapper)?.attachTabLayout(this, viewPager)
}

fun ViewPager2.addOnPageChangeListener(pageChangeListener: ViewPager.OnPageChangeListener) {
    (adapter as? FragmentStateAdapterWrapper)?.addOnPageChangeListener(this, pageChangeListener)
}