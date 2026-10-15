package com.realpredict.data.local.dao

import androidx.room.*
import com.realpredict.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM import_batches WHERE id = :id")
    suspend fun getById(id: String): ImportBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: ImportBatchEntity)

    @Query("DELETE FROM import_batches WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE id = :id")
    suspend fun getById(id: String): PredictionEntity?

    @Query("SELECT * FROM predictions WHERE previousVersionId = :prevId")
    suspend fun getVersionsOf(prevId: String): List<PredictionEntity>

    @Query("SELECT COUNT(*) FROM predictions WHERE selectionMethod = :method")
    suspend fun countByMethod(method: String): Int

    /**
     * SOLO INSERTAR. Las predicciones son inmutables: NO se permite UPDATE.
     * Las correcciones deben crear nueva versión (previousVersionId).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(prediction: PredictionEntity): Long

    // No se prove @Update ni @Delete: inmutabilidad garantizada por diseño.
}

@Dao
interface PredictionEvaluationDao {
    @Query("SELECT * FROM prediction_evaluations WHERE predictionId = :pid")
    suspend fun getByPrediction(pid: String): PredictionEvaluationEntity?

    @Query("SELECT * FROM prediction_evaluations WHERE evaluatedAt BETWEEN :start AND :end")
    fun observeByDateRange(start: String, end: String): Flow<List<PredictionEvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eval: PredictionEvaluationEntity)
}

@Dao
interface ProviderConfigurationDao {
    @Query("SELECT * FROM provider_configurations")
    fun observeAll(): Flow<List<ProviderConfigurationEntity>>

    @Query("SELECT * FROM provider_configurations WHERE providerId = :pid LIMIT 1")
    suspend fun getByProvider(pid: String): ProviderConfigurationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ProviderConfigurationEntity)

    @Query("DELETE FROM provider_configurations WHERE providerId = :pid")
    suspend fun deleteByProvider(pid: String)

    @Query("DELETE FROM provider_configurations")
    suspend fun clearAll()
}

@Dao
interface ChatConversationDao {
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getById(id: String): ChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: ChatConversationEntity)

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM chat_conversations")
    suspend fun clearAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    fun observeByConversation(cid: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(m: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :cid")
    suspend fun deleteByConversation(cid: String)
}

@Dao
interface ExperimentDao {
    @Query("SELECT * FROM experiments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExperimentEntity>>

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getById(id: String): ExperimentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ExperimentEntity)

    @Query("UPDATE experiments SET resultSummary = :summary, experimentStatus = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun complete(id: String, summary: String, status: String, completedAt: String)

    @Query("DELETE FROM experiments WHERE id = :id")
    suspend fun delete(id: String)
}
