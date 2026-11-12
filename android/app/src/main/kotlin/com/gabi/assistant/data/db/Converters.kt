package com.gabi.assistant.data.db

import androidx.room.TypeConverter
import com.gabi.assistant.data.model.MemoryType
import com.gabi.assistant.data.model.MessageRole
import com.gabi.assistant.data.model.MessageStatus

class Converters {
    @TypeConverter fun fromRole(value: MessageRole): String = value.name
    @TypeConverter fun toRole(value: String): MessageRole = MessageRole.valueOf(value)
    @TypeConverter fun fromStatus(value: MessageStatus): String = value.name
    @TypeConverter fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
    @TypeConverter fun fromMemoryType(value: MemoryType): String = value.name
    @TypeConverter fun toMemoryType(value: String): MemoryType = MemoryType.valueOf(value)
}
