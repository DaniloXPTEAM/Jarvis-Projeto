package com.gabi.assistant.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarStateTest {
    @Test fun idleWhenNoVoiceOrRequest() {
        assertEquals(AvatarState.IDLE, resolveAvatarState(false, false, false))
    }
    @Test fun processingKeepsThinkingUntilSpeechActuallyStarts() {
        repeat(100) { assertEquals(AvatarState.THINKING, resolveAvatarState(false, false, true)) }
        assertEquals(AvatarState.TALKING, resolveAvatarState(false, true, true))
        assertEquals(AvatarState.IDLE, resolveAvatarState(false, false, false))
    }
    @Test fun listeningAndSpeechWinOverPendingRequest() {
        assertEquals(AvatarState.LISTENING, resolveAvatarState(true, false, true))
        assertEquals(AvatarState.TALKING, resolveAvatarState(true, true, true))
    }
}
