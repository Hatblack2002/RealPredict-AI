package com.realpredict.domain.model

import kotlinx.serialization.Serializable

/**
 * Configuración de un proveedor de IA (Sección 4 — ProviderConfiguration).
 * La API Key se almacena CIFRADA (ver `:core-security`).
 */
@Serializable
data class ProviderConfiguration(
    val id: String,
    val providerId: String,           // "openai" | "gemini" | "mistral" | "claude"
    val encryptedApiKey: String,      // blob cifrado, nunca texto plano
    val selectedModelId: String,
    val enabledCapabilities: Set<String>,
    val createdAt: String,
    val lastConnectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN
)

@Serializable
enum class ConnectionStatus {
    OK,
    AUTH_FAILED,
    QUOTA_EXCEEDED,
    NETWORK_ERROR,
    UNKNOWN
}

/**
 * Conversación de chat (Sección 4 — ChatConversation / ChatMessage).
 */
@Serializable
data class ChatConversation(
    val id: String,
    val title: String,
    val providerId: String,
    val modelId: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ChatRole,
    val content: String,
    val createdAt: String,
    val attachments: List<ChatAttachment> = emptyList(),
    val providerId: String? = null,
    val modelId: String? = null,
    val relatedAnalysisId: String? = null,
    val verifiedByStatistics: Boolean = false   // true si la respuesta fue validada contra el motor
)

@Serializable
enum class ChatRole { USER, ASSISTANT, SYSTEM }

@Serializable
data class ChatAttachment(
    val id: String,
    val messageId: String,
    val type: AttachmentType,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,      // ruta interna cifrada
    val checksum: String          // SHA-256
)

@Serializable
enum class AttachmentType { IMAGE, FILE }

/**
 * Experimento (Sección 4 — Experiment).
 */
@Serializable
data class Experiment(
    val id: String,
    val name: String,
    val hypothesis: String,
    val method: String,
    val parameters: String,        // JSON serializado de parámetros
    val datasetSnapshotId: String,
    val timeRange: String,         // "2024-01-01..2024-12-31"
    val baselineDefinition: String,
    val resultSummary: String? = null,
    val experimentStatus: ExperimentStatus,
    val createdAt: String,
    val completedAt: String? = null
)

@Serializable
enum class ExperimentStatus {
    DRAFT, RUNNING, COMPLETED, FAILED, ARCHIVED
}
