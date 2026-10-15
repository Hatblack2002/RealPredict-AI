package com.realpredict.domain.repository

import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.ImportBatch
import com.realpredict.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de sorteos.
 * Implementado por `:core-data` (Room) sin exponer dependencias de Android al domain.
 */
interface DrawRepository {
    fun observeAll(): Flow<List<Draw>>
    suspend fun count(): Int
    suspend fun getByDate(date: String, drawType: String = "QUINIELA_REAL_1PM"): Draw?
    suspend fun getRange(startDate: String, endDate: String): List<Draw>
    suspend fun getAllBefore(dateExclusive: String, drawType: String = "QUINIELA_REAL_1PM"): List<Draw>
    suspend fun insertBatch(batch: ImportBatch, draws: List<Draw>): InsertOutcome
    suspend fun replaceDrawsWithConfirmation(draws: List<Draw>): Int
    suspend fun updateVerificationStatus(drawId: String, status: VerificationStatus)
    suspend fun clearAll()
    suspend fun snapshot(drawType: String = "QUINIELA_REAL_1PM"): DatasetSnapshot
}

data class InsertOutcome(
    val batchId: String,
    val accepted: Int,
    val rejected: Int,
    val duplicates: Int,
    val warnings: List<String>
)

data class DatasetSnapshot(
    val snapshotId: String,
    val totalDraws: Int,
    val startDate: String,
    val endDate: String,
    val contentHash: String   // hash determinista del contenido para auditoría
)
