package com.gabi.assistant.ui.components

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Voice and processing control the pose; finishing a clip never changes state. */
enum class AvatarState(val folder: String) {
    IDLE("idle"), LISTENING("listening"), THINKING("thinking"), TALKING("talking")
}

fun resolveAvatarState(listening: Boolean, speaking: Boolean, processing: Boolean): AvatarState = when {
    speaking -> AvatarState.TALKING
    listening -> AvatarState.LISTENING
    processing -> AvatarState.THINKING
    else -> AvatarState.IDLE
}

/** Offline alpha, 24 fps, one decoded frame at a time (also works on Android 8). */
@Composable
fun AvatarAnimation(state: AvatarState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(state, lifecycle) {
        val files = withContext(Dispatchers.IO) {
            context.assets.list("avatar/${state.folder}")?.filter { it.endsWith(".webp") }?.sorted().orEmpty()
        }
        if (files.isEmpty()) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start = SystemClock.uptimeMillis()
            while (isActive) {
                val tick = (SystemClock.uptimeMillis() - start) * 24 / 1000
                val index = (tick % files.size).toInt()
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets.open("avatar/${state.folder}/${files[index]}").use {
                            BitmapFactory.decodeStream(it)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
                if (decoded != null) frame = decoded
                // Slow devices skip late frames instead of slowing the clip or growing a queue.
                val next = start + (tick + 1) * 1000 / 24
                delay((next - SystemClock.uptimeMillis()).coerceAtLeast(1))
            }
        }
    }
    Box(modifier.testTag("avatar_${state.folder}")) {
        frame?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize()) }
    }
}
