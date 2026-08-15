package com.javis.assistant.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.javis.assistant.ui.components.JavisBottomBar
import com.javis.assistant.ui.navigation.Screen
import com.javis.assistant.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(navController: NavController) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    Scaffold(bottomBar = { JavisBottomBar(navController) }, containerColor = DarkBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.Start) {
                Text(greeting, color = TextSecondary, fontSize = 14.sp)
                Text("Gabi", color = OrangeAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            GabiOrb(sizeDp = 220, modifier = Modifier.clickable { navController.navigate(Screen.Chat.route) })
            Text("Toque na Gabi para conversar", color = TextSecondary, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard("Clima", "Como está?", Icons.Default.WbSunny) { navController.navigate(Screen.Chat.route) }
                QuickCard("Memória", "Lembretes", Icons.Default.Psychology) { navController.navigate(Screen.Memory.route) }
                QuickCard("Avisos", "Notificações", Icons.Default.Notifications) { navController.navigate(Screen.Notifications.route) }
            }
        }
    }
}

@Composable
private fun RowScope.QuickCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.weight(1f).height(92.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = OrangeAccent, modifier = Modifier.size(22.dp))
            Column { Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextSecondary, fontSize = 10.sp) }
        }
    }
}

@Composable
fun GabiOrb(modifier: Modifier = Modifier, sizeDp: Int = 220) {
    val t = rememberInfiniteTransition(label = "gabiOrb")
    val pulse by t.animateFloat(0.96f, 1.05f, infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "pulse")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rot")
    val rot2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween(13000, easing = LinearEasing)), label = "rot2")
    Box(modifier = modifier.size(sizeDp.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val cx = w / 2f; val cy = h / 2f; val r = minOf(w, h) / 2f
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xCCFF8C42), Color(0x00FF8C42)), center = Offset(cx, cy), radius = r * 1.15f), center = Offset(cx, cy), radius = r * 1.15f)
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFFFD9A0), Color(0xFFFFB347), Color(0xFFE0670F)), center = Offset(cx - r * 0.25f, cy - r * 0.25f), radius = r * 0.95f), center = Offset(cx, cy), radius = r * 0.82f)
            rotate(rot) { drawArc(brush = Brush.sweepGradient(colors = listOf(Color(0xFFFFD9A0), Color(0x33FFD9A0), Color(0xFFFFD9A0)), center = Offset(cx, cy)), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = r * 0.06f), topLeft = Offset(cx - r * 0.96f, cy - r * 0.96f), size = Size(r * 1.92f, r * 1.92f)) }
            rotate(rot2) { drawArc(brush = Brush.sweepGradient(colors = listOf(Color(0x66FF8C42), Color(0x00FF8C42), Color(0x66FF8C42)), center = Offset(cx, cy)), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = r * 0.03f), topLeft = Offset(cx - r * 1.05f, cy - r * 1.05f), size = Size(r * 2.1f, r * 2.1f)) }
        }
    }
}
