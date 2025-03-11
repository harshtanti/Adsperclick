package com.adsperclick.media.views.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.databinding.ActivityMainBinding
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.homeActivity.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var tokenManager : TokenManager

    private var dataIsLoading = true

    private val chatViewModel : ChatViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep splash screen visible until we explicitly dismiss it
        val splashScreen = installSplashScreen()

        // Set a condition for keeping the splash screen visible
        splashScreen.setKeepOnScreenCondition {
            // Return true to keep splash screen, false to dismiss
            return@setKeepOnScreenCondition dataIsLoading
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkUserSession()
        setupObserver()
    }

    private fun checkUserSession(){
        if(tokenManager.isUserSignedIn()){
            chatViewModel.syncUser()
        } else{
            // For non-signed in users, dismiss splash screen and stay on login page
            dataIsLoading = false
        }
    }


    fun setupObserver(){
        chatViewModel.userLiveData.observe(this){consumableValue->

            consumableValue.handle {response->

                when(response){
                    is NetworkResult.Success ->{
                        dataIsLoading = false       // To dismiss splash screen

                        if(response.data?.blocked == true){
                            chatViewModel.signOut()
                        } else{
                            changeActivity()
                        }
                    }
                    is NetworkResult.Loading->{}
                    is NetworkResult.Error->{
                        Toast.makeText(this@MainActivity, "${response.message}", Toast.LENGTH_LONG).show()
                        Log.d("skt", "Error: ${response.message}")
                    }
                }
            }
        }
    }

    private fun changeActivity(){
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()             // To Finish this MainActivity(LOGIN ACTIVITY) So that user can't come back to Login page using back-navigation
    }

}
