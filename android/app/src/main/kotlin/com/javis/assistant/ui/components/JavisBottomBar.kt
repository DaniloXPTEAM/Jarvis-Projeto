package com.javis.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
    Box(modifier = Modifier.fillMaxWidth().background(SurfaceDark.copy(alpha = 0.95f)).navigationBarsPadding()) {63378f4 (chore: finish Gabi setup)
        HorizontalDivider(color = OrangeAccent.copy(alpha = 0.12f), thickness = 0.5.dp)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            bottomNavItems.forEach { item ->
                val selected = current == item.screen.route
                val tint by animateColorAsState(if (selected) OrangeAccent else TextSecondary, label = "nav_tint")
                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { if (!selected) navController.navigate(item.screen.route) { popUpTo(Screen.Home.route) { saveState = true }; launchSingleTop = true; restoreState = true } }.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}
