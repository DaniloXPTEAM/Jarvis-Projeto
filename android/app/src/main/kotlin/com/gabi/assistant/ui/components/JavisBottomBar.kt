package com.gabi.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gabi.assistant.ui.navigation.Screen
import com.gabi.assistant.ui.theme.*

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector?,
    val label: String,
    val isGabiAvatar: Boolean = false
)

// "Gabi" ocupa o lugar central — mesma posição do mockup aprovado — com a
// foto real dela em vez de um ícone genérico de chat.
val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home, "Início"),
    BottomNavItem(Screen.Notifications, Icons.Default.Notifications, "Avisos"),
    BottomNavItem(Screen.Chat, null, "Gabi", isGabiAvatar = true),
    BottomNavItem(Screen.Memory, Icons.Default.Psychology, "Memória"),
    BottomNavItem(Screen.Settings, Icons.Default.Settings, "Ajustes")
)

@Composable
fun JavisBottomBar(navController: NavController) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Box(modifier = Modifier.fillMaxWidth().background(SurfaceDark.copy(alpha = 0.95f)).navigationBarsPadding()) {
        HorizontalDivider(color = CyanAccent.copy(alpha = 0.12f), thickness = 0.5.dp)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            bottomNavItems.forEach { item ->
                val selected = current == item.screen.route
                val tint by animateColorAsState(if (selected) CyanAccent else TextSecondary, label = "nav_tint")
                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { if (!selected) navController.navigate(item.screen.route) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } }.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (item.isGabiAvatar) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(SurfaceMid)
                                .border(1.5.dp, tint, CircleShape)
                        ) {
                            GabiFaceAvatar(
                                modifier = Modifier.fillMaxSize(),
                                fallback = { Icon(Icons.Default.SmartToy, contentDescription = item.label, tint = tint, modifier = Modifier.fillMaxSize().padding(5.dp)) }
                            )
                        }
                    } else {
                        Icon(item.icon!!, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}
