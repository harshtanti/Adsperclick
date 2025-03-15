package com.adsperclick.media.views.homeActivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.databinding.ActivityHomeBinding
import com.adsperclick.media.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    // This is the activity in which Bottom-nav graph is created, basically
    // fragment-container for navigation across all fragments
    lateinit var binding: ActivityHomeBinding
    private var isAdmin=false

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
        val navController = navHostFragment.navController
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

        requestNotificationPermission()
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
}
