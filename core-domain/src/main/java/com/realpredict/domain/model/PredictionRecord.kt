package com.realpredict.domain.model

import com.realpredict.common.AnalysisMode
import com.realpredict.common.Constants
import kotlinx.serialization.Serializable

/**
 * Predicción registrada inmutable (Sección 4 — PredictionRecord).
 *
 * No puede editarse ni sobrescribirse una vez registrada.
 * Correcciones → nueva versión vinculada al registro anterior (ver `previousVersionId`).
 *
 * `integrityHash` debe calcularse sobre los campos determinísticos
 * (createdAt, targetDrawDate, drawType, mainTriple, selectionMethod, algorithmVersion).
 */
@Serializable
data class PredictionRecord(
    val id: String,
    val createdAt: String,
    val targetDrawDate: String,
    val drawType: String = Constants.DRAW_TYPE_QUINIELA_REAL_1PM,
    val datasetSnapshotId: String,
    val algorithmVersion: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val mode: AnalysisMode,
    val mainTriple: Triple<String, String, String>,
    val alternativeTripleA: Triple<String, String, String>? = null,
    val alternativeTripleB: Triple<String, String, String>? = null,
    val selectionMethod: String,
    val evidenceSummary: String,
    val limitations: String,
    val validationStatus: String = "SEALED",
    val integrityHash: String,
    val previousVersionId: String? = null
) {
    init {
        require(Constants.isValidNumber(mainTriple.first)) { "mainTriple.first inválido" }
        require(Constants.isValidNumber(mainTriple.second)) { "mainTriple.second inválido" }
        require(Constants.isValidNumber(mainTriple.third)) { "mainTriple.third inválido" }
        alternativeTripleA?.let {
            require(Constants.isValidNumber(it.first)) { "altA.first inválido" }
            require(Constants.isValidNumber(it.second)) { "altA.second inválido" }
            require(Constants.isValidNumber(it.third)) { "altA.third inválido" }
        }
        alternativeTripleB?.let {
            require(Constants.isValidNumber(it.first)) { "altB.first inválido" }
            require(Constants.isValidNumber(it.second)) { "altB.second inválido" }
            require(Constants.isValidNumber(it.third)) { "altB.third inválido" }
        }
    }

    /**
     * Versión corta legible del triple principal.
     */
    fun mainTripleDisplay(): String = "${mainTriple.first}-${mainTriple.second}-${mainTriple.third}"
}
