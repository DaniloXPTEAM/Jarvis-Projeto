package com.gabi.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AvatarAnimationTest {
    @get:Rule val compose = createComposeRule()

    private fun pixels(tag: String): IntArray {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        return IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    }

    @Test fun allFourStatesHaveTransparentBackgroundAndMovingFrames() {
        val state = mutableStateOf(AvatarState.IDLE)
        compose.setContent {
            AvatarAnimation(state.value, Modifier.size(240.dp).background(Color.Magenta))
        }
        for (next in AvatarState.entries) {
            compose.runOnIdle { state.value = next }
            Thread.sleep(1500)
            val tag = "avatar_${next.folder}"
            compose.onNodeWithTag(tag).assertIsDisplayed()
            val before = pixels(tag)
            assertEquals("Canvas stays transparent for $next", 0xFFFF00FF.toInt(), before[0])
            assertTrue("Character must have rendered for $next", before.any { it != 0xFFFF00FF.toInt() })
            Thread.sleep(1200)
            val after = pixels(tag)
            assertFalse("Animation must advance for $next", before.contentEquals(after))
        }
        // Thinking lasts 10 seconds. Playback must loop without changing to talking/idle.
        compose.runOnIdle { state.value = AvatarState.THINKING }
        Thread.sleep(12000)
        compose.onNodeWithTag("avatar_thinking").assertIsDisplayed()
        val before = pixels("avatar_thinking")
        Thread.sleep(1000)
        assertFalse(before.contentEquals(pixels("avatar_thinking")))
    }

    @Test fun roomEntranceFinishesExactlyOnce() {
        var completions = 0
        val started = android.os.SystemClock.elapsedRealtime()
        compose.setContent { RoomIntroVideo(Modifier.size(240.dp)) { completions++ } }
        compose.waitUntil(timeoutMillis = 25000) { completions == 1 }
        assertTrue("Entrance must actually play", android.os.SystemClock.elapsedRealtime() - started >= 9500)
        Thread.sleep(1500)
        compose.runOnIdle { assertEquals(1, completions) }
    }
}
