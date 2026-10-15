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
class FrequencyCalculatorTest {

    @Test
    fun `frecuencia vacia lanza excepcion`() {
        try {
            FrequencyCalculator.compute(emptyList())
            assert(false) { "Debería lanzar excepción" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("vacia")
        }
    }

    @Test
    fun `frecuencia de un solo sorteo cuenta 1 aparicion por numero`() {
        val draw = sampleDraw("2024-01-01", "05", "10", "20")
        val report = FrequencyCalculator.compute(listOf(draw))
        assertThat(report.sampleSize).isEqualTo(1)
        assertThat(report.totalAppearances).isEqualTo(3)
        assertThat(report.perNumber["05"]).isEqualTo(1)
        assertThat(report.perNumber["10"]).isEqualTo(1)
        assertThat(report.perNumber["20"]).isEqualTo(1)
        assertThat(report.perNumber["00"]).isEqualTo(0)  // nunca apareció
    }

    @Test
    fun `frecuencia acumula correctamente con sorteos repetidos`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-02", "05", "10", "21"),
            sampleDraw("2024-01-03", "05", "11", "21")
        )
        val report = FrequencyCalculator.compute(draws)
        assertThat(report.sampleSize).isEqualTo(3)
        assertThat(report.totalAppearances).isEqualTo(9)  // 3 sorteos × 3 posiciones
        assertThat(report.perNumber["05"]).isEqualTo(3)  // apareció 3 veces (siempre en posición 1)
        assertThat(report.perNumber["10"]).isEqualTo(2)
        assertThat(report.perNumber["21"]).isEqualTo(2)
        assertThat(report.perPosition[1]!!["05"]).isEqualTo(3)  // posición 1
        assertThat(report.perPosition[2]!!["10"]).isEqualTo(2)
    }

    @Test
    fun `suma de apariciones siempre igual a sampleSize x 3`() {
        val draws = (1..50).map {
            sampleDraw("2024-01-${it.toString().padStart(2, '0')}",
                (it % 100).toString().padStart(2, '0'),
                ((it + 1) % 100).toString().padStart(2, '0'),
                ((it + 2) % 100).toString().padStart(2, '0'))
        }
        val report = FrequencyCalculator.compute(draws)
        assertThat(report.totalAppearances).isEqualTo(150)
        val sum = report.perNumber.values.sum()
        assertThat(sum).isEqualTo(150)
    }

    @Test
    fun `numeros con ceros iniciales se conservan`() {
        val draw = sampleDraw("2024-01-01", "00", "01", "09")
        val report = FrequencyCalculator.compute(listOf(draw))
        assertThat(report.perNumber["00"]).isEqualTo(1)
        assertThat(report.perNumber["01"]).isEqualTo(1)
        assertThat(report.perNumber["09"]).isEqualTo(1)
        assertThat(report.perNumber["9"]).isNull()  // "9" no es válido
    }

    @Test
    fun `top mas frecuentes se ordena por frecuencia descendente`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-02", "05", "10", "21"),
            sampleDraw("2024-01-03", "05", "11", "21")
        )
        val report = FrequencyCalculator.compute(draws)
        assertThat(report.mostFrequent.first().first).isEqualTo("05")
        assertThat(report.mostFrequent.first().second).isEqualTo(3)
    }

    @Test
    fun `todos los 100 numeros aparecen en el mapa`() {
        val report = FrequencyCalculator.compute(listOf(sampleDraw("2024-01-01", "00", "01", "02")))
        assertThat(report.perNumber.size).isEqualTo(100)
        Constants.VALID_NUMBERS.forEach { num ->
            assertThat(report.perNumber).containsKey(num)
        }
    }

    @Test
    fun `algoritmo es determinista - misma entrada misma salida`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "05", "10", "20"),
            sampleDraw("2024-01-02", "06", "11", "21")
        )
        val r1 = FrequencyCalculator.compute(draws)
        val r2 = FrequencyCalculator.compute(draws)
        assertThat(r1).isEqualTo(r2)
        assertThat(r1.algorithmVersion).isEqualTo("1.0.0")
    }

    @Test
    fun `distribucion reporta buckets correctos`() {
        val draws = listOf(
            sampleDraw("2024-01-01", "00", "01", "02"),
            sampleDraw("2024-01-02", "00", "01", "03")
        )
        val report = FrequencyCalculator.compute(draws)
        // 00=2, 01=2, 02=1, 03=1, los demás 97 números = 0
        assertThat(report.distribution.buckets[2]).isEqualTo(2)  // 00 y 01
        assertThat(report.distribution.buckets[1]).isEqualTo(2)  // 02 y 03
        assertThat(report.distribution.buckets[0]).isEqualTo(96)  // 97 - 1 (02) = 96? actually
        // Let's recompute: total = 100 numbers, 00=2, 01=2, 02=1, 03=1, others (96) = 0
        // So buckets: 0→96, 1→2, 2→2
    }

    // Helper
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
