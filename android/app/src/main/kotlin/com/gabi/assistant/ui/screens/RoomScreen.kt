package com.gabi.assistant.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.gabi.assistant.ui.components.RoomIntroVideo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gabi.assistant.ui.components.FrameSequenceAnimation
import com.gabi.assistant.ui.components.GabiAssetImage
import com.gabi.assistant.ui.components.JavisBottomBar
import com.gabi.assistant.ui.components.LoopingSceneVideo
import com.gabi.assistant.ui.components.rememberAnimFrameCount
import com.gabi.assistant.ui.components.rememberRawVideoResId
import com.gabi.assistant.ui.theme.*
import com.gabi.assistant.ui.viewmodel.Outfit
import com.gabi.assistant.ui.viewmodel.Outfits
import com.gabi.assistant.ui.viewmodel.RoomViewModel

/**
 * "Quarto da Gabi" — tela de presença/mini-jogo, fora do papel de assistente.
 * V3 (26/08/2026): guarda-roupa e rotina por horário viraram UM sistema só (pedido do
 * usuário — antes o guarda-roupa mostrava "Vestido Vermelho" desbloqueado enquanto o
 * quarto mostrava uma pose de rotina completamente diferente, o que confundia); agora
 * escolher um look no guarda-roupa troca a pose de verdade, e "Rotina do dia" é uma
 * opção explícita pra voltar a variar por horário. Também ganhou respiração (animação
 * nativa do Compose, sem asset novo) e uma caixinha de som que reage quando o Spotify
 * está tocando.
 */
@Composable
fun RoomScreen(navController: NavController, viewModel: RoomViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWardrobe by remember { mutableStateOf(false) }
    var introFinished by rememberSaveable { mutableStateOf(false) }

    Scaffold(bottomBar = { JavisBottomBar(navController) }, containerColor = DarkBg) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GabiAssetImage(
                fileName = "avatar/room.webp",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                chromaKey = false
            )

            // Personagem: animação com transparência real (sequência de PNGs com
            // canal alfa — não vídeo com cor "assada" no fundo, trocado depois que
            // ficou visivelmente preto comparado com as imagens) quando existir pra
            // essa cena; senão a pose estática + respiração de sempre.
            val charFrameCount = rememberAnimFrameCount(uiState.state.animFolder)
            if (charFrameCount != null) {
                FrameSequenceAnimation(
                    folder = uiState.state.animFolder,
                    frameCount = charFrameCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.62f)
                        .aspectRatio(9f / 16f)
                        .padding(bottom = 96.dp)
                )
            } else {
                BreathingCharacter(
                    poseFile = uiState.state.poseFile,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.62f)
                        .aspectRatio(768f / 1024f)
                        .padding(bottom = 96.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.TopStart).padding(20.dp)) {
                Text("Quarto da Gabi", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(uiState.state.label, color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(feedIndicatorText(uiState.lastFedAt), color = TextSecondary.copy(0.7f), fontSize = 12.sp)
            }

            // Guarda-roupa: canto superior direito, não disputa espaço com a personagem.
            RoomIconButton(
                icon = Icons.Default.Checkroom,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                onClick = { showWardrobe = true }
            )

            // Caixinha de som: canto inferior esquerdo, "num móvel do quarto" — só toca
            // notinhas quando o Spotify está de fato tocando algo (MediaSession real,
            // não só "app aberto").
            SpotifySpeaker(
                isPlaying = uiState.isSpotifyPlaying,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 28.dp)
            )

            // Alimentar: canto inferior, abaixo da personagem, fácil de alcançar com o polegar.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Button(
                    onClick = viewModel::feed,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = DarkBg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Alimentar", color = DarkBg, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!introFinished) {
                Box(Modifier.fillMaxSize().background(DarkBg)) {
                    RoomIntroVideo(Modifier.fillMaxSize(), onFinished = { introFinished = true })
                    TextButton(
                        onClick = { introFinished = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                            .background(SurfaceDark.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    ) { Text("Pular abertura", color = TextPrimary) }
                }
            }
        }
    }

    if (showWardrobe) {
        WardrobeDialog(
            unlockedCount = uiState.unlockedCount,
            nextUnlockInDays = uiState.nextUnlockInDays,
            selectedOutfit = uiState.selectedOutfit,
            onSelect = { viewModel.selectOutfit(it) },
            onDismiss = { showWardrobe = false }
        )
    }
}

