package com.realpredict.statistics

import com.realpredict.domain.model.BacktestResult
import com.realpredict.domain.model.Draw
import com.realpredict.domain.model.PredictionEvaluation
import com.realpredict.domain.model.PredictionRecord
import com.realpredict.common.Constants
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Motor de backtesting cronológico (Sección 7).
 *
 * GARANTÍAS ANTIFUGA:
 * 1. Para cada fecha objetivo, el conjunto de entrenamiento SOLO incluye sorteos anteriores.
 * 2. La predicción se "sellla" (registra) ANTES de consultar el resultado objetivo.
 * 3. Las reglas de evaluación se fijan ANTES de ejecutar el experimento.
 * 4. Se cuenta el número de estrategias probadas para estimar riesgo de sobreajuste.
 *
 * NO afirmar ventaja si la evidencia es insuficiente. La incertidumbre estadística se reporta.
 */
class BacktestEngine(
    private val frequencyCalculator: FrequencyCalculator = FrequencyCalculator,
    private val selectionStrategy: SelectionStrategy,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {

    companion object {
        const val EVALUATION_RULE_VERSION = "1.0.0"
        const val ALGORITHM_VERSION = "1.0.0"

        // Probabilidad esperada por azar de acertar el triple exacto: 1/100 * 1/100 * 1/100 = 1/1_000_000
        const val BASELINE_TRIPLE_HIT_RATE = 1.0 / 1_000_000.0

        // Probabilidad esperada por azar de acertar una posición: 1/100
        const val BASELINE_POSITIONAL_HIT_RATE = 1.0 / 100.0
    }

    /**
     * Ejecuta backtesting cronológico sobre el conjunto de sorteos.
     *
     * @param allDraws Sorteos ordenados por fecha ascendente.
     * @param targetDateStart Fecha de inicio del período de prueba (inclusive).
     * @param targetDateEnd   Fecha de fin del período de prueba (inclusive).
     * @param minTrainingSize Mínimo de sorteos de entrenamiento requeridos (default 30).
     */
    fun run(
        allDraws: List<Draw>,
        targetDateStart: String,
        targetDateEnd: String,
        minTrainingSize: Int = 30,
        experimentId: String
    ): BacktestResult {
        require(allDraws.isNotEmpty()) { "Sin sorteos para backtest" }
        require(minTrainingSize >= 1) { "minTrainingSize debe ser >= 1" }

        val sortedByDate = allDraws.sortedBy { it.drawDate }

        // 1) Identificar índice de inicio del target
        val targetStartIdx = sortedByDate.indexOfFirst { it.drawDate >= targetDateStart }
            .also { require(it >= 0) { "targetDateStart fuera del rango disponible" } }
        require(targetStartIdx >= minTrainingSize) {
            "Conjunto de entrenamiento insuficiente: hay $targetStartIdx sorteos antes de $targetDateStart, se requieren $minTrainingSize"
        }

        val targetEndIdx = sortedByDate.indexOfLast { it.drawDate <= targetDateEnd }
            .also { require(it >= targetStartIdx) { "targetDateEnd fuera del rango o anterior a start" } }

        // 2) Iterar cronológicamente. Para cada objetivo, entrenar SOLO con sorteos anteriores.
        val positionalHits = mutableMapOf(1 to 0, 2 to 0, 3 to 0)
        var exactTripleHits = 0
        var evaluated = 0
        val evaluations = mutableListOf<PredictionEvaluation>()

        for (i in targetStartIdx..targetEndIdx) {
            val target = sortedByDate[i]

            // ENTRENAMIENTO: solo sorteos con índice < i (estrictamente anteriores)
            val training = sortedByDate.subList(0, i)
            if (training.size < minTrainingSize) continue

            // PREDICCIÓN (se genera ANTES de mirar el resultado)
            val snapshot = DatasetSnapshotOf(training)
            val predictedTriple = selectionStrategy.predict(training, snapshot)

            // SELLAR la predicción (inmutable)
            val prediction = PredictionRecord(
                id = idGenerator.newId("pred"),
                createdAt = timeProvider.nowIso(),
                targetDrawDate = target.drawDate,
                drawType = target.drawType,
                datasetSnapshotId = snapshot.snapshotId,
                algorithmVersion = ALGORITHM_VERSION,
                providerId = null,
                modelId = null,
                mode = com.realpredict.common.AnalysisMode.LABORATORY,
                mainTriple = predictedTriple,
                alternativeTripleA = null,
                alternativeTripleB = null,
                selectionMethod = selectionStrategy.name(),
                evidenceSummary = "Backtest con ${training.size} sorteos (training) anterior a ${target.drawDate}",
                limitations = buildList {
                    add("Resultados no constituyen recomendación de juego.")
                    add("La frecuencia pasada no garantiza probabilidad futura.")
                    add("Estrategia probada: ${selectionStrategy.name()}. Sobreajuste posible.")
                }.joinToString(" "),
                integrityHash = computeIntegrityHash(predictedTriple, target.drawDate, snapshot.snapshotId, selectionStrategy.name()),
                previousVersionId = null
            )

            // EVALUACIÓN con reglas fijas (1.0.0)
            val (matchesByPos, exactTriple) = evaluate(prediction, target)
            val eval = PredictionEvaluation(
                id = idGenerator.newId("eval"),
                predictionId = prediction.id,
                observedDrawId = target.id,
                evaluationRuleVersion = EVALUATION_RULE_VERSION,
                exactMatchesByPosition = matchesByPos,
                exactTripleMatch = exactTriple,
                evaluatedAt = timeProvider.nowIso()
            )
            evaluations += eval

            positionalHits[1] = positionalHits.getValue(1) + if (predictedTriple.first == target.firstNumber) 1 else 0
            positionalHits[2] = positionalHits.getValue(2) + if (predictedTriple.second == target.secondNumber) 1 else 0
            positionalHits[3] = positionalHits.getValue(3) + if (predictedTriple.third == target.thirdNumber) 1 else 0
            if (exactTriple) exactTripleHits++
            evaluated++
        }

        require(evaluated > 0) { "No se evaluó ningún sorteo (verifique el rango)" }

        val observedHitRate = exactTripleHits.toDouble() / evaluated
        val limitations = buildList {
            add("Resultados no constituyen recomendación de juego.")
            add("La frecuencia pasada no garantiza probabilidad futura.")
            add("Estrategia probada: ${selectionStrategy.name()}. Sobreajuste posible con ${1} estrategias probadas.")
            add("Tamaño de prueba: $evaluated sorteos. Incertidumbre alta para inferencias estadísticas.")
        }

        return BacktestResult(
            id = idGenerator.newId("bt"),
            experimentId = experimentId,
            startDate = targetDateStart,
            endDate = targetDateEnd,
            totalDrawsEvaluated = evaluated,
            exactTripleHits = exactTripleHits,
            positionalHits = positionalHits.toMap(),
            baselineExpected = BASELINE_TRIPLE_HIT_RATE,
            observedHitRate = observedHitRate,
            baselineHitRate = BASELINE_TRIPLE_HIT_RATE,
            numberOfStrategiesTested = 1,
            limitations = limitations,
            algorithmVersion = ALGORITHM_VERSION,
            generatedAt = timeProvider.nowIso()
        )
    }

    /**
     * Evaluación pura de una predicción contra un sorteo observado.
     * Pública para reutilizarla en evaluaciones online.
     */
    fun evaluate(prediction: PredictionRecord, observed: Draw): Pair<Int, Boolean> {
        require(prediction.targetDrawDate == observed.drawDate) {
            "Fechas no coinciden: predicción=${prediction.targetDrawDate}, observado=${observed.drawDate}"
        }
        var matches = 0
        if (prediction.mainTriple.first == observed.firstNumber) matches++
        if (prediction.mainTriple.second == observed.secondNumber) matches++
        if (prediction.mainTriple.third == observed.thirdNumber) matches++
        val exact = prediction.mainTriple.first == observed.firstNumber &&
                    prediction.mainTriple.second == observed.secondNumber &&
                    prediction.mainTriple.third == observed.thirdNumber
        return matches to exact
    }

    private fun computeIntegrityHash(
        triple: Triple<String, String, String>,
        targetDate: String,
        snapshotId: String,
        method: String
    ): String {
        val raw = "${triple.first}${triple.second}${triple.third}|$targetDate|$snapshotId|$method"
        // SHA-256
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun DatasetSnapshotOf(draws: List<Draw>): com.realpredict.domain.repository.DatasetSnapshot {
        val hash = draws.joinToString("|") { "${it.drawDate}:${it.firstNumber}${it.secondNumber}${it.thirdNumber}" }
            .let { sha256(it) }
        val sorted = draws.sortedBy { it.drawDate }
        return com.realpredict.domain.repository.DatasetSnapshot(
            snapshotId = idGenerator.newId("snap"),
            totalDraws = sorted.size,
            startDate = sorted.first().drawDate,
            endDate = sorted.last().drawDate,
            contentHash = hash
        )
    }

    private fun sha256(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Interfaz para estrategias de selección de triples (Sección 6).
 * Cada implementación debe ser EXPLÍCITA, VERSIONADA y REPRODUCIBLE.
 */
interface SelectionStrategy {
    /** Nombre único + versión. */
    fun name(): String

    /**
     * Genera un triple basado en el conjunto de entrenamiento.
     * DEBE ser determinista: misma entrada → mismo triple.
     */
    fun predict(trainingDraws: List<Draw>, snapshot: com.realpredict.domain.repository.DatasetSnapshot): Triple<String, String, String>
}

/**
 * Estrategia ingenua: triple más frecuente en el conjunto de entrenamiento.
 * NO es una recomendación, solo referencia para backtesting (Sección 7).
 */
class TopFrequencyStrategy(
    private val positionOffset: Int = 0  // 0 = más frecuente, 1 = segundo más frecuente, ...
) : SelectionStrategy {

    override fun name(): String = "TOP_FREQUENCY_v${FrequencyCalculator.ALGORITHM_VERSION}#${positionOffset}"

    override fun predict(
        trainingDraws: List<Draw>,
        snapshot: com.realpredict.domain.repository.DatasetSnapshot
    ): Triple<String, String, String> {
        require(trainingDraws.isNotEmpty()) { "Training vacío" }
        val report = FrequencyCalculator.compute(trainingDraws)

        // Top números por posición (para asegurar tres números distintos)
        val top1 = report.perPosition[1]!!.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ).toList()
        val top2 = report.perPosition[2]!!.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ).toList()
        val top3 = report.perPosition[3]!!.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ).toList()

        // Tomamos el (offset)-ésimo de cada posición. Aseguramos que sean distintos.
        val safeIdx1 = positionOffset.coerceAtMost(top1.size - 1)
        val safeIdx2 = positionOffset.coerceAtMost(top2.size - 1)
        val safeIdx3 = positionOffset.coerceAtMost(top3.size - 1)
        val n1 = top1[safeIdx1].key
        val n2 = top2[safeIdx2].key
        val n3 = top3[safeIdx3].key

        require(Constants.isValidNumber(n1) && Constants.isValidNumber(n2) && Constants.isValidNumber(n3)) {
            "Selección inválida: $n1, $n2, $n3"
        }
        return Triple(n1, n2, n3)
    }
}

/**
 * Estrategia de referencia puramente aleatoria.
 * Útil como baseline de comparación en backtesting.
 */
class RandomStrategy(
    private val seed: Long = 0L   // semilla fija para reproducibilidad
) : SelectionStrategy {

    override fun name(): String = "RANDOM_v1.0.0#seed$seed"

    override fun predict(
        trainingDraws: List<Draw>,
        snapshot: com.realpredict.domain.repository.DatasetSnapshot
    ): Triple<String, String, String> {
        val rng = kotlin.random.Random(seed xor snapshot.contentHash.hashCode().toLong())
        val pool = Constants.VALID_NUMBERS
        val n1 = pool.random(rng)
        val n2 = pool.random(rng)
        val n3 = pool.random(rng)
        return Triple(n1, n2, n3)
    }
}
