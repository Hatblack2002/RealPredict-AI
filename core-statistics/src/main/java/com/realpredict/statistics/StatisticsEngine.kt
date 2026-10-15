package com.realpredict.statistics

import com.realpredict.domain.model.Draw
import com.realpredict.domain.repository.DatasetSnapshot

/**
 * Fachada del motor estadístico (Sección 6).
 *
 * Recopila todos los cálculos verificables:
 * - Frecuencia por número y por posición.
 * - Tiempo desde última aparición.
 * - Parejas y repeticiones consecutivas.
 * - Detección de atípicos.
 * - Comparación entre períodos.
 *
 * Cada método informar el tamaño de muestra, período, posición y datos utilizados.
 *
 * IMPORTANTE: la frecuencia histórica NO es probabilidad futura (Sección 6).
 * Esta clase no etiqueta números como "más probables".
 */
class StatisticsEngine(
    private val frequencyCalculator: FrequencyCalculator = FrequencyCalculator,
    private val absenceCalculator: AbsenceCalculator = AbsenceCalculator,
    private val pairAnalyzer: PairAnalyzer = PairAnalyzer,
    private val outlierDetector: OutlierDetector = OutlierDetector
) {

    /**
     * Snapshot determinista del dataset para auditoría.
     */
    fun snapshot(draws: List<Draw>): DatasetSnapshot {
        require(draws.isNotEmpty()) { "Dataset vacío" }
        val sorted = draws.sortedBy { it.drawDate }
        val hash = sorted.joinToString("|") {
            "${it.drawDate}:${it.firstNumber}${it.secondNumber}${it.thirdNumber}"
        }.let { sha256(it) }
        return DatasetSnapshot(
            snapshotId = "snap_${hash.take(16)}",
            totalDraws = sorted.size,
            startDate = sorted.first().drawDate,
            endDate = sorted.last().drawDate,
            contentHash = hash
        )
    }

    /**
     * Reporte completo de un dataset.
     */
    fun fullReport(draws: List<Draw>): StatisticsReport {
        require(draws.isNotEmpty()) { "Dataset vacío" }
        val sorted = draws.sortedBy { it.drawDate }
        val snap = snapshot(sorted)
        return StatisticsReport(
            snapshot = snap,
            frequency = frequencyCalculator.compute(sorted),
            absence = absenceCalculator.compute(sorted, referenceDate = sorted.last().drawDate),
            pairs = pairAnalyzer.compute(sorted),
            outliers = outlierDetector.analyze(sorted),
            algorithmVersion = "1.0.0",
            generatedAt = java.time.Instant.now().toString()
        )
    }

    /**
     * Reporte filtrado por rango de fechas (inclusive).
     */
    fun reportInRange(draws: List<Draw>, startDate: String, endDate: String): StatisticsReport {
        val filtered = draws.filter { it.drawDate in startDate..endDate }
        require(filtered.isNotEmpty()) { "Sin sorteos en rango $startDate..$endDate" }
        return fullReport(filtered)
    }

    /**
     * Compara dos períodos en términos de cambio de frecuencias.
     */
    fun comparePeriods(p1: List<Draw>, p2: List<Draw>): PeriodComparison {
        require(p1.isNotEmpty() && p2.isNotEmpty()) { "Períodos vacíos" }
        val diff = outlierDetector.comparePeriods(p1, p2)
        val sumAbs = diff.values.sum()
        val meanAbs = if (diff.isEmpty()) 0.0 else sumAbs.toDouble() / diff.size
        return PeriodComparison(
            period1 = "${p1.minByOrNull { it.drawDate }?.drawDate}..${p1.maxByOrNull { it.drawDate }?.drawDate}",
            period2 = "${p2.minByOrNull { it.drawDate }?.drawDate}..${p2.maxByOrNull { it.drawDate }?.drawDate}",
            size1 = p1.size,
            size2 = p2.size,
            absoluteDifferences = diff,
            sumAbsoluteDifference = sumAbs,
            meanAbsoluteDifference = meanAbs,
            algorithmVersion = "1.0.0"
        )
    }

    private fun sha256(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Reporte consolidado del motor estadístico.
 * Inmutable: cada instancia sella un snapshot determinista del dataset.
 */
data class StatisticsReport(
    val snapshot: DatasetSnapshot,
    val frequency: FrequencyReport,
    val absence: AbsenceReport,
    val pairs: PairAnalysisReport,
    val outliers: OutlierReport,
    val algorithmVersion: String,
    val generatedAt: String
)

data class PeriodComparison(
    val period1: String,
    val period2: String,
    val size1: Int,
    val size2: Int,
    val absoluteDifferences: Map<String, Int>,
    val sumAbsoluteDifference: Int,
    val meanAbsoluteDifference: Double,
    val algorithmVersion: String
)
