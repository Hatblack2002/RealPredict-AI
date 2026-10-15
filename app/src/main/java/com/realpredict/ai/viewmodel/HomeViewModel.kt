package com.realpredict.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realpredict.common.DispatcherProvider
import com.realpredict.domain.model.Draw
import com.realpredict.domain.repository.DrawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla Home.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val totalDraws: Int = 0,
    val lastDraw: Draw? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel para HomeScreen.
 * Observa la lista de sorteos en tiempo real, expone:
 *  - totalDraws: cantidad total de sorteos en BD
 *  - lastDraw: el sorteo más reciente (para mostrar las tres LotteryBall reales)
 */
class HomeViewModel(
    private val drawRepository: DrawRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDraws()
    }

    private fun observeDraws() {
        viewModelScope.launch(dispatchers.io) {
            try {
                drawRepository.observeAll().collect { draws ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        totalDraws = draws.size,
                        lastDraw = draws.firstOrNull(),  // el DAO ya ordena DESC por fecha
                        errorMessage = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar sorteos: ${t.message}"
                )
            }
        }
    }
}
