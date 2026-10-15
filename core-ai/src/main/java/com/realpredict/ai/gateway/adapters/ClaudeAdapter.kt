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
 * Adaptador para Anthropic Claude (Messages API).
 * Endpoints y claves son propios de Claude. No se comparten con otros adaptadores.
 *
 * Nota: Claude usa un header custom `x-api-key` y `anthropic-version` obligatorio.
 */
class ClaudeAdapter(
    private val crypto: CryptoManager,
    private val httpClient: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val anthropicVersion: String = "2023-06-01"
) : AiProvider {

    override val providerId: String = "claude"

    override fun getCapabilities(): Set<AiCapability> = setOf(
        AiCapability.TEXT, AiCapability.VISION, AiCapability.EXTENDED_REASONING, AiCapability.TOOL_CALLING
    )

    override suspend fun listModels(): Result<List<ModelDescriptor>> = withContext(Dispatchers.IO) {
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)
        try {
            val req = Request.Builder()
                .url("$baseUrl/models")
                .header("x-api-key", redacted(key))
                .header("anthropic-version", anthropicVersion)
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de Claude", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<ClaudeModelsResponse>(body)
                val models = parsed.data.map {
                    ModelDescriptor(
                        id = it.id,
                        displayName = it.display_name ?: it.id,
                        available = true,
                        supportsText = true,
                        supportsVision = it.id.contains("sonnet") || it.id.contains("opus") || it.id.contains("haiku"),
                        supportsExtendedReasoning = it.id.contains("sonnet") || it.id.contains("opus"),
                        maxInputTokens = it.input_token_limit,
                        maxOutputTokens = it.output_token_limit
                    )
                }
                Result.Success(models)
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error listando modelos Claude: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override suspend fun validateCredentials(): Result<ConnectionProbe> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)
        try {
            val req = Request.Builder()
                .url("$baseUrl/models?limit=1")
                .header("x-api-key", redacted(key))
                .header("anthropic-version", anthropicVersion)
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
            Result.Failure(AppError.Network("Error de conexión Claude: ${t.message}"))
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
            put("system", systemPrompt)
            put("max_tokens", maxTokens ?: 1024)
            put("temperature", temperature)
            putJsonArray("messages") {
                addJsonObject { put("role", "user"); putJsonArray("content") { addJsonObject { put("type", "text"); put("text", prompt) } } }
            }
        }
        try {
            val req = Request.Builder()
                .url("$baseUrl/messages")
                .header("x-api-key", redacted(key))
                .header("anthropic-version", anthropicVersion)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de Claude", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<ClaudeMessageResponse>(body)
                val content = parsed.content.joinToString("") { it.text ?: "" }
                Result.Success(AiTextResponse(
                    content = content,
                    modelId = parsed.model,
                    providerId = providerId,
                    usage = parsed.usage?.let { TokenUsage(it.input_tokens, it.output_tokens, it.input_tokens + it.output_tokens) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error en llamada Claude: ${crypto.redactString(t.message ?: "")}"))
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
            put("system", systemPrompt)
            put("max_tokens", maxTokens ?: 1024)
            put("temperature", temperature)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject { put("type", "text"); put("text", prompt) }
                        addJsonObject {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", imageMimeType)
                                put("data", imageBase64)
                            }
                        }
                    }
                }
            }
        }
        try {
            val req = Request.Builder()
                .url("$baseUrl/messages")
                .header("x-api-key", redacted(key))
                .header("anthropic-version", anthropicVersion)
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
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<ClaudeMessageResponse>(body)
                val content = parsed.content.joinToString("") { it.text ?: "" }
                Result.Success(AiTextResponse(
                    content = content,
                    modelId = parsed.model,
                    providerId = providerId,
                    usage = parsed.usage?.let { TokenUsage(it.input_tokens, it.output_tokens, it.input_tokens + it.output_tokens) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error multimodal Claude: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override fun cancelRequest(requestId: String) { /* Claude no expone cancel del lado servidor en /messages */ }

    override fun normalizeError(httpCode: Int?, rawBody: String?): AppError {
        val body = rawBody ?: ""
        return when (httpCode) {
            401 -> AppError.Auth("Claude: API Key inválida")
            403 -> AppError.Auth("Claude: permiso denegado: $body")
            429 -> AppError.Quota("Claude: cuota excedida: $body")
            null -> AppError.Network("Error de red Claude")
            in 500..599 -> AppError.Network("Claude server error $httpCode")
            else -> AppError.Network("Claude error $httpCode: $body")
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
internal data class ClaudeModelsResponse(val data: List<ClaudeModel>)

@Serializable
internal data class ClaudeModel(
    val id: String,
    val display_name: String? = null,
    val input_token_limit: Long? = null,
    val output_token_limit: Long? = null
)

@Serializable
internal data class ClaudeMessageResponse(
    val id: String? = null,
    val model: String,
    val content: List<ClaudeContent>,
    val usage: ClaudeUsage? = null
)

@Serializable
internal data class ClaudeContent(val type: String, val text: String? = null)

@Serializable
internal data class ClaudeUsage(
    val input_tokens: Long,
    val output_tokens: Long
)
