package com.realpredict.data.exporter

import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.PredictionEvaluation
import com.realpredict.domain.model.PredictionRecord
import com.realpredict.domain.model.BacktestResult
import java.io.Writer

/**
 * Exportador CSV de datos históricos y predicciones (Sección 14).
 * Mantiene ceros iniciales y codificación UTF-8 con BOM (para Excel).
 */
class CsvExporter {

    /**
     * Exporta sorteos al formato: Fecha | Sorteo | Primera | Segunda | Tercera
     */
    fun exportDraws(draws: List<Draw>, writer: Writer) {
        writer.write(BOM_UTF8)
        writer.write("Fecha,Sorteo,Primera,Segunda,Tercera\n")
        for (d in draws.sortedBy { it.drawDate }) {
            writer.write("${d.drawDate},${d.drawType},${d.firstNumber},${d.secondNumber},${d.thirdNumber}\n")
        }
        writer.flush()
    }

    fun exportPredictions(predictions: List<PredictionRecord>, writer: Writer) {
        writer.write(BOM_UTF8)
        writer.write("Id,Creada,FechaObjetivo,Metodo,TriplePrincipal,HashIntegridad,Modo,ProveedorId,ModeloId\n")
        for (p in predictions.sortedByDescending { it.createdAt }) {
            val triple = "${p.mainTriple.first}-${p.mainTriple.second}-${p.mainTriple.third}"
            writer.write("${p.id},${p.createdAt},${p.targetDrawDate},${p.selectionMethod},$triple,${p.integrityHash},${p.mode.name},${p.providerId ?: ""},${p.modelId ?: ""}\n")
        }
        writer.flush()
    }

    fun exportEvaluations(evals: List<PredictionEvaluation>, writer: Writer) {
        writer.write(BOM_UTF8)
        writer.write("Id,PrediccionId,SorteoObservadoId,MatchPosicional,TripleExacto,ReglaEvaluacion,Fecha\n")
        for (e in evals) {
            writer.write("${e.id},${e.predictionId},${e.observedDrawId},${e.exactMatchesByPosition},${e.exactTripleMatch},${e.evaluationRuleVersion},${e.evaluatedAt}\n")
        }
        writer.flush()
    }

    fun exportBacktest(result: BacktestResult, writer: Writer) {
        writer.write(BOM_UTF8)
        writer.write("Id,ExperimentoId,PeriodoInicio,PeriodoFin,SorteosEvaluados,AciertosTripleExacto,AciertosPos1,AciertosPos2,AciertosPos3,HitRateObservado,HitRateBaseline,Limitaciones,GeneradoEn\n")
        val pos1 = result.positionalHits[1] ?: 0
        val pos2 = result.positionalHits[2] ?: 0
        val pos3 = result.positionalHits[3] ?: 0
        writer.write("${result.id},${result.experimentId},${result.startDate},${result.endDate},${result.totalDrawsEvaluated},${result.exactTripleHits},$pos1,$pos2,$pos3,${result.observedHitRate},${result.baselineHitRate},${result.limitations.joinToString(";")},${result.generatedAt}\n")
        writer.flush()
    }

    companion object {
        const val BOM_UTF8 = "\uFEFF"
    }
}
