package com.adsperclick.media.api

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.utils.Constants

@Dao
interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /*suspend */fun insertMessage(message: Message)/* :Int*/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /*suspend */fun insertMessageList(messages: List<Message>)/* :Int*/

    @Query("SELECT * FROM messages WHERE groupId = :roomId ORDER BY timestamp ASC")
    fun getChatsForThisRoom(roomId: String) : LiveData<List<Message>>

    @Query("DELETE FROM messages")
    fun clearAllMessages()


    // Below function is used when user enters a group-chat, it checks timestamp of last message
    // in Room-db from this group, if no message from this group is in room-db, it returns "0" using "COALESCE"
    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM messages WHERE groupId = :groupId")
    /*suspend */fun getLatestMsgTimestampOrZero(groupId: String): Long?

    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp DESC")
    /*suspend */fun getPagedChatsForRoom(groupId: String): List<Message>

    @Query("SELECT COUNT(*) FROM messages WHERE groupId = :groupId")
    /*suspend */fun getMessageCount(groupId: String): Int


    @Query("SELECT * FROM messages WHERE groupId = :groupId AND msgType = :msgType ORDER BY timestamp DESC LIMIT 1")
    fun getLastMsgOfGivenType(groupId: String, msgType: Int): Message?
}
