package com.realpredict.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.realpredict.ai.viewmodel.HistoryViewModel
import com.realpredict.ai.viewmodel.HistoryUiState
import com.realpredict.domain.model.Draw

@Composable
fun HistoryViewModelFactory(): HistoryViewModel = viewModel {
    AppContainer.historyViewModel()
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = HistoryViewModelFactory()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Historial y Auditoría",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Registro inmutable antes de cada sorteo de la 1:00 PM",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = if (state.isLoading) "Cargando..." else "${state.totalDraws} sorteos en BD",
                color = EmeraldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        state.errorMessage?.let { err ->
            Surface(color = Color(0xFF3B1B1B), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(err, color = Color(0xFFFFB4AB), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldBright)
            }
        } else if (state.draws.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sin sorteos importados todavía.\nVe a Import para cargar un CSV o XLSX.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.draws) { draw ->
                    DrawRow(draw)
                }
            }
        }
    }
}

@Composable
private fun DrawRow(draw: Draw) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draw.drawDate,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fila #${draw.sourceRow ?: "—"} · ${draw.sourceId}",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LotteryBall(number = draw.firstNumber, size = 40.dp, variant = BallVariant.EMERALD, label = "1ra")
                LotteryBall(number = draw.secondNumber, size = 40.dp, variant = BallVariant.EMERALD, label = "2da")
                LotteryBall(number = draw.thirdNumber, size = 40.dp, variant = BallVariant.EMERALD, label = "3ra")
            }
        }
    }
}
