package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.realpredict.ai.viewmodel.HomeViewModel
import com.realpredict.ai.viewmodel.HomeUiState

@Composable
fun HomeViewModelFactory(): HomeViewModel = viewModel {
    AppContainer.homeViewModel()
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToStats: () -> Unit = {},
    onNavigateToLab: () -> Unit = {},
    viewModel: HomeViewModel = HomeViewModelFactory()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RealPredict AI",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Quiniela Real 1:00 p. m. · República Dominicana",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = if (state.isLoading) "Cargando..." else "${state.totalDraws} sorteos",
                    color = EmeraldBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Error
        state.errorMessage?.let { msg ->
            Surface(
                color = Color(0xFF3B1B1B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(msg, color = Color(0xFFFFB4AB), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }

        // Card PREDICCIÓN con último sorteo REAL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF133E2B),
                            Color(0xFF0E291D),
                            Color(0xFF0A1A13)
                        )
                    )
                )
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ÚLTIMO SORTEO REAL",
                            color = Color(0xFF6EE7B7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "DESDE LA BD LOCAL · VERIFICADO",
                            color = EmeraldPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "1:00 PM",
                            color = EmeraldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val draw = state.lastDraw
                if (draw == null && state.isLoading) {
                    CircularProgressIndicator(
                        color = EmeraldBright,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (draw != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LotteryBall(number = draw.firstNumber, size = 72.dp, variant = BallVariant.EMERALD, label = "1ra")
                        LotteryBall(number = draw.secondNumber, size = 72.dp, variant = BallVariant.EMERALD, label = "2da")
                        LotteryBall(number = draw.thirdNumber, size = 72.dp, variant = BallVariant.EMERALD, label = "3ra")
                    }
                } else {
                    Text(
                        text = "Sin sorteos importados todavía.\nVe a Import para cargar un CSV/XLSX.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = EmeraldPrimary.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Fecha", color = TextSecondary, fontSize = 10.sp)
                        Text(
                            text = draw?.drawDate ?: "—",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onNavigateToStats,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver estadísticas", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Acceso directo Laboratorio
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Laboratorio de Backtesting", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Simulación cronológica sin fuga temporal", color = TextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = onNavigateToLab,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Abrir", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
