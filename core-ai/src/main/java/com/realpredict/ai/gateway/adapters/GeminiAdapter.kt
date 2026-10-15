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
 * Adaptador para Google Gemini (Generative Language API).
 * Endpoints y claves son propios de Gemini. No se comparten con otros adaptadores.
 */
class GeminiAdapter(
    private val crypto: CryptoManager,
    private val httpClient: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta"
) : AiProvider {

    override val providerId: String = "gemini"

    override fun getCapabilities(): Set<AiCapability> = setOf(
        AiCapability.TEXT, AiCapability.VISION, AiCapability.STRUCTURED_OUTPUT, AiCapability.EXTENDED_REASONING
    )

    override suspend fun listModels(): Result<List<ModelDescriptor>> = withContext(Dispatchers.IO) {
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)
        try {
            val req = Request.Builder()
                .url("$baseUrl/models?key=${redacted(key)}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de Gemini", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiModelsResponse>(body)
                val models = parsed.models.map {
                    val name = it.name.removePrefix("models/")
                    ModelDescriptor(
                        id = name,
                        displayName = it.displayName ?: name,
                        available = it.supportedGenerationMethods?.contains("generateContent") ?: false,
                        supportsText = true,
                        supportsVision = name.contains("vision") || name.startsWith("gemini-1.5") || name.startsWith("gemini-2"),
                        supportsExtendedReasoning = name.contains("thinking") || name.contains("pro"),
                        maxInputTokens = it.inputTokenLimit,
                        maxOutputTokens = it.outputTokenLimit
                    )
                }
                Result.Success(models)
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error listando modelos Gemini: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override suspend fun validateCredentials(): Result<ConnectionProbe> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = crypto.getApiKey(providerId)
        if (key is Result.Failure) return@withContext Result.Failure(key.error)
        try {
            val req = Request.Builder()
                .url("$baseUrl/models?key=${redacted(key)}&pageSize=1")
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
            Result.Failure(AppError.Network("Error de conexión Gemini: ${t.message}"))
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
            putJsonObject("system_instruction") {
                putJsonArray("parts") {
                    addJsonObject { put("text", systemPrompt) }
                }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                if (maxTokens != null) put("maxOutputTokens", maxTokens)
                put("responseMimeType", "application/json")
            }
        }

        try {
            val req = Request.Builder()
                .url("$baseUrl/models/$modelId:generateContent?key=${redacted(key)}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.Failure(normalizeError(resp.code, resp.body?.string()))
                }
                val body = resp.body?.string() ?: return@withContext Result.Failure(
                    AppError.Network("Respuesta vacía de Gemini", resp.code)
                )
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiGenerateResponse>(body)
                val content = parsed.candidates?.firstOrNull()?.content?.parts?.joinToString("") { it.text ?: "" } ?: ""
                Result.Success(AiTextResponse(
                    content = content,
                    modelId = modelId,
                    providerId = providerId,
                    usage = parsed.usageMetadata?.let { TokenUsage(it.promptTokenCount, it.candidatesTokenCount, it.totalTokenCount) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error en llamada Gemini: ${crypto.redactString(t.message ?: "")}"))
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
            putJsonObject("system_instruction") {
                putJsonArray("parts") { addJsonObject { put("text", systemPrompt) } }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", imageMimeType)
                                put("data", imageBase64)
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                if (maxTokens != null) put("maxOutputTokens", maxTokens)
            }
        }

        try {
            val req = Request.Builder()
                .url("$baseUrl/models/$modelId:generateContent?key=${redacted(key)}")
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
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiGenerateResponse>(body)
                val content = parsed.candidates?.firstOrNull()?.content?.parts?.joinToString("") { it.text ?: "" } ?: ""
                Result.Success(AiTextResponse(
                    content = content,
                    modelId = modelId,
                    providerId = providerId,
                    usage = parsed.usageMetadata?.let { TokenUsage(it.promptTokenCount, it.candidatesTokenCount, it.totalTokenCount) },
                    rawJson = body
                ))
            }
        } catch (t: Throwable) {
            Result.Failure(AppError.Network("Error multimodal Gemini: ${crypto.redactString(t.message ?: "")}"))
        }
    }

    override fun cancelRequest(requestId: String) { /* Gemini no expone cancel del lado servidor en generateContent */ }

    override fun normalizeError(httpCode: Int?, rawBody: String?): AppError {
        val body = rawBody ?: ""
        return when (httpCode) {
            400 -> AppError.Auth("Gemini: API Key inválida o request malformado: $body")
            403 -> AppError.Auth("Gemini: permiso denegado: $body")
            429 -> AppError.Quota("Gemini: cuota excedida: $body")
            null -> AppError.Network("Error de red Gemini")
            in 500..599 -> AppError.Network("Gemini server error $httpCode")
            else -> AppError.Network("Gemini error $httpCode: $body")
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
internal data class GeminiModelsResponse(val models: List<GeminiModel>)

@Serializable
internal data class GeminiModel(
    val name: String,
    val displayName: String? = null,
    val supportedGenerationMethods: List<String>? = null,
    val inputTokenLimit: Long? = null,
    val outputTokenLimit: Long? = null
)

@Serializable
internal data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsage? = null
)

@Serializable
internal data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
internal data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
internal data class GeminiPart(val text: String? = null)

@Serializable
internal data class GeminiUsage(
    val promptTokenCount: Long,
    val candidatesTokenCount: Long,
    val totalTokenCount: Long
)
