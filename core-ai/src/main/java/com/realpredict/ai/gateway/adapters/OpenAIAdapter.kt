package com.realpredict.ai.gateway.adapters

import com.realpredict.ai.gateway.*
import com.realpredict.common.AppError
import com.realpredict.common.Result
import com.realpredict.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Adaptador para OpenAI (Chat Completions API).
 * Endpoints y claves son propios de OpenAI. No se comparten con otros adaptadores.
 */
class OpenAIAdapter(
    private val crypto: CryptoManager,
    private val httpClient: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://api.openai.com/v1"
) : AiProvider {

    override val providerId: String = "openai"

    override fun getCapabilities(): Set<AiCapability> = setOf(
        AiCapability.TEXT, AiCapability.VISION, AiCapability.STRUCTURED_OUTPUT, AiCapability.TOOL_CALLING
    )

    override suspend fun listModels(): Result<List<ModelDescriptor>> = withContext(Dispatchers.IO) {
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)

        try {
            val req = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer ${redacted(key)}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de OpenAI", resp.code)
                )
                val json = Json { ignoreUnknownKeys = true }
                val parsed = json.decodeFromString<OpenAiModelsResponse>(body)
                val models = parsed.data.map {
                    ModelDescriptor(
                        id = it.id,
                        displayName = it.id,
                        available = it.owned_by == "openai" || it.id.startsWith("gpt"),
                        supportsText = true,
                        supportsVision = it.id.contains("vision") || it.id.startsWith("gpt-4"),
                        supportsExtendedReasoning = it.id.contains("o1") || it.id.contains("o3"),
                        maxInputTokens = null,
                        maxOutputTokens = null
                    )
                }
                Result.Success(models)
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error listando modelos OpenAI: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override suspend fun validateCredentials(): Result<ConnectionProbe> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)
        try {
            val req = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer ${redacted(key)}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                Result.Success(ConnectionProbe(
                    ok = resp.isSuccessful,
                    httpCode = resp.code,
                    latencyMs = System.currentTimeMillis() - start,
                    message = if (resp.isSuccessful) "OK" else "HTTP ${resp.code}"
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error de conexión OpenAI: ${t.message}"))
        }
    }

    override suspend fun sendTextRequest(
        prompt: String,
        systemPrompt: String,
        modelId: String,
        temperature: Double,
        maxTokens: Int?,
        responseSchema: ResponseSchema?
    ): Result<AiTextResponse> = withContext(Dispatchers.IO) {
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)

        val payload = buildJsonObject {
            put("model", modelId)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("temperature", temperature)
            if (maxTokens != null) put("max_tokens", maxTokens)
            if (responseSchema != null) {
                putJsonObject("response_format") {
                    put("type", "json_object")
                }
            }
        }

        try {
            val req = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer ${redacted(key)}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de OpenAI", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiChatResponse>(body)
                Result.Success(AiTextResponse(
                    content = parsed.choices.firstOrNull()?.message?.content ?: "",
                    modelId = parsed.model,
                    providerId = providerId,
                    usage = parsed.usage?.let { TokenUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error en llamada OpenAI: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override suspend fun sendMultimodalRequest(
        prompt: String,
        systemPrompt: String,
        imageBase64: String,
        imageMimeType: String,
        modelId: String,
        temperature: Double,
        maxTokens: Int?
    ): Result<AiTextResponse> = withContext(Dispatchers.IO) {
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)

        val payload = buildJsonObject {
            put("model", modelId)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:$imageMimeType;base64,$imageBase64")
                            }
                        }
                    }
                }
            }
            put("temperature", temperature)
            if (maxTokens != null) put("max_tokens", maxTokens)
        }

        try {
            val req = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer ${redacted(key)}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiChatResponse>(body)
                Result.Success(AiTextResponse(
                    content = parsed.choices.firstOrNull()?.message?.content ?: "",
                    modelId = parsed.model,
                    providerId = providerId,
                    usage = parsed.usage?.let { TokenUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error multimodal OpenAI: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override fun cancelRequest(requestId: String) {
        // OpenAI no soporta cancelación del lado del servidor en chat completions.
        // La cancelación se gestiona cerrando el Call de OkHttp (capa de cliente).
    }

    override fun normalizeError(httpCode: Int?, rawBody: String?): AppError {
        val body = rawBody ?: ""
        return when (httpCode) {
            401 -> AppError.Auth("API Key OpenAI inválida o expirada")
            429 -> AppError.Quota("Cuota OpenAI excedida: $body")
            null -> AppError.Network("Error de red OpenAI")
            in 500..599 -> AppError.Network("OpenAI server error $httpCode: $body")
            else -> AppError.Network("OpenAI error $httpCode: $body")
        }
    }

    private fun redacted(keyResult: Result<String>): String =
        (keyResult as Result.Success).data

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
internal data class OpenAiModelsResponse(val data: List<OpenAiModel>)

@kotlinx.serialization.Serializable
internal data class OpenAiModel(val id: String, val owned_by: String)

@kotlinx.serialization.Serializable
internal data class OpenAiChatResponse(
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null
)

@kotlinx.serialization.Serializable
internal data class OpenAiChoice(val message: OpenAiMessage, val finish_reason: String? = null)

@kotlinx.serialization.Serializable
internal data class OpenAiMessage(val role: String, val content: String)

@kotlinx.serialization.Serializable
internal data class OpenAiUsage(
    val prompt_tokens: Long,
    val completion_tokens: Long,
    val total_tokens: Long
)
