package com.realpredict.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realpredict.common.DispatcherProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.repository.DrawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla History.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val draws: List<Draw> = emptyList(),
    val totalDraws: Int = 0,
    val errorMessage: String? = null
)

/**
 * ViewModel para HistoryScreen.
 * Lista todos los sorteos de la BD ordenados por fecha descendente.
 */
class HistoryViewModel(
    private val drawRepository: DrawRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeDraws()
    }

    private fun observeDraws() {
        viewModelScope.launch(dispatchers.io) {
            try {
                drawRepository.observeAll().collect { draws ->
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        draws = draws,
                        totalDraws = draws.size,
                        errorMessage = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.value = HistoryUiState(
                    isLoading = false,
                    errorMessage = "Error al cargar historial: ${t.message}"
                )
            }
        }
    }
}
