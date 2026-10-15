package com.realpredict.statistics

import com.google.common.truth.Truth.assertThat
import com.realpredict.common.Constants
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.VerificationStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AbsenceCalculatorTest {

    @Test
    fun `ausencia cuenta dias desde ultima aparicion`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-03", "06", "10", "21"),
            sampleDraw("2024-01-05", "07", "12", "22")
        )
        // referencia = 2024-01-10
        // 05 salió el 01 → 9 días ausente
        // 10 salió el 03 (última vez) → 7 días
        // 06 salió el 03 → 7 días
        // 20 salió el 01 → 9 días
        // 21 salió el 03 → 7 días
        // 07 salió el 05 → 5 días
        // 12 salió el 05 → 5 días
        // 22 salió el 05 → 5 días
        // Los otros 92 números → nunca aparecieron

        val report = AbsenceCalculator.compute(draws, referenceDate = "2024-01-10")
        assertThat(report.perNumber["05"]).isEqualTo(9)
        assertThat(report.perNumber["10"]).isEqualTo(7)
        assertThat(report.perNumber["07"]).isEqualTo(5)
        assertThat(report.perNumber["12"]).isEqualTo(5)
        assertThat(report.perNumber["22"]).isEqualTo(5)
        assertThat(report.perNumber["99"]).isEqualTo(-1)  // nunca apareció
        assertThat(report.neverAppeared).contains("99")
        assertThat(report.neverAppeared.size).isEqualTo(92)
    }

    @Test
    fun `ausencia lanza excepcion si lista vacia`() {
        try {
            AbsenceCalculator.compute(emptyList(), "2024-01-01")
            assert(false)
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("vac")  // coincide con "vacíos" o "vacia"
        }
    }

    @Test
    fun `ausencia conserva ceros iniciales`() {
        val draws = listOf(sampleDraw("2024-01-01", "00", "01", "09"))
        val report = AbsenceCalculator.compute(draws, "2024-01-02")
        assertThat(report.perNumber["00"]).isEqualTo(1)
        assertThat(report.perNumber["01"]).isEqualTo(1)
        assertThat(report.perNumber["09"]).isEqualTo(1)
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
