package com.gabi.assistant.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Animação "de verdade" com transparência REAL — em vez de vídeo opaco com uma
 * cor "assada" no fundo (jeito antigo, trocado depois que o usuário reclamou
 * que o vídeo aparecia com fundo preto sólido em vez de sem fundo como as
 * imagens), toca uma sequência de PNGs individuais, cada um já com canal alfa
 * de verdade (gerados via `ffmpeg colorkey` + export de frames, fora do app —
 * ver CLAUDE.md, seção do pipeline de animação). Fica em cima de QUALQUER
 * fundo (cor sólida, gradiente, vídeo do quarto) sem precisar bater a cor.
 *
 * Ficam em `assets/anim/<folder>/f001.png`, `f002.png`... — [rememberAnimFrameCount]
 * conta quantos arquivos existem em tempo de execução (não precisa saber o
 * número de antemão nem recompilar quando o usuário adiciona mais frames).
 */
@Composable
fun FrameSequenceAnimation(
    folder: String,
    frameCount: Int,
    modifier: Modifier = Modifier,
    fps: Int = 8,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var frames by remember(folder) { mutableStateOf<List<ImageBitmap>?>(null) }
    var frameIndex by remember(folder) { mutableIntStateOf(0) }

    LaunchedEffect(folder, frameCount) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                (1..frameCount).map { i ->
                    val name = "anim/$folder/f%03d.png".format(i)
                    context.assets.open(name).use { BitmapFactory.decodeStream(it) }.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (!loaded.isNullOrEmpty()) frames = loaded
    }

    LaunchedEffect(frames) {
        val list = frames ?: return@LaunchedEffect
        val delayMs = (1000L / fps).coerceAtLeast(1)
        while (isActive) {
            delay(delayMs)
            frameIndex = (frameIndex + 1) % list.size
        }
    }

    frames?.getOrNull(frameIndex)?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

/** Quantos frames existem em `assets/anim/<folder>/` agora — `null` se a pasta
 *  não existir ainda (cena sem animação gerada), pra cair no fallback estático. */
@Composable
fun rememberAnimFrameCount(folder: String): Int? {
    val context = LocalContext.current
    return remember(folder) {
        try {
            val files = context.assets.list("anim/$folder")
            if (files.isNullOrEmpty()) null else files.size
        } catch (e: Exception) {
            null
        }
    }
}
