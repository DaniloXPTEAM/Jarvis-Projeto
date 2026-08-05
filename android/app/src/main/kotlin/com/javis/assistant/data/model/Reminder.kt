package com.javis.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val triggerAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val scheduled: Boolean = false
)
