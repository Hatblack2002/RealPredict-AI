package com.realpredict.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Tabla de lotes de importación (Sección 4 — ImportBatch).
 */
@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val fileHash: String,
    val importedAt: String,
    val totalRows: Int,
    val acceptedRows: Int,
    val rejectedRows: Int,
    val duplicateRows: Int,
    val warnings: String,        // JSON serializado de List<String>
    val importStatus: String
)

/**
 * Tabla de predicciones inmutables (Sección 4 — PredictionRecord).
 * La unicidad se garantiza por id. El hash de integridad previene alteraciones.
 */
@Entity(
    tableName = "predictions",
    indices = [
        Index(value = ["targetDrawDate", "selectionMethod"]),
        Index(value = ["previousVersionId"])
    ]
)
data class PredictionEntity(
    @PrimaryKey
    val id: String,
    val createdAt: String,
    val targetDrawDate: String,
    val drawType: String,
    val datasetSnapshotId: String,
    val algorithmVersion: String,
    val providerId: String?,
    val modelId: String?,
    val mode: String,           // QUICK, DEEP, LABORATORY
    val mainFirst: String,
    val mainSecond: String,
    val mainThird: String,
    val altAFirst: String?,
    val altASecond: String?,
    val altAThird: String?,
    val altBFirst: String?,
    val altBSecond: String?,
    val altBThird: String?,
    val selectionMethod: String,
    val evidenceSummary: String,
    val limitations: String,
    val validationStatus: String,
    val integrityHash: String,
    val previousVersionId: String?
)

@Entity(tableName = "prediction_evaluations")
data class PredictionEvaluationEntity(
    @PrimaryKey
    val id: String,
    val predictionId: String,
    val observedDrawId: String,
    val evaluationRuleVersion: String,
    val exactMatchesByPosition: Int,
    val exactTripleMatch: Boolean,
    val evaluatedAt: String
)

@Entity(
    tableName = "provider_configurations",
    indices = [Index(value = ["providerId"], unique = true)]
)
data class ProviderConfigurationEntity(
    @PrimaryKey
    val id: String,
    val providerId: String,
    val encryptedApiKey: String,
    val selectedModelId: String,
    val enabledCapabilities: String,   // JSON array
    val createdAt: String,
    val lastConnectionStatus: String
)

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val providerId: String,
    val modelId: String,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: String,
    val attachments: String,   // JSON
    val providerId: String?,
    val modelId: String?,
    val relatedAnalysisId: String?,
    val verifiedByStatistics: Boolean
)

@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val hypothesis: String,
    val method: String,
    val parameters: String,
    val datasetSnapshotId: String,
    val timeRange: String,
    val baselineDefinition: String,
    val resultSummary: String?,
    val experimentStatus: String,
    val createdAt: String,
    val completedAt: String?
)
