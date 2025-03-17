package com.adsperclick.media.views.homeActivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.databinding.ActivityHomeBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.FCM.ID_OF_GROUP_TO_OPEN
import com.adsperclick.media.utils.Constants.GROUP_ID
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    // This is the activity in which Bottom-nav graph is created, basically
    // fragment-container for navigation across all fragments
    lateinit var binding: ActivityHomeBinding
    private var isAdmin=false

    private lateinit var navController: NavController


    @Inject
    lateinit var tokenManager: TokenManager

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
        isAdmin = tokenManager.getUser()?.role == Constants.ROLE.ADMIN
        if (!isAdmin) {
            binding.bottomNavigation.menu.removeItem(R.id.navigation_user) // Hides "User"
        }
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_chat ||
                destination.id == R.id.navigation_user ||
                destination.id == R.id.navigation_setting) {
                binding.bottomNavigation.visibility = View.VISIBLE
            } else {
                binding.bottomNavigation.visibility = View.GONE
            }
        }

        intent?.let {
            handleIntent(it)
        }

        controlBottomPadding()

        requestNotificationPermission()
    }

    private fun controlBottomPadding(){
        val bottomNavView = binding.bottomNavigation
        bottomNavView.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
    }

    private fun requestNotificationPermission() {

        // In newer android versions it is mandatory to take user permission before sending him notifications!
        // That is y on phone u get pop-up to allow notifications then only notificatinos are allowed on phone
        // So this function checks if phone user has a high Android version or not, and if he has a high android
        // version, it requests for the permission to allow the sending of notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle new intent if the activity is already running
        intent?.let {
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent) {
        val groupId = intent.getStringExtra(ID_OF_GROUP_TO_OPEN)
        if (!groupId.isNullOrEmpty()) {
            Log.d("HomeActivity", "Navigating to ChatFragment with groupId: $groupId")

            val bundle = Bundle().apply {
                putString(ID_OF_GROUP_TO_OPEN, groupId)
            }

            // Ensure we don't create duplicate instances of ChatFragment

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.navigation_chat, true) // Removes any existing ChatFragment
                .build()

            navController.navigate(R.id.navigation_chat, bundle, navOptions)
        }
    }



}


