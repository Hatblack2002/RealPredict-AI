package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.components.BallVariant
import com.realpredict.ai.ui.components.LotteryBall
import com.realpredict.ai.ui.theme.*

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToStats: () -> Unit = {},
    onNavigateToLab: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado de la Quiniela Real
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
        }

        // Card 1: PREDICCIÓN PRINCIPAL (Idéntica al Mockup Image 2)
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
                            text = "QUINIELA REAL: PREDICCIÓN PRINCIPAL",
                            color = Color(0xFF6EE7B7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "AUDITADA POR MOTOR ESTADÍSTICO",
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

                // Las tres esferas de lotería en 3D
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LotteryBall(number = "38", size = 72.dp, variant = BallVariant.EMERALD, label = "1ra")
                    LotteryBall(number = "64", size = 72.dp, variant = BallVariant.EMERALD, label = "2da")
                    LotteryBall(number = "22", size = 72.dp, variant = BallVariant.EMERALD, label = "3ra")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = EmeraldPrimary.copy(alpha = 0.2f), thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resultados del Sorteo: Hoy 1:00 PM",
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Próximo Sorteo: 1h 45m",
                        color = EmeraldBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card 2: DETECCIÓN DE ANOMALÍAS (Mockup Image 1)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DETECCIÓN DE ANOMALÍAS (Quiniela Real)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Indicadores métricos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Puntaje
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Puntaje AI Anómalo", color = TextSecondary, fontSize = 9.sp)
                            Text("92 Alto", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Alerta
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Nivel de Alerta", color = TextSecondary, fontSize = 9.sp)
                            Text("Crítico", color = AlertRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Detección
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Última Detección", color = TextSecondary, fontSize = 9.sp)
                            Text("#14", color = EmeraldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("p=0.003", color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
