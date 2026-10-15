package com.realpredict.data.importer

import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Importador de archivos CSV con encabezados configurables (Sección 5).
 *
 * Parsing manual sin depender de OpenCSV (evita ambigüedades de overload
 * que surgieron con la versión 5.9). Acepta comillas dobles para celdas
 * con espacios o delimitadores embebidos.
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

    fun import(
        input: InputStream,
        sourceId: String,
        columnMapping: ColumnMapping? = null,
        dateFormat: DateFormat = DateFormat.ISO_YYYY_MM_DD,
        charset: Charset = defaultCharset,
        delimiter: Char = defaultDelimiter
    ): ImportResult {
        val reader = BufferedReader(InputStreamReader(input, charset))
        val rawLines: List<String> = reader.use { it.readLines() }

        if (rawLines.isEmpty()) {
            return ImportResult(
                totalRows = 0,
                acceptedDraws = emptyList(),
                rejected = emptyList(),
                duplicates = emptyList(),
                warnings = listOf("Archivo CSV vacío"),
                fileHash = "",
                sourceId = sourceId
            )
        }

        val rows: List<List<String>> = rawLines.map { parseLine(it, delimiter) }
        val header = rows.first().map { it.trim().lowercase() }
        val mapping = columnMapping ?: ColumnMapping.autoDetect(header)

        val errors = mutableListOf<RowError>()
        val accepted = mutableListOf<Draw>()
        val seen = mutableSetOf<String>()

        for ((idx, row) in rows.drop(1).withIndex()) {
            val rowNumber = idx + 2
            try {
                val fecha = row.getOrNullSafe(mapping.fechaIndex)
                if (fecha.isNullOrBlank()) {
                    errors += RowError(rowNumber, "Falta columna Fecha", row)
                    continue
                }
                val primera = row.getOrNullSafe(mapping.primeraIndex)
                if (primera.isNullOrBlank()) {
                    errors += RowError(rowNumber, "Falta columna Primera", row)
                    continue
                }
                val segunda = row.getOrNullSafe(mapping.segundaIndex)
                if (segunda.isNullOrBlank()) {
                    errors += RowError(rowNumber, "Falta columna Segunda", row)
                    continue
                }
                val tercera = row.getOrNullSafe(mapping.terceraIndex)
                if (tercera.isNullOrBlank()) {
                    errors += RowError(rowNumber, "Falta columna Tercera", row)
                    continue
                }

                val fechaIso = parseDate(fecha.trim(), dateFormat)
                if (fechaIso == null) {
                    errors += RowError(rowNumber, "Fecha inválida: '$fecha'", row)
                    continue
                }

                val p1 = primera.trim().padToTwoDigits()
                val p2 = segunda.trim().padToTwoDigits()
                val p3 = tercera.trim().padToTwoDigits()

                if (!Constants.isValidNumber(p1) || !Constants.isValidNumber(p2) || !Constants.isValidNumber(p3)) {
                    errors += RowError(rowNumber, "Número fuera de rango 00-99: $primera/$segunda/$tercera", row)
                    continue
                }

                val key = "$fechaIso|${Constants.DRAW_TYPE_QUINIELA_REAL_1PM}"
                if (key in seen) {
                    errors += RowError(rowNumber, "Duplicado detectado (misma fecha y tipo)", row)
                    continue
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
                errors += RowError(rowNumber, e.message ?: "Error desconocido", row)
            }
        }

        return ImportResult(
            totalRows = rows.size - 1,
            acceptedDraws = accepted,
            rejected = errors.filter { it.reason.contains("inválid") || it.reason.contains("fuera de rango") || it.reason.contains("Falta") },
            duplicates = errors.filter { it.reason.contains("Duplicado") },
            warnings = errors.filter { it.reason.contains("vacío") || it.reason.contains("Duplicado") }.map { "Fila ${it.rowNumber}: ${it.reason}" },
            fileHash = "",
            sourceId = sourceId
        )
    }

    /**
     * Parser CSV simple que respeta comillas dobles embebidas.
     * No implementa el estándar RFC 4180 completo, pero soporta el caso común
     * de celdas sin delimitadores embebidos (típico en lotería).
     */
    private fun parseLine(line: String, delimiter: Char): List<String> {
        if (line.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = !inQuotes
                }
                c == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun List<String>.getOrNullSafe(index: Int): String? =
        if (index in 0 until size) this[index] else null

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
        return when {
            trimmed.length == 2 && trimmed.all { it.isDigit() } -> trimmed
            trimmed.length == 1 && trimmed.all { it.isDigit() } -> "0$trimmed"
            trimmed.length > 2 && trimmed.all { it.isDigit() } -> trimmed.takeLast(2)
            else -> trimmed
        }
    }
}

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
    ISO_YYYY_MM_DD,
    DD_MM_YYYY_DASH,
    DD_MM_YYYY_SLASH,
    MM_DD_YYYY_SLASH
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
