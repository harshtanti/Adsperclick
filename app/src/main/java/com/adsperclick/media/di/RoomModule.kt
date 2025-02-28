package com.adsperclick.media.di

import android.content.Context
import androidx.room.Room
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.applicationCommonView.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun getRoomDB(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun getMessagesDao(roomDb: AppDatabase): MessagesDao {
        return roomDb.chatsDao()
    }
}