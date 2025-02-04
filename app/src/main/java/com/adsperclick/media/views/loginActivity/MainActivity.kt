package com.adsperclick.media.views.loginActivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adsperclick.media.R
import com.adsperclick.media.databinding.ActivityMainBinding
import com.adsperclick.media.views.homeActivity.HomeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listener()
    }

    private fun listener(){
        binding.incFile.btnLogin.setOnClickListener{
            startActivity(Intent(this, HomeActivity::class.java))
        }
        binding.incFile.tvCreateNewAcc.setOnClickListener{
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("isAdmin", true)  // ✅ Pass "isAdmin" as true
            startActivity(intent)
        }
    }
}