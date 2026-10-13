package com.gabi.assistant.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** One entrance per room visit. No microphone/audio focus and no automatic replay. */
@Composable
fun RoomIntroVideo(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val finish by rememberUpdatedState(onFinished)
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("asset:///avatar/room_intro.mp4"))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            prepare()
        }
    }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) finish()
            }
            override fun onPlayerError(error: PlaybackException) { finish() }
        }
        val observer = LifecycleEventObserver { _, _ ->
            player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(modifier = modifier, factory = {
        PlayerView(it).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            this.player = player
        }
    }, update = { it.player = player })
}
