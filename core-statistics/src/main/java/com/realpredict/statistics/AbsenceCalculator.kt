package com.realpredict.statistics

import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw

/**
 * Calcula el tiempo desde la última aparición de cada número.
 * Determinista y reproducible.
 */
data class AbsenceReport(
    val referenceDate: String,            // fecha hasta donde se cuenta
    val perNumber: Map<String, Int>,       // 00..99 → días desde última aparición (null si nunca)
    val neverAppeared: List<String>,
    val algorithmVersion: String = "1.0.0"
)

object AbsenceCalculator {

    const val ALGORITHM_VERSION = "1.0.0"

    fun compute(draws: List<Draw>, referenceDate: String): AbsenceReport {
        require(draws.isNotEmpty()) { "Sorteos vacíos en AbsenceCalculator" }

        // Última fecha de aparición por número
        val lastSeen: MutableMap<String, String> = mutableMapOf()
        for (draw in draws.sortedBy { it.drawDate }) {
            for (num in draw.allNumbers()) {
                require(Constants.isValidNumber(num)) { "Número inválido: $num" }
                // Nos quedamos con la mayor fecha vista (draws ya ordenados ascendentemente)
                lastSeen[num] = draw.drawDate
            }
        }

        val refDay = parseIso(referenceDate)
        val perNumber: Map<String, Int> = Constants.VALID_NUMBERS.associateWith { num ->
            val last = lastSeen[num]
            if (last == null) -1
            else daysBetween(parseIso(last), refDay)
        }

        val never = Constants.VALID_NUMBERS.filter { num ->
            perNumber[num] == -1 || perNumber[num] == null
        }

        return AbsenceReport(
            referenceDate = referenceDate,
            perNumber = perNumber,
            neverAppeared = never,
            algorithmVersion = ALGORITHM_VERSION
        )
    }

    // ---------- utilidades de fecha (sin depender de java.time para tests JVM) ----------

    private fun parseIso(dateStr: String): Triple<Int, Int, Int> {
        val parts = dateStr.split("-")
        require(parts.size == 3) { "Fecha inválida: $dateStr (esperado yyyy-MM-dd)" }
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()
        require(m in 1..12) { "Mes inválido: $m" }
        require(d in 1..31) { "Día inválido: $d" }
        return Triple(y, m, d)
    }

    /**
     * Días entre dos fechas ISO. Implementación simple basada en día juliano modificado.
     * No maneja calendario gregoriano prolepsis completo, pero suficiente para rango 2020-2030.
     */
    private fun daysBetween(start: Triple<Int, Int, Int>, end: Triple<Int, Int, Int>): Int {
        return julianDay(end.first, end.second, end.third) -
               julianDay(start.first, start.second, start.third)
    }

    private fun julianDay(y: Int, m: Int, d: Int): Int {
        // Algoritmo de Fliegel-Van Flandern (entero)
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }
}
