package com.example.adsperclick.views.homeActivity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.adsperclick.R
import com.example.adsperclick.databinding.ActivityHomeBinding
import com.example.adsperclick.views.homeActivity.Fragments.ChatFragment
import com.example.adsperclick.views.homeActivity.Fragments.UserListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        replaceFragment(ChatFragment())     // Because initially we want the "Chat Fragment" to be shown
        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId){
                R.id.btn_chat_frag -> replaceFragment(ChatFragment())
//                R.id.btn_setting_frag -> replaceFragment(MySettingFragment())
                R.id.btn_users_frag -> replaceFragment(UserListFragment())
                else -> {}
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.my_frame_layout, fragment)
        fragmentTransaction.commit()
    }
}