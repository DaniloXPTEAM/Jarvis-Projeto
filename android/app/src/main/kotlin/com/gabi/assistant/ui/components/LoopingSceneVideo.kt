package com.gabi.assistant.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Toca um vídeo curto em loop, mudo, sem controles — usado pras cenas animadas
 * do Quarto da Gabi (fundo do quarto + personagem já "assados" juntos no
 * vídeo). Vídeo em vez de PNG + canal alfa de propósito: compositar alfa de
 * vídeo em tempo real no Android exigiria um shader próprio (RenderEffect só
 * a partir da API 31, o app mira minSdk 26) — assar a cena inteira num MP4
 * comum resolve sem essa complexidade extra.
 */
@Composable
fun LoopingSceneVideo(
    rawResId: Int,
    modifier: Modifier = Modifier,
    /** ZOOM (padrão) = preenche todo o espaço cortando as bordas — certo pra
     *  cena de tela cheia do Quarto. FIT = mostra o vídeo inteiro dentro do
     *  espaço disponível, sem cortar — certo quando o vídeo já tem sua própria
     *  cor de fundo "assada" que precisa bater com o fundo ao redor (ex.: o
     *  avatar da Home, que não é tela cheia). */
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
) {
    val context = LocalContext.current
    val exoPlayer = remember(rawResId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResId")))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
            }
        }
    )
}

/** `true`/id se existir `res/raw/<name>.mp4` (ou outro formato de vídeo) com
 *  esse nome — permite cair de volta pra pose estática quando a animação
 *  daquela cena ainda não foi gerada, sem quebrar nada. */
@Composable
fun rememberRawVideoResId(name: String): Int? {
    val context = LocalContext.current
    return remember(name) {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id != 0) id else null
    }
}
