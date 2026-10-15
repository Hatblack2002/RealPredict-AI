package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.theme.*

@Composable
fun LaboratoryScreen(
    modifier: Modifier = Modifier
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
        Column {
            Text(
                text = "Laboratorio de Backtesting",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Simulación cronológica sin fuga temporal (Sección 7)",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // Tarjeta Principal
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BACKTESTING: Estrategia v3.2 (Últimos 6 Meses)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ROI
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Retorno (ROI)", color = TextSecondary, fontSize = 9.sp)
                            Text("+18.4%", color = EmeraldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Aciertos
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aciertos Reales", color = TextSecondary, fontSize = 9.sp)
                            Text("29.1%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Drawdown
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Drawdown Máx", color = TextSecondary, fontSize = 9.sp)
                            Text("7.5%", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Métricas numéricas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sorteos", color = TextSecondary, fontSize = 9.sp)
                        Text("110", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aciertos", color = TextSecondary, fontSize = 9.sp)
                        Text("32", color = EmeraldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pérdidas", color = TextSecondary, fontSize = 9.sp)
                        Text("78", color = AlertRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Neto RD$", color = TextSecondary, fontSize = 9.sp)
                        Text("4,500", color = EmeraldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Nueva Simulación", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
