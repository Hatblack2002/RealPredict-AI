package com.realpredict.domain.model

import kotlinx.serialization.Serializable

/**
 * Evaluación de una predicción contra un sorteo observado (Sección 4 — PredictionEvaluation).
 *
 * No evaluar automáticamente con un sorteo cuya identidad/fecha no esté verificada.
 */
@Serializable
data class PredictionEvaluation(
    val id: String,
    val predictionId: String,
    val observedDrawId: String,
    val evaluationRuleVersion: String,
    val exactMatchesByPosition: Int,      // 0..3
    val exactTripleMatch: Boolean,
    val evaluatedAt: String
) {
    init {
        require(exactMatchesByPosition in 0..3) {
            "exactMatchesByPosition debe estar entre 0 y 3, fue $exactMatchesByPosition"
        }
    }
}

/**
 * Resultado de backtesting cronológico (Sección 7).
 * Sin fuga de información futura.
 */
@Serializable
data class BacktestResult(
    val id: String,
    val experimentId: String,
    val startDate: String,
    val endDate: String,
    val totalDrawsEvaluated: Int,
    val exactTripleHits: Int,
    val positionalHits: Map<Int, Int>,    // posición (1,2,3) → número de aciertos
    val baselineExpected: Double,         // referencia aleatoria esperada
    val observedHitRate: Double,
    val baselineHitRate: Double,
    val numberOfStrategiesTested: Int,
    val limitations: List<String>,
    val algorithmVersion: String,
    val generatedAt: String
)
