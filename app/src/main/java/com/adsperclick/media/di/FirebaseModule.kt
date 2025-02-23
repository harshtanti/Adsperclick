package com.adsperclick.media.di

import com.adsperclick.media.api.ApiService
import com.adsperclick.media.api.ApiServiceImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

/*
    Offline Persistence of Data in Firestore:
    So that even if internet is not there user can see older data when using app..

    Firestore offers two types of caching to ensure that users can see older data even when offline.

    Default Caching:
    This is "LRU-based caching" (Least Recently Used).
    Data is cached temporarily and persists for a short period, typically only while the app is running.
    Once the app is closed and reopened, the data will not be available.

    Disk-Based Caching:
    This type of caching stores data on the device's disk (ROM) rather than in RAM, similar to RoomDB.
    Data is stored in the device's permanent storage, which can increase the app size.
    Although this data is stored more persistently, Firestore manages it and may delete some data as needed.
    */

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            // Optional: Set Firestore settings here (to set "LRU based caching" or "disk based caching"
            // we're using "disk-based-caching"
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
        }
    }


    @Provides
    @Singleton
    fun providefirebaseAuth(): FirebaseAuth{
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideApiService(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ): ApiService {
        return ApiServiceImpl(db, auth)
    }
}