package com.adsperclick.media.applicationCommonView

import androidx.room.Database
import androidx.room.RoomDatabase
import com.adsperclick.media.api.MessagesDao
import com.adsperclick.media.data.dataModels.Message


@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatsDao(): MessagesDao
}
