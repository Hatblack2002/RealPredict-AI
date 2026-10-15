package com.realpredict.ai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.theme.DarkBackground
import com.realpredict.ai.ui.theme.DarkSurface
import com.realpredict.ai.ui.theme.EmeraldPrimary
import com.realpredict.ai.ui.theme.TextSecondary

@Composable
fun RealPredictBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("home", "Inicio", Icons.Default.Home),
            Triple("stats", "Estadísticas", Icons.Default.BarChart),
            Triple("prediction", "Pronóstico", Icons.Default.TrendingUp),
            Triple("lab", "Resultados", Icons.Default.Science),
            Triple("settings", "Perfil", Icons.Default.Person)
        )

        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBackground,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
