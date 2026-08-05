package com.javis.assistant.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.javis.assistant.data.model.Memory
import com.javis.assistant.data.model.Message
import com.javis.assistant.data.model.NotificationItem
import com.javis.assistant.data.model.Reminder

@Database(
    entities = [Message::class, Memory::class, NotificationItem::class, Reminder::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JavisDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reminderDao(): ReminderDao
}
