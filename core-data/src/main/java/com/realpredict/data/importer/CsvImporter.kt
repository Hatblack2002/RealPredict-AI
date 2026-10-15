package com.realpredict.data.importer

import com.opencsv.CSVReader
import com.realpredict.common.AppError
import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.Result
import com.realpredict.common.TimeProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Importador de archivos CSV con encabezados configurables (Sección 5).
 *
 * Encabezados preferidos: Fecha | Sorteo | Primera | Segunda | Tercera
 * Admite aliases comunes vía [ColumnMapping].
 *
 * NO inventa valores para completar filas incompletas.
 * NO interpreta silenciosamente fechas ambiguas — el usuario debe elegir el formato.
 */
class CsvImporter(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val defaultCharset: Charset = StandardCharsets.UTF_8,
    private val defaultDelimiter: Char = ','
) {

    /**
     * @param input Stream del archivo CSV.
     * @param sourceId Identificador de fuente (ej: nombre del archivo).
     * @param columnMapping Mapeo de columnas (auto-detectado si es null).
     * @param dateFormat Formato de fecha (por defecto yyyy-MM-dd; el usuario puede elegir dd-MM-yyyy, etc.).
     */
    fun import(
        input: InputStream,
        sourceId: String,
        columnMapping: ColumnMapping? = null,
        dateFormat: DateFormat = DateFormat.ISO_YYYY_MM_DD,
        charset: Charset = defaultCharset,
        delimiter: Char = defaultDelimiter
    ): ImportResult {
        val reader = CSVReader(BufferedReader(InputStreamReader(input, charset)), delimiter)
        val rows = reader.readAll()

        if (rows.isEmpty()) {
            return ImportResult(
                totalRows = 0,
                acceptedDraws = emptyList(),
                rejected = emptyList(),
                duplicates = emptyList(),
                warnings = listOf("Archivo CSV vacío"),
                fileHash = sha256(input),
                sourceId = sourceId
            )
        }

        val header = rows.first().map { it.trim().lowercase() }
        val mapping = columnMapping ?: ColumnMapping.autoDetect(header)

        val errors = mutableListOf<RowError>()
        val accepted = mutableListOf<Draw>()
        val duplicateKeys = mutableSetOf<String>()
        val seen = mutableSetOf<String>()  // clave (fecha, tipo)

        for ((idx, row) in rows.drop(1).withIndex()) {
            val rowNumber = idx + 2  // +1 por header, +1 por 1-indexed
            try {
                val fecha = row.getOrNull(mapping.fechaIndex)
                    ?: throw IllegalArgumentException("Falta columna Fecha")
                val primera = row.getOrNull(mapping.primeraIndex)
                    ?: throw IllegalArgumentException("Falta columna Primera")
                val segunda = row.getOrNull(mapping.segundaIndex)
                    ?: throw IllegalArgumentException("Falta columna Segunda")
                val tercera = row.getOrNull(mapping.terceraIndex)
                    ?: throw IllegalArgumentException("Falta columna Tercera")

                val fechaIso = parseDate(fecha.trim(), dateFormat)
                    ?: run {
                        errors += RowError(rowNumber, "Fecha inválida: '$fecha'", row.toList())
                        return@try
                    }

                val p1 = primera.trim().padToTwoDigits()
                val p2 = segunda.trim().padToTwoDigits()
                val p3 = tercera.trim().padToTwoDigits()

                if (!Constants.isValidNumber(p1) || !Constants.isValidNumber(p2) || !Constants.isValidNumber(p3)) {
                    errors += RowError(rowNumber, "Número fuera de rango 00-99: $primera/$segunda/$tercera", row.toList())
                    return@try
                }

                val key = "$fechaIso|${Constants.DRAW_TYPE_QUINIELA_REAL_1PM}"
                if (key in seen) {
                    duplicateKeys += key
                    errors += RowError(rowNumber, "Duplicado detectado (misma fecha y tipo)", row.toList())
                    return@try
                }
                seen += key

                accepted += Draw(
                    id = idGenerator.newId("draw"),
                    drawDate = fechaIso,
                    drawType = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
                    firstNumber = p1,
                    secondNumber = p2,
                    thirdNumber = p3,
                    sourceId = sourceId,
                    sourceRow = rowNumber,
                    verificationStatus = VerificationStatus.UNVERIFIED,
                    createdAt = timeProvider.nowIso()
                )
            } catch (e: Exception) {
                errors += RowError(rowNumber, e.message ?: "Error desconocido", row.toList())
            }
        }

        return ImportResult(
            totalRows = rows.size - 1,
            acceptedDraws = accepted,
            rejected = errors.filter { it.reason.contains("inválid") || it.reason.contains("fuera de rango") || it.reason.contains("Falta") },
            duplicates = errors.filter { it.reason.contains("Duplicado") },
            warnings = errors.filter { it.reason.contains("vacío") || it.reason.contains("Duplicado") }.map { "Fila ${it.rowNumber}: ${it.reason}" },
            fileHash = "",  // recalculado por el caller si guarda el archivo
            sourceId = sourceId
        )
    }

    private fun parseDate(value: String, format: DateFormat): String? {
        return when (format) {
            DateFormat.ISO_YYYY_MM_DD -> {
                if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
                val parts = value.split("-")
                val (y, m, d) = Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                if (m in 1..12 && d in 1..31) value else null
            }
            DateFormat.DD_MM_YYYY_DASH -> parseDmy(value, '-')
            DateFormat.DD_MM_YYYY_SLASH -> parseDmy(value, '/')
            DateFormat.MM_DD_YYYY_SLASH -> {
                if (!value.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return null
                val parts = value.split("/")
                val (m, d, y) = Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                if (m in 1..12 && d in 1..31) "%04d-%02d-%02d".format(y, m, d) else null
            }
        }
    }

    private fun parseDmy(value: String, sep: Char): String? {
        if (!value.matches(Regex("\\d{2}${Regex.escape(sep.toString())}\\d{2}${Regex.escape(sep.toString())}\\d{4}"))) return null
        val parts = value.split(sep)
        val (d, m, y) = Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        if (m in 1..12 && d in 1..31) return "%04d-%02d-%02d".format(y, m, d)
        return null
    }

    private fun String.padToTwoDigits(): String {
        val trimmed = this.trim()
        if (trimmed.isEmpty()) return this
        // Si ya tiene 2 dígitos, dejar igual. Si tiene 1, padLeft. Si 3+ y son puros dígitos, tomar últimos 2.
        return when {
            trimmed.length == 2 && trimmed.all { it.isDigit() } -> trimmed
            trimmed.length == 1 && trimmed.all { it.isDigit() } -> "0$trimmed"
            trimmed.length > 2 && trimmed.all { it.isDigit() } -> trimmed.takeLast(2)
            else -> trimmed  // deja que la validación falle
        }
    }

    private fun sha256(input: InputStream): String {
        // Nota: esto consume el stream. Se debe llamar antes de leer el CSV,
        // o guardar el contenido en un ByteArray para reutilizar.
        return ""  // placeholder: la verificación real la hace el repositorio
    }
}

/**
 * Mapeo de columnas. Auto-detecta con aliases comunes en español.
 */
data class ColumnMapping(
    val fechaIndex: Int,
    val primeraIndex: Int,
    val segundaIndex: Int,
    val terceraIndex: Int
) {
    companion object {
        fun autoDetect(headerLower: List<String>): ColumnMapping {
            fun findIndex(vararg aliases: String): Int =
                headerLower.indexOfFirst { h -> aliases.any { a -> h.contains(a) } }.let { if (it >= 0) it else 0 }

            return ColumnMapping(
                fechaIndex = findIndex("fecha", "date"),
                primeraIndex = findIndex("primera", "1ra", "1ro", "first"),
                segundaIndex = findIndex("segunda", "2da", "2do", "second"),
                terceraIndex = findIndex("tercera", "3ra", "3ro", "third")
            )
        }
    }
}

enum class DateFormat {
    ISO_YYYY_MM_DD,        // 2024-01-15
    DD_MM_YYYY_DASH,       // 15-01-2024
    DD_MM_YYYY_SLASH,      // 15/01/2024
    MM_DD_YYYY_SLASH       // 01/15/2024
}

data class ImportResult(
    val totalRows: Int,
    val acceptedDraws: List<Draw>,
    val rejected: List<RowError>,
    val duplicates: List<RowError>,
    val warnings: List<String>,
    val fileHash: String,
    val sourceId: String
)

data class RowError(
    val rowNumber: Int,
    val reason: String,
    val rawRow: List<String>
)
