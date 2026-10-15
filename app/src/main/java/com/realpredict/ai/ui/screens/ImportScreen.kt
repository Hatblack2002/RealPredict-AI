package com.realpredict.ai.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.realpredict.ai.di.AppContainer
import com.realpredict.ai.ui.theme.*
import com.realpredict.ai.viewmodel.ImportViewModel
import com.realpredict.ai.viewmodel.ImportUiState

@Composable
fun ImportViewModelFactory(): ImportViewModel = viewModel {
    AppContainer.importViewModel()
}

@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = ImportViewModelFactory()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // SAF launcher: permite al usuario seleccionar CSV o XLSX
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo.csv"
            viewModel.importFromUri(uri, fileName) {
                context.contentResolver.openInputStream(uri)
            }
        }
    }

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
                text = "Importación de Archivos Reales",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Admite CSV y Excel (XLSX) mediante Android Storage Access Framework",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar Archivo Oficial",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Encabezados recomendados: Fecha | Sorteo | Primera | Segunda | Tercera\n" +
                           "Admite aliases: 'fecha'/'date', '1ra'/'primera', etc.\n" +
                           "Formato de fecha: YYYY-MM-DD (preferido) o DD-MM-YYYY",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        openFileLauncher.launch(arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "*/*"
                        ))
                    },
                    enabled = !state.isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (state.isImporting) "Importando..." else "Abrir Selector de Documentos Android",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Estado de importación
        if (state.isImporting) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldBright, modifier = Modifier.size(24.dp))
                    Text("Procesando archivo...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        state.successMessage?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF102820),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Importación exitosa", color = EmeraldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(msg, color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearMessages() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        state.errorMessage?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF3B1B1B),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✗ Error de importación", color = Color(0xFFFFB4AB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(err, color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearMessages() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Detalle del último resultado
        state.lastResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Detalle del último lote", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    DetailRow("Filas totales", result.totalRows.toString())
                    DetailRow("Aceptados", result.acceptedDraws.size.toString())
                    DetailRow("Rechazados", result.rejected.size.toString())
                    DetailRow("Duplicados", result.duplicates.size.toString())
                    if (result.rejected.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Errores detallados:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        result.rejected.take(5).forEach { err ->
                            Text("  • Fila ${err.rowNumber}: ${err.reason}", color = TextSecondary, fontSize = 10.sp)
                        }
                        if (result.rejected.size > 5) {
                            Text("  • y ${result.rejected.size - 5} más...", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
