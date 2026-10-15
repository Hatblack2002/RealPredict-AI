package com.realpredict.ai.gateway

import com.realpredict.common.AppError
import com.realpredict.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz común para todos los proveedores de IA (Sección 9).
 * Permite intercambiar OpenAI / Gemini / Mistral / Claude sin acoplar el resto del sistema.
 *
 * NUNCA compartir las claves ni los endpoints de un proveedor en otro adaptador.
 * El sistema NO debe cambiar de proveedor silenciosamente.
 */
interface AiProvider {

    /** Identificador único y estable del proveedor. */
    val providerId: String

    /** Capacidades soportadas (texto, imágenes, razonamiento extendido). */
    fun getCapabilities(): Set<AiCapability>

    /** Lista de modelos disponibles (si la API lo permite). */
    suspend fun listModels(): Result<List<ModelDescriptor>>

    /** Verifica credenciales sin realizar una llamada completa. */
    suspend fun validateCredentials(): Result<ConnectionProbe>

    /**
     * Envía una solicitud de texto. Devuelve respuesta estructurada si el modelo la genera.
     */
    suspend fun sendTextRequest(
        prompt: String,
        systemPrompt: String,
        modelId: String,
        temperature: Double = 0.2,
        maxTokens: Int? = null,
        responseSchema: ResponseSchema? = null
    ): Result<AiTextResponse>

    /**
     * Envía una solicitud multimodal (texto + imagen). Solo a modelos con visión.
     */
    suspend fun sendMultimodalRequest(
        prompt: String,
        systemPrompt: String,
        imageBase64: String,
        imageMimeType: String,
        modelId: String,
        temperature: Double = 0.2,
        maxTokens: Int? = null
    ): Result<AiTextResponse>

    /**
     * Cancela una solicitud en curso (cuando el proveedor lo permite).
     */
    fun cancelRequest(requestId: String)

    /**
     * Normaliza los errores del proveedor a [AppError] común.
     */
    fun normalizeError(httpCode: Int?, rawBody: String?): AppError
}

enum class AiCapability { TEXT, VISION, EXTENDED_REASONING, STRUCTURED_OUTPUT, TOOL_CALLING }

/**
 * Descriptor de modelo. NO asumir que todos los modelos ofrecen visión o razonamiento.
 */
data class ModelDescriptor(
    val id: String,                          // Identificador oficial
    val displayName: String,                 // Nombre visible
    val available: Boolean,
    val supportsText: Boolean,
    val supportsVision: Boolean,
    val supportsExtendedReasoning: Boolean,
    val maxInputTokens: Long?,
    val maxOutputTokens: Long?
)

data class ConnectionProbe(
    val ok: Boolean,
    val httpCode: Int?,
    val latencyMs: Long,
    val message: String
)

data class AiTextResponse(
    val content: String,
    val modelId: String,
    val providerId: String,
    val usage: TokenUsage?,
    val rawJson: String? = null
)

data class TokenUsage(
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long
)

/**
 * Esquema de respuesta estructurada (Sección 12).
 * Exige al modelo responder con campos definidos: analysisScope, datasetSummary, etc.
 */
data class ResponseSchema(
    val name: String,
    val fields: List<SchemaField>
)

data class SchemaField(
    val name: String,
    val type: SchemaFieldType,
    val description: String,
    val required: Boolean,
    val enum: List<String>? = null
)

enum class SchemaFieldType { STRING, NUMBER, BOOLEAN, ARRAY_STRING }
