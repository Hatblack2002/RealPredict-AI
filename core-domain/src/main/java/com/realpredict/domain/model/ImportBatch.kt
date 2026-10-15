package com.realpredict.domain.model

import kotlinx.serialization.Serializable

/**
 * Lote de importación (Sección 4 — ImportBatch).
 * Registra cada operación de importación con su huella criptográfica.
 */
@Serializable
data class ImportBatch(
    val id: String,
    val fileName: String,
    val fileHash: String,        // SHA-256 del archivo original
    val importedAt: String,      // ISO datetime
    val totalRows: Int,
    val acceptedRows: Int,
    val rejectedRows: Int,
    val duplicateRows: Int,
    val warnings: List<String> = emptyList(),
    val importStatus: ImportStatus
)

@Serializable
enum class ImportStatus {
    PENDING,
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    FAILED,
    PARTIAL
}
