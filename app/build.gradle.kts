plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.adsperclick.media"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adsperclick.media"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom))
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-common-ktx:3.2.1")
    implementation(libs.androidx.paging.common.android)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.transport.api)
    implementation(libs.firebase.functions.ktx) // Required if using Paging in Repository with Flow
    implementation(libs.transport.api)
    implementation(libs.androidx.lifecycle.process) // Required if using Paging in Repository with Flow
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.databinding.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    implementation(libs.plugins.dagger.hilt) // Add Hilt runtime dependency
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt.android) // Hilt runtime
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core.splashscreen)
    implementation(libs.glide)

    // room-db
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)

//    ExoPlayer for video playback: Add
    implementation("com.google.android.exoplayer:exoplayer:2.18.1")
//    PhotoView for zoomable images: Add
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    //Agora
    implementation(libs.voice.sdk)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation ("com.airbnb.android:lottie:6.1.0")

    implementation ("androidx.emoji2:emoji2:1.3.0") // Latest EmojiCompat Library
    implementation ("androidx.emoji2:emoji2-views:1.3.0") // Support for EditText & TextView
    implementation ("androidx.emoji2:emoji2-bundled:1.3.0") // Bundled Emoji support

}