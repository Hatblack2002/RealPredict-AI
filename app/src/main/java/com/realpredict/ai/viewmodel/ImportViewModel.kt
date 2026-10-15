package com.realpredict.ai.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realpredict.common.DispatcherProvider
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.data.importer.CsvImporter
import com.realpredict.data.importer.ImportResult
import com.realpredict.data.importer.XlsxImporter
import com.realpredict.domain.model.ImportBatch
import com.realpredict.domain.model.ImportStatus
import com.realpredict.domain.repository.DrawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

/**
 * Estado de la pantalla Import.
 */
data class ImportUiState(
    val isImporting: Boolean = false,
    val lastResult: ImportResult? = null,
    val lastBatchId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel para ImportScreen.
 *
 * Recibe el [Uri] del archivo seleccionado por el usuario vía SAF (Storage Access Framework),
 * detecta el formato (CSV vs XLSX) y delega al importador correspondiente.
 * El resultado se persiste en BD vía [DrawRepository.insertBatch].
 */
class ImportViewModel(
    private val drawRepository: DrawRepository,
    private val csvImporter: CsvImporter,
    private val xlsxImporter: XlsxImporter,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    /**
     * Importa un archivo dado su [Uri] y un [openInputStream] que provee el stream.
     * El caller (Composable) debe tener acceso al ContentResolver para abrir el stream.
     *
     * Detección de formato:
     *  - .csv → CsvImporter
     *  - .xlsx → XlsxImporter
     *  - otro → error
     */
    fun importFromUri(
        uri: Uri,
        fileName: String,
        openInputStream: () -> InputStream?
    ) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = ImportUiState(isImporting = true)
            try {
                val input = openInputStream()
                    ?: throw java.io.IOException("No se pudo abrir el archivo")
                val lower = fileName.lowercase()
                val result = when {
                    lower.endsWith(".csv") -> csvImporter.import(
                        input, sourceId = fileName, dateFormat = com.realpredict.data.importer.DateFormat.ISO_YYYY_MM_DD
                    )
                    lower.endsWith(".xlsx") -> xlsxImporter.import(
                        input, sourceId = fileName, dateFormat = com.realpredict.data.importer.DateFormat.ISO_YYYY_MM_DD
                    )
                    else -> throw IllegalArgumentException(
                        "Formato no soportado: '$fileName'. Use .csv o .xlsx"
                    )
                }

                // Persistir el lote en BD
                val batch = ImportBatch(
                    id = idGenerator.newId("batch"),
                    fileName = fileName,
                    fileHash = "import_${System.currentTimeMillis()}",
                    importedAt = timeProvider.nowIso(),
                    totalRows = result.totalRows,
                    acceptedRows = result.acceptedDraws.size,
                    rejectedRows = result.rejected.size,
                    duplicateRows = result.duplicates.size,
                    warnings = result.warnings,
                    importStatus = if (result.acceptedDraws.isEmpty()) ImportStatus.FAILED
                                   else if (result.rejected.isNotEmpty() || result.duplicates.isNotEmpty())
                                       ImportStatus.COMPLETED_WITH_WARNINGS
                                   else ImportStatus.COMPLETED
                )

                val outcome = drawRepository.insertBatch(batch, result.acceptedDraws)

                _uiState.value = ImportUiState(
                    isImporting = false,
                    lastResult = result.copy(),
                    lastBatchId = batch.id,
                    successMessage = buildString {
                        append("Importación completa:\n")
                        append("• ${outcome.accepted} sorteos aceptados\n")
                        if (outcome.duplicates > 0) append("• ${outcome.duplicates} duplicados omitidos\n")
                        if (outcome.rejected > 0) append("• ${outcome.rejected} filas rechazadas\n")
                        if (outcome.warnings.isNotEmpty()) append("• ${outcome.warnings.size} advertencias")
                    },
                    errorMessage = null
                )
            } catch (t: Throwable) {
                _uiState.value = ImportUiState(
                    isImporting = false,
                    errorMessage = "Error al importar: ${t.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
