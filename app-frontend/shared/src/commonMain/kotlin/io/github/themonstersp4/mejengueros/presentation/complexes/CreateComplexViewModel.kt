package io.github.themonstersp4.mejengueros.presentation.complexes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateComplexViewModel(
    private val complexRepository: IComplexRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(CreateComplexUiState())
  val uiState: StateFlow<CreateComplexUiState> = _uiState.asStateFlow()

  fun updateComplexName(value: String) {
    _uiState.value =
        _uiState.value.copy(
            complexName = value,
            errorMessage = null,
            successMessage = null,
            createdComplex = null,
        )
  }

  fun updateComplexAddress(value: String) {
    _uiState.value =
        _uiState.value.copy(
            complexAddress = value,
            errorMessage = null,
            successMessage = null,
            createdComplex = null,
        )
  }

  fun updateFirstCourtName(value: String) {
    _uiState.value =
        _uiState.value.copy(
            firstCourtName = value,
            errorMessage = null,
            successMessage = null,
            createdComplex = null,
        )
  }

  fun submit() {
    val currentState = _uiState.value
    if (!currentState.canSubmit) {
      _uiState.value =
          currentState.copy(
              errorMessage =
                  "Completá nombre del complejo, dirección y nombre de la primera cancha.",
              successMessage = null,
              createdComplex = null,
          )
      return
    }

    coroutineScope.launch {
      _uiState.value =
          currentState.copy(isSubmitting = true, errorMessage = null, successMessage = null)

      runCatching {
            complexRepository.createComplex(
                CreateComplexRequest(
                    complexName = currentState.complexName.trim(),
                    complexAddress = currentState.complexAddress.trim(),
                    firstCourtName = currentState.firstCourtName.trim(),
                )
            )
          }
          .onSuccess { createdComplex ->
            _uiState.value =
                CreateComplexUiState(
                    successMessage = "Complejo y primera cancha creados correctamente.",
                    createdComplex = createdComplex,
                )
          }
          .onFailure { error ->
            _uiState.value =
                currentState.copy(
                    isSubmitting = false,
                    errorMessage = error.toUserMessage(),
                    successMessage = null,
                    createdComplex = null,
                )
          }
    }
  }
}

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          if (statusCode == 403) {
            "Tu cuenta está autenticada, pero todavía no tiene el rol OWNER local. Pedí la provisión demo e intentá de nuevo."
          } else {
            message
          }
      else -> message ?: "No se pudo crear el complejo en este momento."
    }
