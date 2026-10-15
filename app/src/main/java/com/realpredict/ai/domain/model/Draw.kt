package com.realpredict.ai.domain.model

/**
 * Representa un sorteo histórico de la Quiniela Real de República Dominicana.
 * Los números se conservan obligatoriamente como cadenas de 2 dígitos: "00" a "99".
 */
data class Draw(
    val id: String,
    val drawDate: String, // Formato ISO YYYY-MM-DD
    val drawType: String = "QUINIELA_REAL_1PM",
    val firstNumber: String,  // Formato estricto "00" - "99"
    val secondNumber: String, // Formato estricto "00" - "99"
    val thirdNumber: String,  // Formato estricto "00" - "99"
    val sourceId: String,
    val sourceRow: Int? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val createdAt: String
)

enum class VerificationStatus {
    VERIFIED,
    UNVERIFIED,
    FLAGGED
}

data class PredictionRecord(
    val id: String,
    val createdAt: String,
    val targetDrawDate: String,
    val drawType: String = "QUINIELA_REAL_1PM",
    val datasetSnapshotId: String,
    val algorithmVersion: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val mode: String, // "QUICK", "DEEP", "LABORATORY"
    val mainTriple: Triple<String, String, String>,
    val alternativeTripleA: Triple<String, String, String>? = null,
    val alternativeTripleB: Triple<String, String, String>? = null,
    val selectionMethod: String,
    val evidenceSummary: String,
    val limitations: String,
    val validationStatus: String = "SEALED",
    val integrityHash: String
)
