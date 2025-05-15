package com.adsperclick.media.views.splashActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.databinding.ActivitySplashBinding
import com.adsperclick.media.services.FCM_Service
import com.adsperclick.media.utils.Constants.FCM.ID_OF_GROUP_TO_OPEN
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.homeActivity.HomeActivity
import com.adsperclick.media.views.login.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    @Inject
    lateinit var tokenManager : TokenManager

    @Inject
    lateinit var fcmService: FCM_Service

    private val chatViewModel : ChatViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        binding.lottieSplashScreen.speed =2.7f

        checkUserSession()
        setupObserver()
    }

    private fun checkUserSession(){
        if(tokenManager.isUserSignedIn()){
            chatViewModel.syncUser()
        } else{
            // For non-signed in users, dismiss splash screen and goto login page
            gotoLoginActivity()
        }
    }


    private fun setupObserver(){
        chatViewModel.userLiveData.observe(this){consumableValue->
            consumableValue.handle {response->

                when(response){
                    is NetworkResult.Success ->{
                        if(response.data?.blocked == true){
                            chatViewModel.signOut()
                        } else{
                            gotoHomeActivity()
                        }
                    }
                    is NetworkResult.Loading->{}
                    is NetworkResult.Error->{
                        Toast.makeText(this@SplashActivity, "${response.message}", Toast.LENGTH_LONG).show()
                        Log.d("skt", "Error: ${response.message}")
                        gotoLoginActivity()
                    }
                }
            }
        }
    }

    private fun gotoHomeActivity(){
        Log.d("skt", "Intent Extras: ${intent?.extras}")
        val groupId = intent?.getStringExtra(ID_OF_GROUP_TO_OPEN) // Ensure we check the existing intent
        Log.d("skt", "Received Group ID: $groupId")

        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            if (!groupId.isNullOrEmpty()) {
                putExtra(ID_OF_GROUP_TO_OPEN, groupId)
            }
        }

        startActivity(homeIntent)
        finish() // To Finish this MainActivity(LOGIN ACTIVITY) So that user can't come back to Login page using back-navigation
    }

    fun gotoLoginActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // To Finish this SplashActivity So user can't come back using back-navigation
    }
}

/*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var tokenManager : TokenManager

    private var dataIsLoading = true
    private lateinit var navController: NavController


    private val chatViewModel : ChatViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // Keep splash screen visible until we explicitly dismiss it
//        val splashScreen = installSplashScreen()
//        Log.d("skt", "Intent Extras OnCreate: ${intent?.extras}")
//
//
//        // Set a condition for keeping the splash screen visible
//        splashScreen.setKeepOnScreenCondition {
//            // Return true to keep splash screen, false to dismiss
//            return@setKeepOnScreenCondition dataIsLoading
//        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        intent?.let {
            handleIntent(it)
        }

//        checkUserSession()
//        setupObserver()
    }

    private fun handleIntent(intent: Intent) {
        val groupId = intent.getStringExtra(ID_OF_GROUP_TO_OPEN)
        if (!groupId.isNullOrEmpty()) {
            Log.d("HomeActivity", "Navigating to ChatFragment with groupId: $groupId")

            val bundle = Bundle().apply {
                putString(ID_OF_GROUP_TO_OPEN, groupId)
            }

            // Ensure we don't create duplicate instances of fragment

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.splashFragment, true) // Removes any existing ChatFragment
                .build()

            navController.navigate(R.id.splashFragment, bundle, navOptions)
        }
    }

//    private fun checkUserSession(){
//        if(tokenManager.isUserSignedIn()){
//            chatViewModel.syncUser()
//        } else{
//            // For non-signed in users, dismiss splash screen and stay on login page
//            dataIsLoading = false
//        }
//    }


//    fun setupObserver(){
//        chatViewModel.userLiveData.observe(this){consumableValue->
//
//            consumableValue.handle {response->
//
//                when(response){
//                    is NetworkResult.Success ->{
//                        dataIsLoading = false       // To dismiss splash screen
//
//                        if(response.data?.blocked == true){
//                            chatViewModel.signOut()
//                        } else{
//                            changeActivity()
//                        }
//                    }
//                    is NetworkResult.Loading->{}
//                    is NetworkResult.Error->{
//                        Toast.makeText(this@MainActivity, "${response.message}", Toast.LENGTH_LONG).show()
//                        Log.d("skt", "Error: ${response.message}")
//                    }
//                }
//            }
//        }
//    }

//    private fun changeActivity(){
//        Log.d("skt", "Intent Extras: ${intent?.extras}")
//        val groupId = intent?.getStringExtra(ID_OF_GROUP_TO_OPEN) // Ensure we check the existing intent
//        Log.d("skt", "Received Group ID: $groupId")
//
//        val homeIntent = Intent(this, HomeActivity::class.java).apply {
//            if (!groupId.isNullOrEmpty()) {
//                putExtra(ID_OF_GROUP_TO_OPEN, groupId)
//            }
//        }
//
//        startActivity(homeIntent)
//        finish() // To Finish this MainActivity(LOGIN ACTIVITY) So that user can't come back to Login page using back-navigation
//    }

}



*/


