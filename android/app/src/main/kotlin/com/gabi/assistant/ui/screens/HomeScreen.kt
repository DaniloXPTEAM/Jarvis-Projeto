package com.gabi.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gabi.assistant.ui.components.AvatarAnimation
import com.gabi.assistant.ui.components.resolveAvatarState
import com.gabi.assistant.ui.components.GabiCharacter
import com.gabi.assistant.ui.components.GabiCharacterState
import com.gabi.assistant.ui.components.JavisBottomBar
import com.gabi.assistant.ui.navigation.Screen
import com.gabi.assistant.ui.theme.*
import com.gabi.assistant.ui.viewmodel.ChatViewModel
import com.gabi.assistant.ui.viewmodel.HomeViewModel
import com.gabi.assistant.ui.viewmodel.sharedChatViewModel
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Calendar

// Gradiente "Céu de Outono" do mockup aprovado — teal-noite no topo, quase
// preto embaixo. Brush.radialGradient não aceita centro em % sem conhecer o
// tamanho em px, então aproxima com um vertical (mesmo efeito visual num
// celular alto: claro em cima, escuro embaixo).
private val skyGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1C3A4A), Color(0xFF0D2230), Color(0xFF071019))
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = sharedChatViewModel(),
    autoStartListening: MutableState<Boolean>? = null
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val characterState by viewModel.characterState.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    // Disparado pelo widget de atalho da tela inicial (toque abre a Gabi já ouvindo,
    // sem precisar achar o app e tocar no avatar manualmente). Liga o mesmo modo
    // contínuo do toque normal — reseta a flag em seguida pra não religar sozinho de
    // novo numa recomposição futura.
    LaunchedEffect(autoStartListening?.value) {
        if (autoStartListening?.value == true) {
            if (!chatState.continuousMode) chatViewModel.toggleContinuousMode()
            autoStartListening.value = false
        }
    }

    Scaffold(bottomBar = { JavisBottomBar(navController) }, containerColor = DarkBg) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(skyGradient).padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.Start) {
                Text(greeting, color = TextSecondary, fontSize = 14.sp)
                Text("Gabi", color = CyanAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            // Keep the existing voice/processing signals and tap action unchanged.
            AvatarAnimation(
                state = resolveAvatarState(
                    listening = characterState == GabiCharacterState.LISTENING,
                    speaking = characterState == GabiCharacterState.TALKING,
                    processing = chatState.isLoading
                ),
                modifier = Modifier.size(260.dp).pointerInput(Unit) {
                    detectTapGestures(onTap = { chatViewModel.toggleContinuousMode() })
                }
            )
            Text(
                when {
                    chatState.voiceState.isListening -> "Ouvindo…"
                    chatState.isLoading -> "Pensando…"
                    chatState.pendingCommand != null -> "Esperando confirmação…"
                    chatState.continuousMode -> "Escutando… toque pra parar"
                    else -> "Toque na Gabi pra conversar"
                },
                color = TextSecondary,
                fontSize = 13.sp
            )

            // Ações como mandar WhatsApp pedem confirmação — a Gabi já pergunta em voz
            // alta e aceita "sim"/"não" falado, mas sem esses botões aqui (só existiam
            // na tela de Chat) quem usa a Home nunca tinha como confirmar por toque.
            AnimatedVisibility(chatState.pendingCommand != null) {
                chatState.pendingCommand?.let { cmd ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { chatViewModel.confirmPendingCommand(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sim, fazer", color = DarkBg, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { chatViewModel.confirmPendingCommand(false) },
                            border = BorderStroke(1.dp, TextSecondary.copy(0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = TextSecondary)
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val weatherSubtitle = when {
                    dashboard.weather != null -> "${dashboard.weather!!.tempC}°C · ${dashboard.weather!!.description}"
                    dashboard.weatherLoading -> "Carregando…"
                    else -> "Indisponível"
                }
                InfoCard(
                    emoji = dashboard.weather?.emoji ?: "☀️",
                    label = "Clima em ${dashboard.weather?.cityName ?: "Fortaleza"}",
                    value = weatherSubtitle,
                    accent = CyanAccent,
                    fillFraction = null,
                    modifier = Modifier.fillMaxWidth()
                ) { navController.navigate(Screen.Chat.route) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    InfoCard(
                        icon = Icons.Default.Psychology,
                        label = "Lembretes",
                        value = if (dashboard.reminderCount > 0) "${dashboard.reminderCount} hoje" else "Nada hoje",
                        accent = BlueAccent,
                        fillFraction = (dashboard.reminderCount / 5f).coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Screen.Memory.route) }
                    InfoCard(
                        icon = Icons.Default.Notifications,
                        label = "Avisos",
                        value = if (dashboard.notificationCount > 0) "${dashboard.notificationCount} novos" else "Nenhum novo",
                        accent = Color(0xFFB48CFF),
                        fillFraction = (dashboard.notificationCount / 5f).coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate(Screen.Notifications.route) }
                }

                InfoCard(
                    emoji = "🏠",
                    label = "Quarto da Gabi",
                    value = "Ver o que ela está fazendo",
                    accent = Color(0xFFFF8C9E),
                    fillFraction = null,
                    modifier = Modifier.fillMaxWidth()
                ) { navController.navigate(Screen.Room.route) }
            }
        }
    }
}

/**
 * Card "vidro" com chip de ícone/emoji, rótulo + valor, e uma barrinha de
 * destaque embaixo na cor [accent]. Se [fillFraction] for null (caso do
 * clima, que não tem uma proporção real associada), a barra fica só
 * decorativa e cheia; quando tem [fillFraction] (lembretes/avisos), ela
 * reflete a contagem de verdade em vez de ser um número inventado.
 */
@Composable
private fun InfoCard(
    label: String,
    value: String,
    accent: Color,
    fillFraction: Float?,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    emoji: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (emoji != null) Text(emoji, fontSize = 15.sp)
                    else if (icon != null) Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(label.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                    Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(TextSecondary.copy(alpha = 0.15f))) {
                Box(
                    modifier = Modifier.fillMaxWidth(fillFraction ?: 1f).fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp)).background(accent)
                )
            }
        }
    }
}

