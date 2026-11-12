package com.gabi.assistant.voice

/** Main-thread session state, also testable without Android or a microphone. */
internal class RecognitionSession {
    private var serial = 0L
    private var active: Long? = null
    private var absoluteDeadline = 0L
    private var deadline = 0L
    private var awaitingResult = false
    private var lastPartial = ""
    val isActive: Boolean get() = active != null

    fun start(now: Long): Long? {
        if (isActive) return null
        val id = ++serial
        active = id
        absoluteDeadline = now + 90_000L
        deadline = now + 10_000L
        awaitingResult = false
        lastPartial = ""
        return id
    }
    fun accepts(id: Long): Boolean = active == id
    fun ready(id: Long, now: Long) {
        if (accepts(id) && !awaitingResult) deadline = minOf(now + 30_000L, absoluteDeadline)
    }
    fun partial(id: Long, text: String, now: Long) {
        if (accepts(id) && !awaitingResult && text.isNotBlank() && text != lastPartial) {
            lastPartial = text
            deadline = minOf(now + 15_000L, absoluteDeadline)
        }
    }
    fun ended(id: Long, now: Long) {
        if (accepts(id) && !awaitingResult) {
            awaitingResult = true
            deadline = minOf(now + 8_000L, absoluteDeadline)
        }
    }
    fun timedOut(id: Long, now: Long): Boolean = accepts(id) && now >= deadline
    fun remaining(id: Long, now: Long): Long = if (accepts(id)) (deadline - now).coerceAtLeast(1L) else 0L
    fun finish(id: Long): Boolean {
        if (!accepts(id)) return false
        active = null
        return true
    }
    fun cancel() { active = null }
}
