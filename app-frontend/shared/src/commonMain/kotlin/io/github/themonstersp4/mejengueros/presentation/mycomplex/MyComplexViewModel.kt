package io.github.themonstersp4.mejengueros.presentation.mycomplex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyComplexViewModel(
    private val repository: IComplexRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(MyComplexUiState())
  val uiState: StateFlow<MyComplexUiState> = _uiState.asStateFlow()

  fun refresh() {
    coroutineScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

      runCatching { repository.getMyComplexHub() }
          .onSuccess { hub ->
            _uiState.value =
                MyComplexUiState(
                    isLoading = false,
                    complexes = hub.complexes,
                    errorMessage = null,
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              _uiState.value = _uiState.value.copy(isLoading = false)
              return@onFailure
            }

            _uiState.value =
                MyComplexUiState(
                    isLoading = false,
                    complexes = emptyList(),
                    errorMessage = error.toUserMessage(),
                )
          }
    }
  }
}

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para ver tus complejos."
            else -> "No pudimos cargar tu hub de complejos. Intentá de nuevo."
          }
      else -> "No pudimos cargar tu hub de complejos. Intentá de nuevo."
    }