/**
 * Orb de partículas — laranja calmo parado (IDLE), acelera e clareia pra um
 * dourado quase branco enquanto ESCUTA, e pulsa num coral quente enquanto FALA.
 * As três cores/velocidades interpolam suavemente entre si conforme [state] muda.
 */
@Composable
fun GabiOrb(modifier: Modifier = Modifier, sizeDp: Int = 220, state: GabiCharacterState = GabiCharacterState.IDLE) {
    val points = remember { buildOrbPoints(ORB_POINTS) }
    val edges  = remember(points) { buildOrbEdges(points, ORB_EDGE_THRESHOLD) }

    var timeMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nano -> timeMs = nano / 1_000_000f }
        }
    }

    val speedMultiplier by animateFloatAsState(
        targetValue = when (state) {
            GabiCharacterState.LISTENING -> 2.2f
            GabiCharacterState.TALKING -> 1.6f
            GabiCharacterState.IDLE -> 1f
        },
        label = "orb_speed"
    )
    val glowIntensity by animateFloatAsState(
        targetValue = if (state == GabiCharacterState.IDLE) 1f else 1.6f,
        label = "orb_glow"
    )
    val glowColor by animateColorAsState(
        targetValue = when (state) {
            GabiCharacterState.LISTENING -> Color(0xFFFFE9B8) // dourado quase branco, alerta
            GabiCharacterState.TALKING -> Color(0xFFFF7A66)   // coral quente, ritmo de fala
            GabiCharacterState.IDLE -> Color(0xFFFF8C42)      // laranja calmo de sempre
        },
        label = "orb_glow_color"
    )
    val particleColor by animateColorAsState(
        targetValue = when (state) {
            GabiCharacterState.LISTENING -> Color(0xFFFFF3D6)
            GabiCharacterState.TALKING -> Color(0xFFFFA98F)
            GabiCharacterState.IDLE -> Color(0xFFFFB347)
        },
        label = "orb_particle_color"
    )

    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawParticleOrb(points, edges, timeMs * speedMultiplier, glowColor, particleColor, glowIntensity)
        }
    }
}

