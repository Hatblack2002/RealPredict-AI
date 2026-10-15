package com.realpredict.statistics

import com.google.common.truth.Truth.assertThat
import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OutlierDetectorTest {

    @Test
    fun `triple identico se marca como WARN`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "11", "11", "11"),
            sampleDraw("2024-01-02", "05", "10", "20")  // normal
        )
        val report = OutlierDetector.analyze(draws)
        assertThat(report.flagged).hasSize(1)
        assertThat(report.flagged.first().severity).isEqualTo(OutlierSeverity.WARN)
        assertThat(report.flagged.first().reason).contains("idénticos")
    }

    @Test
    fun `secuencia ascendente se marca como INFO`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "01", "02", "03"),
            sampleDraw("2024-01-02", "05", "10", "20")
        )
        val report = OutlierDetector.analyze(draws)
        val seqFlags = report.flagged.filter { it.severity == OutlierSeverity.INFO }
        assertThat(seqFlags).hasSize(1)
        assertThat(seqFlags.first().reason).contains("secuencia")
    }

    @Test
    fun `numeros fuera de rango se marcan como ERROR`() {
        // El constructor de Draw valida y debería fallar antes. Pero forzamos aquí.
        // Como Draw lanza excepción en init con número inválido, este test verifica que no se acepta.
        try {
            sampleDraw("2024-01-01", "100", "10", "20")  // "100" no es 2 dígitos
            assert(false) { "Draw debería rechazar '100'" }
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `comparacion de periodos devuelve diferencias`() {
        val p1 = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-02", "06", "11", "21")
        )
        val p2 = listOf(
            sampleDraw("2024-02-01", "07", "12", "22"),
            sampleDraw("2024-02-02", "08", "13", "23")
        )
        val diff = OutlierDetector.comparePeriods(p1, p2)
        assertThat(diff).hasSize(100)
        // 05 aparece 1 vez en p1, 0 en p2 → diff = 1
        assertThat(diff["05"]).isEqualTo(1)
        assertThat(diff["07"]).isEqualTo(1)
        // 99 no aparece en ninguno → diff = 0
        assertThat(diff["99"]).isEqualTo(0)
    }

    @Test
    fun `total checked es igual al numero de sorteos`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-02", "06", "11", "21"),
            sampleDraw("2024-01-03", "07", "12", "22")
        )
        val report = OutlierDetector.analyze(draws)
        assertThat(report.totalChecked).isEqualTo(3)
    }

    private fun sampleDraw(date: String, a: String, b: String, c: String): Draw {
        return Draw(
            id = "draw_$date",
            drawDate = date,
            drawType = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
            firstNumber = a,
            secondNumber = b,
            thirdNumber = c,
            sourceId = "test",
            sourceRow = 1,
            verificationStatus = VerificationStatus.VERIFIED,
            createdAt = "2024-01-01T00:00:00Z"
        )
    }
}
