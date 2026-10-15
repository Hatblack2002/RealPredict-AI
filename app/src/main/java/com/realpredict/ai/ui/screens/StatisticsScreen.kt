package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.components.BallVariant
import com.realpredict.ai.ui.components.LotteryBall
import com.realpredict.ai.ui.theme.*

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf("grid") }
    var selectedNumber by remember { mutableStateOf<String?>(null) }

    val allNumbers = remember {
        (0..99).map { String.format("%02d", it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Explorador Estadístico",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Quiniela Real 1:00 PM · Análisis Posicional y Temporal",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "110 Sorteos",
                color = EmeraldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Sub Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "grid" to "Matriz 00-99",
                "freq" to "Frecuencias",
                "week" to "Semanal"
            ).forEach { (id, label) ->
                val isSelected = selectedSubTab == id
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) EmeraldPrimary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    onClick = { selectedSubTab = id }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) DarkBackground else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Contenido Grid 00-99
        if (selectedSubTab == "grid") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HOT & COLD NUMBER GRID (00-99)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("HOT: Verde", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("COLD: Ámbar", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allNumbers) { num ->
                            val isHot = num in listOf("14", "25", "71", "62", "99")
                            val isCold = num in listOf("23", "44", "52", "76")
                            val variant = if (isHot) BallVariant.EMERALD else if (isCold) BallVariant.AMBER else BallVariant.DARK

                            LotteryBall(
                                number = num,
                                size = 32.dp,
                                variant = variant
                            )
                        }
                    }
                }
            }
        }
    }
}
