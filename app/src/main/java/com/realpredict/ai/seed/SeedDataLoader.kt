package com.realpredict.ai.seed

import android.content.Context
import com.realpredict.ai.di.AppContainer
import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.ImportBatch
import com.realpredict.domain.model.ImportStatus
import com.realpredict.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cargador de datos semilla: inserta los 100 sorteos verificados de la Quiniela
 * Real La Una (2026-07-07 a 2026-10-14) al primer arranque de la app.
 *
 * Fuente original: loteriadela1.com (CC BY 4.0) + verificación externa para
 * 2026-08-20 (hoy.com.do) y 2026-08-21 (loteriadominicana.com.do).
 *
 * Idempotente: solo inserta si la BD está vacía.
 */
class SeedDataLoader(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {

    /**
     * Llama desde Application.onCreate() en un scope secundario para no bloquear el arranque.
     */
    fun seedIfEmpty(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
        scope.launch {
            try {
                val repo = AppContainer.drawRepository
                val count = repo.count()
                if (count > 0) {
                    android.util.Log.i(TAG, "SeedDataLoader: BD ya tiene $count sorteos, no se inserta semilla.")
                    return@launch
                }
                val draws = seedDraws()
                val batch = ImportBatch(
                    id = idGenerator.newId("seed_batch"),
                    fileName = "quiniela_real_la_una_2026_seed.csv",
                    fileHash = "seed_${draws.size}_draws",
                    importedAt = timeProvider.nowIso(),
                    totalRows = draws.size,
                    acceptedRows = 0,  // se actualiza tras insertBatch
                    rejectedRows = 0,
                    duplicateRows = 0,
                    warnings = emptyList(),
                    importStatus = ImportStatus.PENDING
                )
                val outcome = repo.insertBatch(batch, draws)
                android.util.Log.i(TAG, "SeedDataLoader: insertados ${outcome.accepted} sorteos, ${outcome.duplicates} duplicados, ${outcome.rejected} rechazados.")
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "SeedDataLoader falló", t)
            }
        }
    }

    companion object {
        private const val TAG = "SeedDataLoader"

        /**
         * Los 100 sorteos verificados de la Quiniela Real La Una del 2026-07-07 al 2026-10-14.
         * Cada entrada es (fecha, primera, segunda, tercera).
         *
         * NO usar estos datos como predicción futura — son histórico pasado.
         * La frecuencia pasada NO es probabilidad futura (Sección 6).
         */
        fun seedDraws(): List<Draw> = SEED_DRAWS.mapIndexed { idx, (fecha, a, b, c) ->
            Draw(
                id = "seed_${fecha}",
                drawDate = fecha,
                drawType = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
                firstNumber = a,
                secondNumber = b,
                thirdNumber = c,
                sourceId = "loteriadela1.com (CC BY 4.0)",
                sourceRow = idx + 1,
                verificationStatus = VerificationStatus.VERIFIED,
                createdAt = "2026-10-15T00:00:00Z"
            )
        }

        // 100 registros verificados (formato: fecha, primera, segunda, tercera)
        // Generados a partir del dataset Quiniela_Real_La_Una_2020-2026.xlsx (sección previa)
        private val SEED_DRAWS: List<Triple<String, String, String, String>> = listOf(
            Triple("2026-07-07", "14", "74", "88"),
            Triple("2026-07-08", "35", "73", "67"),
            Triple("2026-07-09", "09", "02", "10"),
            Triple("2026-07-10", "20", "34", "38"),
            Triple("2026-07-11", "75", "28", "27"),
            Triple("2026-07-12", "95", "98", "01"),
            Triple("2026-07-13", "77", "60", "62"),
            Triple("2026-07-14", "34", "76", "70"),
            Triple("2026-07-15", "12", "29", "21"),
            Triple("2026-07-16", "70", "00", "75"),
            Triple("2026-07-17", "18", "19", "06"),
            Triple("2026-07-18", "39", "67", "55"),
            Triple("2026-07-19", "06", "64", "23"),
            Triple("2026-07-20", "21", "76", "27"),
            Triple("2026-07-21", "78", "88", "42"),
            Triple("2026-07-22", "41", "97", "20"),
            Triple("2026-07-23", "76", "04", "59"),
            Triple("2026-07-24", "27", "10", "08"),
            Triple("2026-07-25", "40", "29", "33"),
            Triple("2026-07-26", "94", "40", "29"),
            Triple("2026-07-27", "21", "16", "32"),
            Triple("2026-07-28", "10", "20", "94"),
            Triple("2026-07-29", "88", "63", "62"),
            Triple("2026-07-30", "38", "32", "75"),
            Triple("2026-07-31", "19", "35", "06"),
            Triple("2026-08-01", "11", "55", "33"),
            Triple("2026-08-02", "03", "94", "06"),
            Triple("2026-08-03", "18", "97", "12"),
            Triple("2026-08-04", "44", "94", "73"),
            Triple("2026-08-05", "47", "96", "44"),
            Triple("2026-08-06", "97", "55", "75"),
            Triple("2026-08-07", "73", "20", "90"),
            Triple("2026-08-08", "27", "11", "23"),
            Triple("2026-08-09", "82", "92", "63"),
            Triple("2026-08-10", "40", "27", "63"),
            Triple("2026-08-11", "59", "47", "95"),
            Triple("2026-08-12", "16", "19", "27"),
            Triple("2026-08-13", "22", "55", "85"),
            Triple("2026-08-14", "24", "59", "10"),
            Triple("2026-08-15", "88", "44", "19"),
            Triple("2026-08-16", "33", "30", "44"),
            Triple("2026-08-17", "85", "75", "43"),
            Triple("2026-08-18", "08", "77", "38"),
            Triple("2026-08-19", "70", "37", "25"),
            Triple("2026-08-20", "39", "89", "29"),
            Triple("2026-08-21", "42", "84", "59"),
            Triple("2026-08-22", "45", "15", "02"),
            Triple("2026-08-23", "74", "63", "20"),
            Triple("2026-08-24", "37", "47", "85"),
            Triple("2026-08-25", "28", "92", "55"),
            Triple("2026-08-26", "33", "19", "84"),
            Triple("2026-08-27", "11", "32", "85"),
            Triple("2026-08-28", "15", "92", "32"),
            Triple("2026-08-29", "67", "40", "22"),
            Triple("2026-08-30", "97", "12", "33"),
            Triple("2026-08-31", "82", "00", "10"),
            Triple("2026-09-01", "30", "45", "84"),
            Triple("2026-09-02", "36", "67", "27"),
            Triple("2026-09-03", "93", "29", "20"),
            Triple("2026-09-04", "12", "55", "73"),
            Triple("2026-09-05", "70", "06", "32"),
            Triple("2026-09-06", "27", "47", "07"),
            Triple("2026-09-07", "10", "55", "32"),
            Triple("2026-09-08", "06", "11", "84"),
            Triple("2026-09-09", "32", "08", "97"),
            Triple("2026-09-10", "94", "39", "84"),
            Triple("2026-09-11", "29", "50", "44"),
            Triple("2026-09-12", "75", "76", "76"),
            Triple("2026-09-13", "98", "82", "19"),
            Triple("2026-09-14", "20", "30", "62"),
            Triple("2026-09-15", "97", "63", "46"),
            Triple("2026-09-16", "44", "62", "94"),
            Triple("2026-09-17", "94", "67", "00"),
            Triple("2026-09-18", "76", "87", "29"),
            Triple("2026-09-19", "00", "59", "35"),
            Triple("2026-09-20", "30", "20", "75"),
            Triple("2026-09-21", "55", "59", "19"),
            Triple("2026-09-22", "85", "01", "27"),
            Triple("2026-09-23", "40", "67", "94"),
            Triple("2026-09-24", "00", "46", "98"),
            Triple("2026-09-25", "39", "33", "67"),
            Triple("2026-09-26", "28", "16", "75"),
            Triple("2026-09-27", "93", "98", "32"),
            Triple("2026-09-28", "20", "85", "48"),
            Triple("2026-09-29", "16", "67", "33"),
            Triple("2026-09-30", "85", "45", "76"),
            Triple("2026-10-01", "93", "76", "55"),
            Triple("2026-10-02", "02", "62", "27"),
            Triple("2026-10-03", "98", "06", "77"),
            Triple("2026-10-04", "38", "50", "11"),
            Triple("2026-10-05", "85", "20", "30"),
            Triple("2026-10-06", "82", "47", "29"),
            Triple("2026-10-07", "47", "55", "44"),
            Triple("2026-10-08", "11", "60", "33"),
            Triple("2026-10-09", "84", "01", "30"),
            Triple("2026-10-10", "23", "93", "24"),
            Triple("2026-10-11", "97", "22", "64"),
            Triple("2026-10-12", "57", "43", "53"),
            Triple("2026-10-13", "94", "81", "55"),
            Triple("2026-10-14", "20", "43", "39")
        )
    }
}
