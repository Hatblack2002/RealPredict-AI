package com.realpredict.statistics

import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw

/**
 * Análisis de parejas (pares de números) y repeticiones entre sorteos consecutivos.
 * Sirve para detectar patrones sin afirmar causalidad.
 */
data class PairAnalysisReport(
    val sampleSize: Int,
    val pairFrequencies: Map<String, Int>,      // clave "ab-cd" → apariciones (pareja en misma triple)
    val consecutiveRepeats: Int,                // cuántos sorteos tienen un número repetido del sorteo anterior
    val algorithmVersion: String = "1.0.0"
)

object PairAnalyzer {

    const val ALGORITHM_VERSION = "1.0.0"

    fun compute(draws: List<Draw>): PairAnalysisReport {
        require(draws.isNotEmpty()) { "Sorteos vacíos en PairAnalyzer" }

        // Generar las 3 parejas (1-2, 1-3, 2-3) por cada sorteo
        val pairFreq: MutableMap<String, Int> = mutableMapOf<String, Int>().withDefault { 0 }
        for (draw in draws.sortedBy { it.drawDate }) {
            val nums = draw.allNumbers()
            for (i in 0 until nums.size) {
                for (j in (i + 1) until nums.size) {
                    val (a, b) = normalizePair(nums[i], nums[j])
                    val key = "$a-$b"
                    pairFreq[key] = pairFreq.getValue(key) + 1
                }
            }
        }

        // Repeticiones consecutivas: número que aparece en sorteo N y N+1
        val sorted = draws.sortedBy { it.drawDate }
        var repeats = 0
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1].allNumbers().toSet()
            val curr = sorted[i].allNumbers().toSet()
            if (prev.intersect(curr).isNotEmpty()) repeats++
        }

        return PairAnalysisReport(
            sampleSize = draws.size,
            pairFrequencies = pairFreq.toMap(),
            consecutiveRepeats = repeats,
            algorithmVersion = ALGORITHM_VERSION
        )
    }

    /** Normaliza la pareja: siempre el menor primero, para evitar duplicados (ab-cd == cd-ab). */
    private fun normalizePair(x: String, y: String): Pair<String, String> {
        require(Constants.isValidNumber(x) && Constants.isValidNumber(y)) {
            "Pareja inválida: $x, $y"
        }
        return if (x <= y) x to y else y to x
    }
}
