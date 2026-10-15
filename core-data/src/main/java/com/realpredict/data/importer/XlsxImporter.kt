package com.realpredict.data.importer

import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.security.MessageDigest

/**
 * Importador de archivos XLSX (Sección 5).
 * Usa Apache POI para leer la primera hoja del workbook.
 * Respeta la misma validación que CsvImporter: ceros iniciales, rango 00-99, fechas.
 */
class XlsxImporter(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {

    fun import(
        input: InputStream,
        sourceId: String,
        dateFormat: DateFormat = DateFormat.ISO_YYYY_MM_DD,
        sheetIndex: Int = 0
    ): ImportResult {
        val workbook: Workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(sheetIndex)
        val headerRow = sheet.getRow(0) ?: return ImportResult(
            totalRows = 0, acceptedDraws = emptyList(), rejected = emptyList(),
            duplicates = emptyList(), warnings = listOf("Sin fila de encabezado"),
            fileHash = "", sourceId = sourceId
        )

        val headerLower = (0 until headerRow.lastCellNum.toInt()).map { idx ->
            (headerRow.getCell(idx)?.toString()?.trim() ?: "").lowercase()
        }
        val mapping = ColumnMapping.autoDetect(headerLower)

        val errors = mutableListOf<RowError>()
        val accepted = mutableListOf<Draw>()
        val seen = mutableSetOf<String>()

        var total = 0
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            total++
            val rowNumber = r + 1

            try {
                val fecha = cellToString(row.getCell(mapping.fechaIndex))
                val primera = cellToString(row.getCell(mapping.primeraIndex))
                val segunda = cellToString(row.getCell(mapping.segundaIndex))
                val tercera = cellToString(row.getCell(mapping.terceraIndex))

                if (fecha.isBlank() || primera.isBlank() || segunda.isBlank() || tercera.isBlank()) {
                    errors += RowError(rowNumber, "Fila con celdas vacías", listOf(fecha, primera, segunda, tercera))
                    continue
                }

                val fechaIso = parseDate(fecha.trim(), dateFormat)
                    ?: run {
                        errors += RowError(rowNumber, "Fecha inválida: '$fecha'", listOf(fecha, primera, segunda, tercera))
                        continue
                    }

                val p1 = primera.trim().padToTwoDigits()
                val p2 = segunda.trim().padToTwoDigits()
                val p3 = tercera.trim().padToTwoDigits()

                if (!Constants.isValidNumber(p1) || !Constants.isValidNumber(p2) || !Constants.isValidNumber(p3)) {
                    errors += RowError(rowNumber, "Número fuera de rango 00-99: $primera/$segunda/$tercera", listOf(fecha, primera, segunda, tercera))
                    continue
                }

                val key = "$fechaIso|${Constants.DRAW_TYPE_QUINIELA_REAL_1PM}"
                if (key in seen) {
                    errors += RowError(rowNumber, "Duplicado detectado", listOf(fecha, primera, segunda, tercera))
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
                errors += RowError(rowNumber, e.message ?: "Error desconocido", emptyList())
            }
        }

        workbook.close()

        return ImportResult(
            totalRows = total,
            acceptedDraws = accepted,
            rejected = errors.filter { it.reason.contains("inválid") || it.reason.contains("fuera de rango") || it.reason.contains("vacías") },
            duplicates = errors.filter { it.reason.contains("Duplicado") },
            warnings = errors.map { "Fila ${it.rowNumber}: ${it.reason}" },
            fileHash = "",  // caller si quiere
            sourceId = sourceId
        )
    }

    private fun cellToString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                // Si es fecha real:
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue
                    java.text.SimpleDateFormat("yyyy-MM-dd").format(date)
                } else {
                    // Números como 5 → "5" → caller hace padToTwoDigits
                    val d = cell.numericCellValue
                    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                }
            }
            CellType.FORMULA -> cell.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> cell.toString()
        }
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
        return when {
            trimmed.length == 2 && trimmed.all { it.isDigit() } -> trimmed
            trimmed.length == 1 && trimmed.all { it.isDigit() } -> "0$trimmed"
            trimmed.length > 2 && trimmed.all { it.isDigit() } -> trimmed.takeLast(2)
            else -> trimmed
        }
    }
}
