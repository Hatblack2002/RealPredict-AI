package com.realpredict.common

/**
 * Constantes compartidas por todo el motor.
 * No incluir secretos ni claves aquí.
 */
object Constants {
    /** Identificador del sorteo principal de esta app. */
    const val DRAW_TYPE_QUINIELA_REAL_1PM = "QUINIELA_REAL_1PM"

    /** Cantidad de posiciones sorteadas por la Quiniela Real. */
    const val POSITIONS_PER_DRAW = 3

    /** Rango válido de números como String de 2 dígitos. */
    val VALID_NUMBERS: List<String> = (0..99).map { it.toString().padStart(2, '0') }

    /** Verifica que un número sea válido (2 dígitos, 00-99). */
    fun isValidNumber(value: String): Boolean {
        if (value.length != 2) return false
        if (!value.all { it.isDigit() }) return false
        return value in VALID_NUMBERS
    }

    /** Formato ISO de fecha local. */
    const val DATE_PATTERN_ISO = "yyyy-MM-dd"
    const val DATETIME_PATTERN_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'"
}

/**
 * Modos de análisis según Sección 8.
 */
enum class AnalysisMode {
    QUICK,
    DEEP,
    LABORATORY;

    companion object {
        fun fromString(value: String): AnalysisMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: QUICK
    }
}
