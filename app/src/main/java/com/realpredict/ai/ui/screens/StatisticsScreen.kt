package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.realpredict.ai.di.AppContainer
import com.realpredict.ai.ui.components.BallVariant
import com.realpredict.ai.ui.components.LotteryBall
import com.realpredict.ai.ui.theme.*
import com.realpredict.ai.viewmodel.StatisticsViewModel
import com.realpredict.ai.viewmodel.StatisticsUiState

@Composable
fun StatisticsViewModelFactory(): StatisticsViewModel = viewModel {
    AppContainer.statisticsViewModel()
}

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = StatisticsViewModelFactory()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSubTab by remember { mutableStateOf("grid") }

    val allNumbers = remember { (0..99).map { String.format("%02d", it) } }
    val hotSet by remember(state.hotNumbers) {
        derivedStateOf { state.hotNumbers.take(10).map { it.first }.toSet() }
    }
    val coldSet by remember(state.coldNumbers) {
        derivedStateOf { state.coldNumbers.take(10).map { it.first }.toSet() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado con datos REALES
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
                text = if (state.isLoading) "..." else "${state.totalDraws} sorteos",
                color = EmeraldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Rango de fechas + mensaje error
        if (state.dateRange.isNotEmpty()) {
            Text(
                text = "Rango: ${state.dateRange}",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        state.errorMessage?.let { err ->
            Surface(color = Color(0xFF3B1B1B), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(err, color = Color(0xFFFFB4AB), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
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
                "hot" to "Hot/Cold"
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
                        modifier = Modifier.padding(vertical = 8.dp).wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldBright)
            }
            return@Column
        }

        // Contenido Grid 00-99 con HOT/COLD basado en datos REALES del motor
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
                            val variant = when {
                                num in hotSet -> BallVariant.EMERALD
                                num in coldSet -> BallVariant.AMBER
                                else -> BallVariant.DARK
                            }
                            LotteryBall(number = num, size = 32.dp, variant = variant)
                        }
                    }
                }
            }
        }

        // Top 10 HOT
        if (selectedSubTab == "hot") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Top 10 — Más frecuentes", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.hotNumbers.take(10)) { (num, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LotteryBall(number = num, size = 36.dp, variant = BallVariant.EMERALD)
                                Text("Apariciones: $count", color = Color.White, fontSize = 12.sp)
                                Text(
                                    "%.2f%%".format((count.toDouble() / (state.totalDraws * 3)) * 100),
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Top 10 — Menos frecuentes", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.coldNumbers.take(10)) { (num, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LotteryBall(number = num, size = 36.dp, variant = BallVariant.AMBER)
                                Text("Apariciones: $count", color = Color.White, fontSize = 12.sp)
                                Text(
                                    if (count > 0) "%.2f%%".format((count.toDouble() / (state.totalDraws * 3)) * 100) else "—",
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Frecuencias por número
        if (selectedSubTab == "freq") {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Frecuencia por número (00-99)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val perNumber = state.perNumber
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(allNumbers) { num ->
                            val count = perNumber[num] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(num, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { (count.toFloat() / (state.totalDraws * 3)) },
                                    color = EmeraldPrimary,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.width(140.dp).height(8.dp)
                                )
                                Text("$count", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
