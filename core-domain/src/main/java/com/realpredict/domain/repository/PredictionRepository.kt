package com.realpredict.domain.repository

import com.realpredict.domain.model.BacktestResult
import com.realpredict.domain.model.PredictionEvaluation
import com.realpredict.domain.model.PredictionRecord
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de predicciones. Inmutable: no permite sobrescribir.
 * Las correcciones deben crear una nueva versión (previousVersionId).
 */
interface PredictionRepository {
    fun observeAll(): Flow<List<PredictionRecord>>
    suspend fun getById(id: String): PredictionRecord?
    suspend fun seal(prediction: PredictionRecord): PredictionRecord
    suspend fun newVersion(previousId: String, corrected: PredictionRecord): PredictionRecord
    suspend fun countByMethod(method: String): Int
}

interface EvaluationRepository {
    suspend fun evaluate(prediction: PredictionRecord, observed: com.realpredict.domain.model.Draw): PredictionEvaluation
    suspend fun getByPrediction(predictionId: String): PredictionEvaluation?
    fun observeByDateRange(start: String, end: String): Flow<List<PredictionEvaluation>>
}

interface BacktestRepository {
    suspend fun save(result: BacktestResult): BacktestResult
    suspend fun getById(id: String): BacktestResult?
    fun observeByExperiment(experimentId: String): Flow<List<BacktestResult>>
}
