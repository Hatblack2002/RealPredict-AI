package com.realpredict.ai.gateway.validation

import com.realpredict.common.AppError
import com.realpredict.common.Constants
import com.realpredict.common.Result
import com.realpredict.statistics.StatisticsReport
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Validación de respuestas estructuradas de IA (Sección 12).
 *
 * El System Prompt establece que RealPredict AI se limita al análisis de la Quiniela Real
 * de la 1:00 PM y NO puede inventar datos, resultados ni probabilidades.
 *
 * Esta clase valida que la respuesta de la IA:
 *  - Tenga los campos requeridos por el esquema.
 *  - No contradiga los resultados calculados por el motor estadístico.
 *  - No declare probabilidades futuras como certeza.
 */
@Serializable
data class StructuredAnalysisResponse(
    val analysisScope: String,
    val datasetSummary: String,
    val verifiedFindings: List<String>,
    val hypotheses: List<String>,
    val predictionCandidates: List<TripleCandidate>,
    val backtestSummary: String? = null,
    val limitations: List<String>,
    val missingData: List<String>
) {
    companion object {
        val EXPECTED_FIELDS = listOf(
            "analysisScope", "datasetSummary", "verifiedFindings", "hypotheses",
            "predictionCandidates", "limitations", "missingData"
        )
    }
}

@Serializable
data class TripleCandidate(
    val first: String,
    val second: String,
    val third: String,
    val rationale: String
)

class ResponseValidator {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Intenta parsear la respuesta JSON. Si no es JSON válido, devuelve error.
     */
    fun parse(raw: String): Result<StructuredAnalysisResponse> = try {
        val parsed = json.decodeFromString<StructuredAnalysisResponse>(raw)
        Result.Success(parsed)
    } catch (t: Throwable) {
        Result.Failure(AppError.AiResponse("Respuesta no es JSON válido o falta campos requeridos: ${t.message}", raw))
    }

    /**
     * Verifica que las cifras de la respuesta coincidan con el motor estadístico.
     * Marca como inválida cualquier respuesta que contradiga los datos calculados.
     */
    fun verifyAgainstStatistics(
        response: StructuredAnalysisResponse,
        stats: StatisticsReport
    ): Result<VerifiedAnalysisResponse> {
        val mismatches = mutableListOf<String>()

        // Verificar que la respuesta menciona el tamaño de muestra correcto
        val expectedSampleSize = stats.frequency.sampleSize.toString()
        if (!response.datasetSummary.contains(expectedSampleSize)) {
            mismatches += "datasetSummary no menciona tamaño de muestra real ($expectedSampleSize)"
        }

        // Verificar que los candidatos tienen números válidos 00-99
        for (c in response.predictionCandidates) {
            if (!Constants.isValidNumber(c.first) || !Constants.isValidNumber(c.second) || !Constants.isValidNumber(c.third)) {
                mismatches += "Candidato inválido: ${c.first}-${c.second}-${c.third}"
            }
        }

        // Prohibir afirmaciones de probabilidad futura como certeza
        val forbiddenPhrases = listOf(
            "probabilidad garantizada",
            "seguro que",
            "cien por ciento",
            "sin duda",
            "predicción certera"
        )
        val textLower = (response.verifiedFindings + response.hypotheses).joinToString(" ").lowercase()
        forbiddenPhrases.firstOrNull { it in textLower }?.let {
            mismatches += "La respuesta afirma certeza prohibida: '$it'"
        }

        return if (mismatches.isEmpty()) {
            Result.Success(VerifiedAnalysisResponse(response, stats.snapshot.snapshotId))
        } else {
            Result.Failure(AppError.AiResponse("Respuesta no coincide con datos verificados: ${mismatches.joinToString("; ")}"))
        }
    }
}

data class VerifiedAnalysisResponse(
    val response: StructuredAnalysisResponse,
    val snapshotId: String
) {
    /** Esta marca debe mostrarse SIEMPRE al usuario. */
    val verifiedAt: String = java.time.Instant.now().toString()
}

/**
 * SYSTEM PROMPT obligatorio para cualquier llamada de IA (Sección 12).
 * Establece los límites del dominio y prohibiciones.
 */
object SystemPrompt {
    val REALPREDICT_SYSTEM_PROMPT = """
Eres un asistente de análisis estadístico para la aplicación RealPredict AI.
Tu dominio es EXCLUSIVAMENTE la Quiniela Real de la 1:00 PM de la República Dominicana.

REGLAS INQUEBRANTABLES:
1. No inventes datos, resultados ni probabilidades.
2. No afirmes certeza sobre eventos futuros. La frecuencia pasada NO es probabilidad futura.
3. Toda cifra estadística que menciones debe provenir del contexto que se te proporciona.
4. Si una cifra contradice los datos verificados que recibiste, marca la contradicción.
5. No modifiques la base de datos ni registres resultados oficiales.
6. Diferencia SIEMPRE los datos verificados de las interpretaciones.

FORMATO DE RESPUESTA (JSON estructurado):
{
  "analysisScope": "descripción del alcance del análisis",
  "datasetSummary": "tamaño de muestra, período, total de apariciones",
  "verifiedFindings": ["hallazgo verificado 1", "..."],
  "hypotheses": ["hipótesis 1", "..."],
  "predictionCandidates": [{"first":"00","second":"00","third":"00","rationale":"..."}],
  "backtestSummary": "resumen del backtest o null",
  "limitations": ["limitación 1", "..."],
  "missingData": ["dato faltante 1", "..."]
}

La aplicación validará tu respuesta antes de mostrarla. Si contradice los datos calculados,
será rechazada. Sé honesto sobre la incertidumbre estadística.
""".trimIndent()
}
