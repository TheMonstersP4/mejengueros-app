package example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LaunchListUiState(
    val isLoading: Boolean = false,
    val launches: List<LaunchUiModel> = emptyList(),
    val errorMessage: String? = null,
)

class LaunchListViewModel(
    private val repository: LaunchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LaunchListUiState())
    val uiState: StateFlow<LaunchListUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                repository.latestLaunches.collect { launches ->
                    _uiState.value = LaunchListUiState(launches = launches)
                }
            }.onFailure { error ->
                _uiState.value = LaunchListUiState(errorMessage = error.message)
            }
        }
    }
}
