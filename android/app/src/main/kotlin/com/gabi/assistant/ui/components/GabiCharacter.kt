package com.gabi.assistant.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/** Estado visual da Gabi-personagem, espelha o que a Gabi tá fazendo agora. */
enum class GabiCharacterState { IDLE, LISTENING, TALKING }

/**
 * Mostra a Gabi como personagem ilustrada (em vez do Orb abstrato de partículas),
 * trocando com crossfade suave entre 3 poses conforme [state].
 *
 * As poses vêm de `assets/gabi-idle.png`, `assets/gabi-listening.png` e
 * `assets/gabi-talking.png`. O ideal é vir com canal alfa real (é o que a
 * versão atual dos arquivos tem); se algum dia vier num pacote com fundo
 * magenta sólido em vez de alfa (mais fácil de pedir a algumas ferramentas de
 * geração de imagem), o processamento abaixo remove o magenta com suavização
 * de borda por distância de cor — e é inofensivo rodar em cima de um PNG que
 * já é transparente de verdade, então funciona nos dois casos sem precisar
 * saber de antemão qual foi usado.
 *
 * Se algum dos 3 arquivos ainda não existir em assets/, mostra [fallback] no
 * lugar (o Orb de partículas, por exemplo) — não trava nem quebra a tela.
 */
@Composable
fun GabiCharacter(
    state: GabiCharacterState,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var poses by remember { mutableStateOf<Map<GabiCharacterState, ImageBitmap>?>(null) }
    var missingAssets by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { loadAllPoses(context) }
        if (loaded == null) missingAssets = true else poses = loaded
    }

    Box(modifier = modifier) {
        val current = poses
        when {
            current != null -> {
                Crossfade(targetState = state, animationSpec = tween(420), label = "gabi_character") { s ->
                    current[s]?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            missingAssets -> fallback()
            // ainda carregando (primeiro frame) — não mostra nada por uma fração de segundo
        }
    }
}

/**
 * Miniatura circular só com o rosto da Gabi (pose idle), pra usar como ícone
 * — ex. avatar central da barra de baixo. Carrega e processa só 1 arquivo
 * (não as 3 poses do [GabiCharacter]), e recorta pro topo da imagem via
 * ContentScale.Crop + Alignment.TopCenter, já que as poses são corpo inteiro.
 */
@Composable
fun GabiFaceAvatar(modifier: Modifier = Modifier, fallback: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    var face by remember { mutableStateOf<ImageBitmap?>(null) }
    var missing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open(POSE_IDLE_FILE).use { BitmapFactory.decodeStream(it) }
                    ?.let { chromaKeyToTransparent(it) }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap == null) missing = true else face = bitmap
    }

    val current = face
    when {
        current != null -> Image(
            bitmap = current,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = modifier
        )
        missing -> fallback()
    }
}

/**
 * Carrega e mostra UM arquivo de `assets/` pelo nome — usado pelas poses de rotina do
 * Quarto (`gabi-comer.png`, `gabi-dormir.png`...) e pelo fundo do quarto
 * (`room-bg.png`), que não fazem parte do ciclo de 3 poses do [GabiCharacter].
 * [chromaKey] liga o mesmo processamento de fundo transparente das outras poses —
 * desligado pro `room-bg.png`, que é uma cena opaca, não um recorte de personagem.
 */
@Composable
fun GabiAssetImage(
    fileName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    chromaKey: Boolean = true,
    fallback: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
    var missing by remember(fileName) { mutableStateOf(false) }

    LaunchedEffect(fileName) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                context.assets.open(fileName).use { BitmapFactory.decodeStream(it) }
                    ?.let { if (chromaKey) chromaKeyToTransparent(it) else it.asImageBitmap() }
            } catch (e: Exception) {
                null
            }
        }
        if (loaded == null) missing = true else bitmap = loaded
    }

    val current = bitmap
    when {
        current != null -> Image(
            bitmap = current,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
        missing -> fallback()
    }
}

private const val POSE_IDLE_FILE = "gabi-idle.png"
private const val POSE_LISTENING_FILE = "gabi-listening.png"
private const val POSE_TALKING_FILE = "gabi-talking.png"

/** Cor de referência do chroma key — magenta "puro" clássico de tela verde/azul digital. */
private data class ChromaKey(val r: Int, val g: Int, val b: Int)
private val MAGENTA = ChromaKey(255, 0, 255)

private suspend fun loadAllPoses(context: Context): Map<GabiCharacterState, ImageBitmap>? {
    val files = mapOf(
        GabiCharacterState.IDLE to POSE_IDLE_FILE,
        GabiCharacterState.LISTENING to POSE_LISTENING_FILE,
        GabiCharacterState.TALKING to POSE_TALKING_FILE
    )
    val result = mutableMapOf<GabiCharacterState, ImageBitmap>()
    for ((state, fileName) in files) {
        val bitmap = try {
            context.assets.open(fileName).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        } ?: return null // falta algum arquivo — melhor cair pro fallback do que mostrar poses pela metade
        result[state] = chromaKeyToTransparent(bitmap)
    }
    return result
}

/**
 * Remove o fundo magenta sólido com borda suave: pixels bem parecidos com o
 * magenta viram 100% transparentes, pixels bem diferentes ficam intactos, e a
 * faixa intermediária (a transição natural entre personagem e fundo que a
 * própria imagem já tem) recebe alfa gradual via smoothstep — sem isso, a
 * borda fica serrilhada, tipo recorte de tesoura em vez de um recorte limpo.
 * Também suprime o "vazamento" de cor magenta que sobra na borda (despill).
 */
private fun chromaKeyToTransparent(
    source: Bitmap,
    key: ChromaKey = MAGENTA,
    innerThreshold: Float = 40f, // abaixo disso: 100% transparente
    outerThreshold: Float = 110f // acima disso: 100% opaco, cor original
): ImageBitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val range = (outerThreshold - innerThreshold).coerceAtLeast(1f)

    for (i in pixels.indices) {
        val p = pixels[i]
        val a = (p ushr 24) and 0xFF
        var r = (p ushr 16) and 0xFF
        var g = (p ushr 8) and 0xFF
        var b = p and 0xFF

        val dist = colorDistance(r, g, b, key)
        val newAlpha = when {
            dist <= innerThreshold -> 0
            dist >= outerThreshold -> a
            else -> (a * smoothstep((dist - innerThreshold) / range)).toInt().coerceIn(0, 255)
        }

        // Despill: na faixa de borda, puxa R/G de volta pra perto do cinza neutro,
        // proporcional a quão transparente o pixel ficou (mais transparente = mais perto
        // do fundo magenta = mais resíduo de cor pra suprimir).
        if (newAlpha in 1..254) {
            val suppress = 1f - newAlpha / 255f
            val avg = (r + g + b) / 3
            r = (r - (r - avg).coerceAtLeast(0) * suppress).toInt().coerceIn(0, 255)
            b = (b - (b - avg).coerceAtLeast(0) * suppress).toInt().coerceIn(0, 255)
        }

        pixels[i] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out.asImageBitmap()
}

private fun colorDistance(r: Int, g: Int, b: Int, key: ChromaKey): Float {
    val dr = (r - key.r).toFloat()
    val dg = (g - key.g).toFloat()
    val db = (b - key.b).toFloat()
    return sqrt(dr * dr + dg * dg + db * db)
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
