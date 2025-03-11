package com.adsperclick.media.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}

class VersionProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appVersion: Long
        get() = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode  // Use longVersionCode for API 28+
            } else {
                packageInfo.versionCode.toLong()  // Use versionCode for older versions
            }
        } catch (e: Exception) {
            1 // Fallback version
        }
}