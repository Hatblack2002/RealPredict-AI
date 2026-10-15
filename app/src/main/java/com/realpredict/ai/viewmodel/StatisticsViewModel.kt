package com.realpredict.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realpredict.common.DispatcherProvider
import com.realpredict.domain.repository.DrawRepository
import com.realpredict.statistics.FrequencyReport
import com.realpredict.statistics.StatisticsEngine
import com.realpredict.statistics.StatisticsReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla Statistics.
 */
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalDraws: Int = 0,
    val dateRange: String = "",
    val frequencyReport: FrequencyReport? = null,
    val hotNumbers: List<Pair<String, Int>> = emptyList(),
    val coldNumbers: List<Pair<String, Int>> = emptyList(),
    val perNumber: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
)

/**
 * ViewModel para StatisticsScreen.
 * Carga todos los sorteos de la BD, invoca el motor estadístico (StatisticsEngine)
 * y expone el FrequencyReport con números hot/cold reales basados en datos verificados.
 */
class StatisticsViewModel(
    private val drawRepository: DrawRepository,
    private val statisticsEngine: StatisticsEngine,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val draws = drawRepository.observeAll().first()
                if (draws.isEmpty()) {
                    _uiState.value = StatisticsUiState(
                        isLoading = false,
                        errorMessage = "No hay sorteos importados todavía. Ve a Import para cargar datos."
                    )
                    return@launch
                }
                val report = statisticsEngine.fullReport(draws)
                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    totalDraws = report.frequency.sampleSize,
                    dateRange = report.frequency.dateRange,
                    frequencyReport = report.frequency,
                    hotNumbers = report.frequency.mostFrequent,
                    coldNumbers = report.frequency.leastFrequent,
                    perNumber = report.frequency.perNumber,
                    errorMessage = null
                )
            } catch (t: Throwable) {
                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    errorMessage = "Error al calcular estadísticas: ${t.message}"
                )
            }
        }
    }
}
