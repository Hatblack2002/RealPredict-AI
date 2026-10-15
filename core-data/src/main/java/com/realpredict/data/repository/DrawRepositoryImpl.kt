package com.realpredict.data.repository

import android.content.Context
import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.data.local.EngineDatabase
import com.realpredict.data.local.dao.DrawDao
import com.realpredict.data.local.entity.DrawEntity
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.ImportBatch
import com.realpredict.domain.model.ImportStatus
import com.realpredict.domain.model.VerificationStatus
import com.realpredict.domain.repository.DrawRepository
import com.realpredict.domain.repository.DatasetSnapshot
import com.realpredict.domain.repository.InsertOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Implementación de [DrawRepository] sobre Room (Sección 4 — Data).
 * Convierte entre [DrawEntity] (Room) y [Draw] (domain) sin acoplar el dominio a Android.
 */
class DrawRepositoryImpl(
    private val drawDao: DrawDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : DrawRepository {

    override fun observeAll(): Flow<List<Draw>> =
        drawDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun count(): Int = drawDao.countDraws()

    override suspend fun getByDate(date: String, drawType: String): Draw? =
        drawDao.getDrawByDate(date, drawType)?.toDomain()

    override suspend fun getRange(startDate: String, endDate: String): List<Draw> =
        drawDao.getRange(startDate, endDate).map { it.toDomain() }

    override suspend fun getAllBefore(dateExclusive: String, drawType: String): List<Draw> =
        drawDao.getAllBefore(dateExclusive, drawType).map { it.toDomain() }

    override suspend fun insertBatch(batch: ImportBatch, draws: List<Draw>): InsertOutcome {
        val now = timeProvider.nowIso()
        var accepted = 0
        var rejected = 0
        var duplicates = 0
        val warnings = mutableListOf<String>()

        for (draw in draws) {
            try {
                val existing = drawDao.getDrawByDate(draw.drawDate, draw.drawType)
                if (existing != null) {
                    duplicates++
                    continue
                }
                drawDao.insertDraws(listOf(draw.toEntity(createdAt = now)))
                accepted++
            } catch (e: Exception) {
                rejected++
                warnings += "Fila con fecha ${draw.drawDate}: ${e.message}"
            }
        }

        return InsertOutcome(
            batchId = batch.id,
            accepted = accepted,
            rejected = rejected,
            duplicates = duplicates,
            warnings = warnings
        )
    }

    override suspend fun replaceDrawsWithConfirmation(draws: List<Draw>): Int {
        drawDao.clearAll()
        val now = timeProvider.nowIso()
        val entities = draws.map { it.toEntity(createdAt = now) }
        drawDao.insertDraws(entities)
        return entities.size
    }

    override suspend fun updateVerificationStatus(drawId: String, status: VerificationStatus) {
        drawDao.updateVerificationStatus(drawId, status.name)
    }

    override suspend fun clearAll() = drawDao.clearAll()

    override suspend fun snapshot(drawType: String): DatasetSnapshot {
        val all = drawDao.getAllDraws().filter { it.drawType == drawType }.sortedBy { it.drawDate }
        if (all.isEmpty()) {
            return DatasetSnapshot(
                snapshotId = idGenerator.newId("snap_empty"),
                totalDraws = 0,
                startDate = "",
                endDate = "",
                contentHash = sha256("empty")
            )
        }
        val hash = all.joinToString("|") {
            "${it.drawDate}:${it.firstNumber}${it.secondNumber}${it.thirdNumber}"
        }.let { sha256(it) }
        return DatasetSnapshot(
            snapshotId = idGenerator.newId("snap"),
            totalDraws = all.size,
            startDate = all.first().drawDate,
            endDate = all.last().drawDate,
            contentHash = hash
        )
    }

    // ---- Mappers ----
    private fun DrawEntity.toDomain(): Draw = Draw(
        id = id,
        drawDate = drawDate,
        drawType = drawType,
        firstNumber = firstNumber,
        secondNumber = secondNumber,
        thirdNumber = thirdNumber,
        sourceId = sourceId,
        sourceRow = sourceRow,
        verificationStatus = VerificationStatus.valueOf(verificationStatus),
        createdAt = createdAt
    )

    private fun Draw.toEntity(createdAt: String): DrawEntity = DrawEntity(
        id = id,
        drawDate = drawDate,
        drawType = drawType,
        firstNumber = firstNumber,
        secondNumber = secondNumber,
        thirdNumber = thirdNumber,
        sourceId = sourceId,
        sourceRow = sourceRow,
        verificationStatus = verificationStatus.name,
        createdAt = createdAt
    )

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    companion object {
        /**
         * Fábrica para crear una instancia a partir del contexto de Android.
         * Recomendado para ServiceLocator / DI manual.
         */
        fun fromContext(
            context: Context,
            idGenerator: IdGenerator,
            timeProvider: TimeProvider
        ): DrawRepositoryImpl {
            val db = EngineDatabase.get(context)
            return DrawRepositoryImpl(db.drawDao(), idGenerator, timeProvider)
        }
    }
}
