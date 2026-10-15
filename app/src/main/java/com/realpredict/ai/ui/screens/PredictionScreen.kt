package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.components.BallVariant
import com.realpredict.ai.ui.components.LotteryBall
import com.realpredict.ai.ui.theme.*

@Composable
fun PredictionScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf("QUICK") }
    var isCalculating by remember { mutableStateOf(false) }

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
                    text = "Pronóstico Auditable",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Quiniela Real 1:00 p. m. · Registro Inmutable",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Inmutable",
                    color = EmeraldBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Selector de Modo (QUICK, DEEP, LABORATORY)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MODO DE ANÁLISIS ESTADÍSTICO",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("QUICK", "DEEP", "LABORATORY").forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else DarkBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.05f)
                            ),
                            onClick = { selectedMode = mode }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) EmeraldBright else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tarjeta Principal de Triple Candidata
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
                    Text(
                        text = "TRIPLE CANDIDATA PRINCIPAL",
                        color = Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Algoritmo v3.2",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LotteryBall(number = "38", size = 76.dp, variant = BallVariant.EMERALD, label = "1ra Salida")
                    LotteryBall(number = "64", size = 76.dp, variant = BallVariant.EMERALD, label = "2da Salida")
                    LotteryBall(number = "22", size = 76.dp, variant = BallVariant.EMERALD, label = "3ra Salida")
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = EmeraldPrimary.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sorteo: Hoy 1:00 PM", color = Color(0xFFA7F3D0), fontSize = 11.sp)
                    Text(text = "Próximo: 1h 45m", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Triples Alternativas
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TRIPLES ALTERNATIVAS PONDERADAS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Alternativa A
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cluster Recency #2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Percentil 80 Posicional", color = TextSecondary, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LotteryBall(number = "11", size = 36.dp, variant = BallVariant.EMERALD)
                        LotteryBall(number = "88", size = 36.dp, variant = BallVariant.EMERALD)
                        LotteryBall(number = "54", size = 36.dp, variant = BallVariant.EMERALD)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Alternativa B
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Análisis Multimodelo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Verificado por Gateway", color = TextSecondary, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LotteryBall(number = "07", size = 36.dp, variant = BallVariant.AMBER)
                        LotteryBall(number = "43", size = 36.dp, variant = BallVariant.AMBER)
                        LotteryBall(number = "91", size = 36.dp, variant = BallVariant.AMBER)
                    }
                }
            }
        }

        // Botón Principal
        Button(
            onClick = { isCalculating = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Generar Pronóstico Auditable ($selectedMode)",
                color = DarkBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