// ── Orb de partículas (estilo NEXUS) em laranja claro ─────────────────

private const val ORB_POINTS = 220
private const val ORB_EDGE_THRESHOLD = 0.42f

private class OrbPoint(val x: Float, val y: Float, val z: Float, val phase: Float)
private class OrbProjection(val sx: Float, val sy: Float, val depth: Float, val scale: Float)

private fun buildOrbPoints(count: Int): List<OrbPoint> =
    List(count) { i ->
        val phi = acos(1f - 2f * (i + 0.5f) / count)
        val theta = PI.toFloat() * (1f + sqrt(5f)) * i
        OrbPoint(
            x = sin(phi) * cos(theta),
            y = sin(phi) * sin(theta),
            z = cos(phi),
            phase = (i * 0.7f) % (2f * PI.toFloat())
        )
    }

private fun buildOrbEdges(points: List<OrbPoint>, threshold: Float): List<IntArray> {
    val edges = mutableListOf<IntArray>()
    for (i in points.indices) {
        for (j in i + 1 until points.size) {
            val dx = points[i].x - points[j].x
            val dy = points[i].y - points[j].y
            val dz = points[i].z - points[j].z
            if (sqrt(dx * dx + dy * dy + dz * dz) < threshold) {
                edges.add(intArrayOf(i, j))
            }
        }
    }
    return edges
}

private fun DrawScope.drawParticleOrb(
    points: List<OrbPoint>,
    edges: List<IntArray>,
    timeMs: Float,
    glowColor: Color = Color(0xFFFF8C42),
    particleColor: Color = Color(0xFFFFB347),
    glowIntensity: Float = 1f
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val R = w * 0.38f
    val unit = w / 200f
    val t = timeMs

    val rotY = t * 0.0012f
    val rotX = sin(t * 0.0004f) * 0.3f
    val sinY = sin(rotY); val cosY = cos(rotY)
    val sinX = sin(rotX); val cosX = cos(rotX)
    val fov = 320f

    val projections = points.map { p ->
        var x = p.x * cosY - p.z * sinY
        var z = p.x * sinY + p.z * cosY
        var y = p.y

        val y2 = y * cosX - z * sinX
        val z2 = y * sinX + z * cosX
        y = y2
        z = z2

        val pulse = 1f + 0.03f * sin(t * 0.002f + p.phase)
        x *= pulse
        y *= pulse
        z *= pulse

        val scale = fov / (fov + z * R + R)
        val sx = cx + x * R * scale
        val sy = cy + y * R * scale
        val depth = ((z + 1f) / 2f).coerceIn(0f, 1f)
        OrbProjection(sx, sy, depth, scale)
    }

    // Arestas entre partículas próximas
    for (edge in edges) {
        val pa = projections[edge[0]]
        val pb = projections[edge[1]]
        val d = (pa.depth + pb.depth) * 0.5f
        if (d < 0.2f) continue
        val alpha = (d * 0.18f * glowIntensity).coerceIn(0f, 0.42f)
        drawLine(
            color = glowColor.copy(alpha = alpha),
            start = Offset(pa.sx, pa.sy),
            end = Offset(pb.sx, pb.sy),
            strokeWidth = max(0.6f * unit, 0.5f)
        )
    }

    // Partículas com profundidade
    projections.forEachIndexed { idx, p ->
        if (p.depth < 0.1f) return@forEachIndexed
        val radius = p.scale * (2.2f + p.depth * 1.8f) * unit
        val alpha = (0.4f + p.depth * 0.6f).coerceIn(0f, 1f)
        val color = if (idx % 2 == 0) particleColor.copy(alpha = (particleColor.alpha)) else glowColor
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = max(radius, 0.5f),
            center = Offset(p.sx, p.sy)
        )
    }

    // Brilho do núcleo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = (0.28f * glowIntensity).coerceIn(0f, 0.85f)), Color.Transparent),
            center = Offset(cx, cy),
            radius = R * 0.55f
        ),
        center = Offset(cx, cy),
        radius = R * 0.55f
    )
}
