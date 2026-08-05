package com.javis.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.javis.assistant.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Query("SELECT * FROM reminders WHERE triggerAt > :now ORDER BY triggerAt ASC")
    suspend fun getPending(now: Long = System.currentTimeMillis()): List<Reminder>

    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reminders WHERE triggerAt <= :now")
    suspend fun deletePast(now: Long = System.currentTimeMillis())
}
