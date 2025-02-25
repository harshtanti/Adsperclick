package com.adsperclick.media.api

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adsperclick.media.data.dataModels.Message

@Dao
interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageList(messages: List<Message>)

    @Query("SELECT * FROM messages WHERE groupId = :roomId ORDER BY timestamp ASC")
    fun getChatsForThisRoom(roomId: String) : LiveData<List<Message>>

    @Query("DELETE FROM messages")
    fun clearAllMessages()


    // Below function is used when user enters a group-chat, it checks timestamp of last message
    // in Room-db from this group, if no message from this group is in room-db, it returns "0" using "COALESCE"
    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM messages WHERE groupId = :roomId")
    fun getLatestMsgTimestampOrZero(roomId: String): Long

    @Query("SELECT * FROM messages WHERE groupId = :roomId ORDER BY timestamp DESC")
    fun getPagedChatsForRoom(roomId: String): List<Message>

    @Query("SELECT COUNT(*) FROM messages WHERE groupId = :roomId")
    fun getMessageCount(roomId: String): Int
}
