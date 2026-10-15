package com.realpredict.domain.model

import com.realpredict.common.Constants
import kotlinx.serialization.Serializable

/**
 * Estado de validación de un sorteo histórico (Sección 4).
 * No usar para resultados oficiales hasta que se verifique la fecha.
 */
@Serializable
enum class VerificationStatus {
    VERIFIED,     // Validado por fuente oficial
    UNVERIFIED,   // Pendiente de validación
    FLAGGED       // Marcado por revisión (posible error)
}

/**
 * Sorteo histórico. (Sección 4 — Draw)
 *
 * Los números se conservan obligatoriamente como cadenas de 2 dígitos "00"-"99".
 * La unicidad respeta (drawDate, drawType, sourceId). No se eliminan registros
 * solo porque sus números coincidan.
 */
@Serializable
data class Draw(
    val id: String,
    val drawDate: String,        // ISO yyyy-MM-dd
    val drawType: String = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
    val firstNumber: String,    // "00".."99"
    val secondNumber: String,   // "00".."99"
    val thirdNumber: String,    // "00".."99"
    val sourceId: String,
    val sourceRow: Int? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val createdAt: String       // ISO datetime
) {
    init {
        require(Constants.isValidNumber(firstNumber)) { "firstNumber inválido: $firstNumber" }
        require(Constants.isValidNumber(secondNumber)) { "secondNumber inválido: $secondNumber" }
        require(Constants.isValidNumber(thirdNumber)) { "thirdNumber inválido: $thirdNumber" }
    }

    fun toTriple(): Triple<String, String, String> = Triple(firstNumber, secondNumber, thirdNumber)

    fun allNumbers(): List<String> = listOf(firstNumber, secondNumber, thirdNumber)
}
