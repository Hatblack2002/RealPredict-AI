package com.realpredict.statistics

import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw
import kotlin.math.abs

/**
 * Detección de registros atípicos y posibles errores (Sección 6).
 *
 * Heurística conservadora: marca como sospechosos:
 *  - Triples con números idénticos (ej: 11-11-11)
 *  - Triples donde los tres números salen en orden secuencial (01-02-03)
 *  - Sorteos con formato fuera del rango válido
 *
 * NO borra ni corrige — solo reporta para revisión humana.
 */
data class OutlierReport(
    val flagged: List<OutlierEntry>,
    val totalChecked: Int,
    val algorithmVersion: String = "1.0.0"
)

data class OutlierEntry(
    val drawId: String,
    val drawDate: String,
    val reason: String,
    val severity: OutlierSeverity,
    val numbers: Triple<String, String, String>
)

enum class OutlierSeverity { INFO, WARN, ERROR }

object OutlierDetector {

    const val ALGORITHM_VERSION = "1.0.0"

    fun analyze(draws: List<Draw>): OutlierReport {
        val flagged = mutableListOf<OutlierEntry>()
        for (draw in draws) {
            val (a, b, c) = draw.toTriple()

            // Validación de formato (debería fallar en constructor, doble verificación aquí)
            if (!Constants.isValidNumber(a) || !Constants.isValidNumber(b) || !Constants.isValidNumber(c)) {
                flagged += OutlierEntry(draw.id, draw.drawDate, "Número fuera de rango 00-99", OutlierSeverity.ERROR, draw.toTriple())
                continue
            }

            // Triple idéntico
            if (a == b && b == c) {
                flagged += OutlierEntry(draw.id, draw.drawDate, "Triple con tres números idénticos ($a)", OutlierSeverity.WARN, draw.toTriple())
            }

            // Secuencia numérica (ascendente o descendente)
            if (isSequential(a, b, c)) {
                flagged += OutlierEntry(draw.id, draw.drawDate, "Triple en secuencia numérica", OutlierSeverity.INFO, draw.toTriple())
            }
        }

        return OutlierReport(
            flagged = flagged,
            totalChecked = draws.size,
            algorithmVersion = ALGORITHM_VERSION
        )
    }

    private fun isSequential(a: String, b: String, c: String): Boolean {
        val na = a.toInt(); val nb = b.toInt(); val nc = c.toInt()
        val asc = (nb == na + 1) && (nc == nb + 1)
        val desc = (nb == na - 1) && (nc == nb - 1)
        val same = (na == nb) && (nb == nc)
        return (asc || desc) && !same
    }

    /**
     * Compara dos períodos para detectar cambios bruscos.
     * Devuelve un mapa con la diferencia absoluta de frecuencias por número.
     */
    fun comparePeriods(p1: List<Draw>, p2: List<Draw>): Map<String, Int> {
        require(p1.isNotEmpty() && p2.isNotEmpty()) { "Períodos vacíos" }
        val freq1 = rawFrequencies(p1)
        val freq2 = rawFrequencies(p2)
        return Constants.VALID_NUMBERS.associateWith { n ->
            abs((freq1[n] ?: 0) - (freq2[n] ?: 0))
        }
    }

    private fun rawFrequencies(draws: List<Draw>): Map<String, Int> {
        val m: MutableMap<String, Int> = mutableMapOf<String, Int>().withDefault { 0 }
        for (d in draws) for (n in d.allNumbers()) m[n] = m.getValue(n) + 1
        return m.toMap()
    }
}
