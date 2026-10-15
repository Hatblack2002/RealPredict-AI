package com.realpredict.statistics

import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw

/**
 * Métricas de frecuencia calculadas sobre una colección de sorteos.
 * Determinista: la misma entrada produce siempre el mismo resultado.
 *
 * NO confundir frecuencia histórica con probabilidad futura (Sección 6).
 */
data class FrequencyReport(
    val sampleSize: Int,
    val dateRange: String,                // "yyyy-MM-dd..yyyy-MM-dd"
    val perNumber: Map<String, Int>,      // 100 entradas (00..99) → apariciones totales
    val perPosition: Map<Int, Map<String, Int>>,   // 1/2/3 → número → apariciones
    val totalAppearances: Int,            // = sampleSize × 3
    val mostFrequent: List<Pair<String, Int>>,     // top N
    val leastFrequent: List<Pair<String, Int>>,
    val distribution: FrequencyDistribution,
    val algorithmVersion: String = "1.0.0"
)

data class FrequencyDistribution(
    val buckets: Map<Int, Int>   // bucketCount → cuántos números caen en ese bucket
)

object FrequencyCalculator {

    /** Versión actual del algoritmo (Sección 6 — métodos versionados). */
    const val ALGORITHM_VERSION = "1.0.0"

    fun compute(draws: List<Draw>): FrequencyReport {
        require(draws.isNotEmpty()) { "No se puede calcular frecuencia sobre lista vacía" }

        val perPosition = mapOf(
            1 to mutableMapOf<String, Int>().withDefault { 0 },
            2 to mutableMapOf<String, Int>().withDefault { 0 },
            3 to mutableMapOf<String, Int>().withDefault { 0 }
        )
        val total = mutableMapOf<String, Int>().withDefault { 0 }

        for (draw in draws) {
            val nums = draw.allNumbers()
            nums.forEachIndexed { idx, num ->
                require(Constants.isValidNumber(num)) { "Número inválido en sorteo ${draw.id}: $num" }
                perPosition.getValue(idx + 1)[num] = perPosition.getValue(idx + 1).getValue(num) + 1
                total[num] = total.getValue(num) + 1
            }
        }

        // Llenar los 100 números con 0 si no aparecieron
        val fullPerNumber: Map<String, Int> = Constants.VALID_NUMBERS.associateWith { total[it] ?: 0 }
        val fullPerPosition: Map<Int, Map<String, Int>> = perPosition.mapValues { (_, map) ->
            Constants.VALID_NUMBERS.associateWith { map[it] ?: 0 }
        }

        val totalAppearances = draws.size * Constants.POSITIONS_PER_DRAW
        require(fullPerNumber.values.sum() == totalAppearances) {
            "Suma de apariciones (${fullPerNumber.values.sum()}) ≠ $totalAppearances. Error de integridad."
        }

        val sortedDesc = fullPerNumber.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ).map { it.key to it.value }

        val sortedAsc = fullPerNumber.entries.sortedWith(
            compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ).map { it.key to it.value }

        val buckets = fullPerNumber.values.groupingBy { it }.eachCount().toSortedMap()

        val minDate = draws.minByOrNull { it.drawDate }?.drawDate ?: ""
        val maxDate = draws.maxByOrNull { it.drawDate }?.drawDate ?: ""

        return FrequencyReport(
            sampleSize = draws.size,
            dateRange = "$minDate..$maxDate",
            perNumber = fullPerNumber,
            perPosition = fullPerPosition,
            totalAppearances = totalAppearances,
            mostFrequent = sortedDesc.take(15),
            leastFrequent = sortedAsc.take(15),
            distribution = FrequencyDistribution(buckets),
            algorithmVersion = ALGORITHM_VERSION
        )
    }
}
