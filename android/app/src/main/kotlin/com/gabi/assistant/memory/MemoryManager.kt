package com.gabi.assistant.memory

import com.gabi.assistant.data.db.MemoryDao
import com.gabi.assistant.data.model.Memory
import com.gabi.assistant.data.model.MemoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    private val memoryDao: MemoryDao
) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()

    suspend fun getUserName(): String? = memoryDao.getUserName()

    suspend fun remember(type: MemoryType, key: String, value: String, confidence: Float = 1.0f) {
        val existing = memoryDao.getByKey(key)
        if (existing != null) {
            memoryDao.updateValue(key, value)
        } else {
            memoryDao.insert(Memory(type = type, key = key, value = value, confidence = confidence))
        }
    }

    suspend fun recall(key: String): String? {
        val memory = memoryDao.getByKey(key) ?: return null
        memoryDao.incrementUse(memory.id)
        return memory.value
    }

    suspend fun recallAll(type: MemoryType): List<Memory> = memoryDao.getByType(type)

    suspend fun buildContextSummary(): String {
        val name = getUserName()
        val prefs = memoryDao.getByType(MemoryType.PREFERENCE)
        val habits = memoryDao.getByType(MemoryType.HABIT)
        val facts = memoryDao.getByType(MemoryType.FACT)

        return buildString {
            name?.let { append("User's name: $it. ") }
            if (prefs.isNotEmpty()) {
                append("User preferences: ")
                append(prefs.joinToString(", ") { "${it.key}: ${it.value}" })
                append(". ")
            }
            if (habits.isNotEmpty()) {
                append("Known habits: ")
                append(habits.joinToString(", ") { it.value })
                append(". ")
            }
            if (facts.isNotEmpty()) {
                append("Facts: ")
                append(facts.joinToString(", ") { "${it.key}: ${it.value}" })
                append(". ")
            }
        }
    }

    suspend fun forget(key: String) {
        memoryDao.getByKey(key)?.let { memoryDao.delete(it) }
    }

    suspend fun clearAll() = memoryDao.deleteAll()
}