/** Pulso sutil de escala + leve flutuação vertical, simulando respiração — sem
 *  precisar de nenhum asset novo (GIF/vídeo fica pra uma fase futura maior). */
@Composable
private fun BreathingCharacter(poseFile: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingOffset"
    )
    GabiAssetImage(
        fileName = poseFile,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = offsetY
        }
    )
}

/** Notinhas flutuando saindo de uma caixinha de som — só aparece quando [isPlaying]. */
@Composable
private fun SpotifySpeaker(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.BottomStart) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Speaker,
                null,
                tint = if (isPlaying) SpotifyGreen else TextSecondary.copy(0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (isPlaying) {
            FloatingNote(delayMs = 0, offsetX = (-4).dp)
            FloatingNote(delayMs = 500, offsetX = 14.dp)
            FloatingNote(delayMs = 1000, offsetX = 4.dp)
        }
    }
}

@Composable
private fun BoxScope.FloatingNote(delayMs: Int, offsetX: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "note")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = delayMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "noteProgress"
    )
    Icon(
        Icons.Default.MusicNote,
        null,
        tint = SpotifyGreen.copy(alpha = 1f - progress),
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = offsetX, y = (-progress * 34f).dp)
            .size(14.dp)
    )
}

private fun feedIndicatorText(lastFedAt: Long): String {
    if (lastFedAt == 0L) return "Ainda não foi alimentada"
    val diffMin = (System.currentTimeMillis() - lastFedAt) / 60_000
    return when {
        diffMin < 1 -> "Alimentada agora mesmo"
        diffMin < 60 -> "Alimentada há ${diffMin}min"
        else -> "Alimentada há ${diffMin / 60}h"
    }
}

@Composable
private fun RoomIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceDark.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = CyanAccent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun WardrobeDialog(
    unlockedCount: Int,
    nextUnlockInDays: Int,
    selectedOutfit: Outfit?,
    onSelect: (Outfit?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(20.dp)
        ) {
            Text("Guarda-roupa", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                if (nextUnlockInDays > 0) "Próximo look em $nextUnlockInDays dia${if (nextUnlockInDays > 1) "s" else ""}"
                else "Tudo desbloqueado!",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 460.dp)
            ) {
                item {
                    // "Rotina do dia": volta a variar a pose por horário real do
                    // aparelho, em vez de ficar fixa num look — sempre disponível.
                    RoutineSlot(
                        selected = selectedOutfit == null,
                        onClick = { onSelect(null); onDismiss() }
                    )
                }
                items(Outfits.all) { outfit ->
                    val unlocked = outfit.unlockIndex < unlockedCount
                    OutfitSlot(
                        outfit = outfit,
                        unlocked = unlocked,
                        selected = selectedOutfit?.id == outfit.id,
                        onClick = {
                            if (unlocked) {
                                onSelect(outfit)
                                onDismiss()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Fechar", color = CyanAccent)
            }
        }
    }
}

@Composable
private fun WardrobeSlotFrame(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    thumbnail: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceMid)
            .then(
                if (selected) Modifier.border(2.dp, CyanAccent, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkBg),
            contentAlignment = Alignment.Center,
            content = thumbnail
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (enabled) TextPrimary else TextSecondary.copy(0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoutineSlot(selected: Boolean, onClick: () -> Unit) {
    WardrobeSlotFrame(label = "Rotina do dia", selected = selected, enabled = true, onClick = onClick) {
        Icon(Icons.Default.Home, null, tint = CyanAccent, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun OutfitSlot(outfit: Outfit, unlocked: Boolean, selected: Boolean, onClick: () -> Unit) {
    WardrobeSlotFrame(label = outfit.label, selected = selected, enabled = unlocked, onClick = onClick) {
        when {
            !unlocked -> Icon(Icons.Default.Lock, null, tint = TextSecondary.copy(0.5f), modifier = Modifier.size(28.dp))
            else -> GabiAssetImage(
                fileName = outfit.poseFile,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Text(
                        "Arte\npendente",
                        color = TextSecondary.copy(0.6f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }
    }
}
