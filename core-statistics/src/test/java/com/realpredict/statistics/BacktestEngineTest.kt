package com.realpredict.statistics

import com.google.common.truth.Truth.assertThat
import com.realpredict.common.Constants
import com.realpredict.common.DefaultTimeProvider
import com.realpredict.common.UuidIdGenerator
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BacktestEngineTest {

    private val timeProvider = DefaultTimeProvider()
    private val idGen = UuidIdGenerator()

    @Test
    fun `backtest simple con estrategia top frequency sin errores`() {
        // 50 sorteos consecutivos como dataset
        val draws = (1..50).map {
            val day = it.toString().padStart(2, '0')
            sampleDraw("2024-01-$day",
                (it % 100).toString().padStart(2, '0'),
                ((it + 5) % 100).toString().padStart(2, '0'),
                ((it + 10) % 100).toString().padStart(2, '0'))
        }

        val engine = BacktestEngine(
            selectionStrategy = TopFrequencyStrategy(positionOffset = 0),
            idGenerator = idGen,
            timeProvider = timeProvider
        )

        val result = engine.run(
            allDraws = draws,
            targetDateStart = "2024-01-31",  // sorteo #31 en adelante
            targetDateEnd = "2024-02-15",   // hasta #46
            minTrainingSize = 30,
            experimentId = "exp_test_1"
        )

        assertThat(result.totalDrawsEvaluated).isGreaterThan(0)
        assertThat(result.exactTripleHits).isAtLeast(0)
        assertThat(result.baselineHitRate).isGreaterThan(0.0)
        assertThat(result.algorithmVersion).isEqualTo("1.0.0")
        assertThat(result.limitations).isNotEmpty()
    }

    @Test
    fun `backtest no permite fuga - targetStartIdx menor que minTrainingSize falla`() {
        val draws = (1..20).map {
            val day = it.toString().padStart(2, '0')
            sampleDraw("2024-01-$day",
                "01", "02", "03")
        }

        val engine = BacktestEngine(
            selectionStrategy = TopFrequencyStrategy(),
            idGenerator = idGen,
            timeProvider = timeProvider
        )

        try {
            engine.run(
                allDraws = draws,
                targetDateStart = "2024-01-05",  // solo 4 sorteos antes - insuficiente
                targetDateEnd = "2024-01-20",
                minTrainingSize = 30,
                experimentId = "exp_fail"
            )
            assert(false) { "Debería lanzar excepción por training insuficiente" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("insuficiente")
        }
    }

    @Test
    fun `evaluacion posicional es correcta`() {
        val engine = BacktestEngine(
            selectionStrategy = TopFrequencyStrategy(),
            idGenerator = idGen,
            timeProvider = timeProvider
        )
        val observed = sampleDraw("2024-01-15", "05", "10", "20")
        val prediction = com.realpredict.domain.model.PredictionRecord(
            id = "p1",
            createdAt = "2024-01-14T00:00:00Z",
            targetDrawDate = "2024-01-15",
            datasetSnapshotId = "snap",
            algorithmVersion = "1.0.0",
            mode = com.realpredict.common.AnalysisMode.LABORATORY,
            mainTriple = Triple("05", "10", "20"),  // exact match
            selectionMethod = "TEST",
            evidenceSummary = "",
            limitations = "",
            integrityHash = "test_hash"
        )
        val (matches, exact) = engine.evaluate(prediction, observed)
        assertThat(matches).isEqualTo(3)
        assertThat(exact).isTrue()

        // Predicción parcial
        val pred2 = prediction.copy(mainTriple = Triple("05", "99", "99"))
        val (m2, e2) = engine.evaluate(pred2, observed)
        assertThat(m2).isEqualTo(1)
        assertThat(e2).isFalse()
    }

    @Test
    fun `estrategia random es reproducible con misma semilla`() {
        val draws = (1..50).map {
            val day = it.toString().padStart(2, '0')
            sampleDraw("2024-01-$day",
                (it % 100).toString().padStart(2, '0'),
                ((it + 1) % 100).toString().padStart(2, '0'),
                ((it + 2) % 100).toString().padStart(2, '0'))
        }
        val strategy = RandomStrategy(seed = 42L)
        val snap = com.realpredict.domain.repository.DatasetSnapshot(
            snapshotId = "snap_test",
            totalDraws = draws.size,
            startDate = "2024-01-01",
            endDate = "2024-02-20",
            contentHash = "fixed_hash"
        )
        val r1 = strategy.predict(draws, snap)
        val r2 = strategy.predict(draws, snap)
        assertThat(r1).isEqualTo(r2)  // determinista
        assertThat(Constants.isValidNumber(r1.first)).isTrue()
        assertThat(Constants.isValidNumber(r1.second)).isTrue()
        assertThat(Constants.isValidNumber(r1.third)).isTrue()
    }

    @Test
    fun `prediccion es inmutable - no se puede sobrescribir`() {
        // Aunque construimos un PredictionRecord, sus campos son val (inmutables por diseño Kotlin)
        val pred = com.realpredict.domain.model.PredictionRecord(
            id = "p1",
            createdAt = "2024-01-01T00:00:00Z",
            targetDrawDate = "2024-01-15",
            datasetSnapshotId = "snap",
            algorithmVersion = "1.0.0",
            mode = com.realpredict.common.AnalysisMode.LABORATORY,
            mainTriple = Triple("05", "10", "20"),
            selectionMethod = "TEST",
            evidenceSummary = "",
            limitations = "",
            integrityHash = "hash"
        )
        // No hay setters — el siguiente código no compilaría:
        // pred.id = "otro"
        // Verificamos que el campo sigue siendo el mismo
        assertThat(pred.id).isEqualTo("p1")
    }

    @Test
    fun `validacion de fecha coincide entre prediccion y observado`() {
        val engine = BacktestEngine(
            selectionStrategy = TopFrequencyStrategy(),
            idGenerator = idGen,
            timeProvider = timeProvider
        )
        val observed = sampleDraw("2024-01-15", "05", "10", "20")
        val pred = com.realpredict.domain.model.PredictionRecord(
            id = "p1",
            createdAt = "2024-01-14T00:00:00Z",
            targetDrawDate = "2024-01-16",  // FECHA DIFERENTE
            datasetSnapshotId = "snap",
            algorithmVersion = "1.0.0",
            mode = com.realpredict.common.AnalysisMode.LABORATORY,
            mainTriple = Triple("05", "10", "20"),
            selectionMethod = "TEST",
            evidenceSummary = "",
            limitations = "",
            integrityHash = "hash"
        )
        try {
            engine.evaluate(pred, observed)
            assert(false) { "Debería fallar por fecha distinta" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Fechas no coinciden")
        }
    }

    private fun sampleDraw(date: String, a: String, b: String, c: String): Draw {
        return Draw(
            id = "draw_$date",
            drawDate = date,
            drawType = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
            firstNumber = a,
            secondNumber = b,
            thirdNumber = c,
            sourceId = "test_source",
            sourceRow = 1,
            verificationStatus = VerificationStatus.VERIFIED,
            createdAt = "2024-01-01T00:00:00Z"
        )
    }
}
