package com.javis.assistant.ui.screens


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.DrawScope63378f4 (chore: finish Gabi setup)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.javis.assistant.ui.components.JavisBottomBar
import com.javis.assistant.ui.navigation.Screen
import com.javis.assistant.ui.theme.*
    val points = remember { buildOrbPoints(ORB_POINTS) }
    val edges  = remember(points) { buildOrbEdges(points, ORB_EDGE_THRESHOLD) }

    var timeMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nano -> timeMs = nano / 1_000_000f }
        }
    }

    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawParticleOrb(points, edges, timeMs)
        }
    }
}

// ── Orb de partículas (estilo NEXUS) em laranja claro ─────────────────

private const val ORB_POINTS = 220
private const val ORB_EDGE_THRESHOLD = 0.42f

private val OrbEdgeColor      = Color(0xFFFFB347) // laranja
private val OrbParticleLight  = Color(0xFFFFD9A0) // laranja claro
private val OrbParticleOrange = Color(0xFFFFB347)
private val OrbGlow           = Color(0xFFFF8C42)

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
    timeMs: Float
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
        val alpha = (d * 0.18f).coerceIn(0f, 0.30f)
        drawLine(
            color = OrbEdgeColor.copy(alpha = alpha),
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
        val color = if (idx % 2 == 0) OrbParticleLight else OrbParticleOrange
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = max(radius, 0.5f),
            center = Offset(p.sx, p.sy)
        )
    }

    // Brilho do núcleo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(OrbGlow.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(cx, cy),
            radius = R * 0.55f
        ),
        center = Offset(cx, cy),
        radius = R * 0.55f
    )
}63378f4 (chore: finish Gabi setup)
