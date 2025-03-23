package com.adsperclick.media.applicationCommonView

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

//@HiltAndroidApp
//class AdsperclickApplication:Application(), LifecycleObserver {
//
//    companion object {
//        lateinit var appLifecycleObserver: AppLifecycleObserver
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        // Initialize Firebase
//        FirebaseApp.initializeApp(this)
//
//        appLifecycleObserver = AppLifecycleObserver()
//        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
//    }
//}


@HiltAndroidApp
class AdsperclickApplication : Application() {

    companion object {
        lateinit var appLifecycleObserver: AppLifecycleObserver
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        appLifecycleObserver = AppLifecycleObserver()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)


        // Initialize EmojiCompat with Bundled Configuration
        val config = BundledEmojiCompatConfig(this)
            .setReplaceAll(true) // Ensures emojis are replaced with compatible ones
        EmojiCompat.init(config)
    }
}