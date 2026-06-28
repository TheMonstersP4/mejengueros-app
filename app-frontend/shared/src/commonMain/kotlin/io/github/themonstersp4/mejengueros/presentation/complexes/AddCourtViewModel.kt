package io.github.themonstersp4.mejengueros.presentation.complexes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddCourtViewModel(
    private val complexId: String,
    private val complexName: String,
    private val repository: IComplexRepository,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(AddCourtUiState(complexName = complexName))
  val uiState: StateFlow<AddCourtUiState> = _uiState.asStateFlow()

  init {
    refreshServices()
  }

  fun updateCourtName(value: String) {
    _uiState.value =
        _uiState.value.copy(courtName = value, formErrorMessage = null, createdCourt = null)
  }

  fun toggleCourtService(serviceId: String) {
    val selected = _uiState.value.selectedCourtServiceIds
    _uiState.value =
        _uiState.value.copy(
            selectedCourtServiceIds =
                if (selected.contains(serviceId)) selected - serviceId else selected + serviceId,
            formErrorMessage = null,
            createdCourt = null,
        )
  }

  fun refreshServices() {
    _uiState.value =
        _uiState.value.copy(
            isLoadingServices = true,
            loadErrorMessage = null,
            formErrorMessage = null,
            createdCourt = null,
        )

    coroutineScope.launch {
      runCatching { repository.getServices(ServiceScope.COURT) }
          .onSuccess { services ->
            _uiState.value =
                _uiState.value.copy(
                    courtServices = services,
                    isLoadingServices = false,
                    loadErrorMessage = null,
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              return@onFailure
            }

            _uiState.value =
                _uiState.value.copy(
                    courtServices = emptyList(),
                    isLoadingServices = false,
                    loadErrorMessage = error.toCatalogLoadUserMessage(),
                )

            errorReporter.reportRecoverableFailure(
                name = "add_court_services_load_failed",
                attributes = error.toReportAttributes(operation = "load_services"),
            )
          }
    }
  }

  fun submit() {
    val currentState = _uiState.value
    if (!currentState.canSubmit) {
      _uiState.value =
          currentState.copy(
              formErrorMessage =
                  "Completá el nombre de la cancha y elegí al menos un servicio de cancha.",
              createdCourt = null,
          )
      return
    }

    coroutineScope.launch {
      _uiState.value =
          currentState.copy(isSubmitting = true, formErrorMessage = null, loadErrorMessage = null)

      runCatching {
            repository.addCourt(
                complexId = complexId,
                request =
                    CreateCourtRequest(
                        name = currentState.courtName.trim(),
                        serviceIds = currentState.selectedCourtServiceIds,
                    ),
            )
          }
          .onSuccess { court ->
            _uiState.value =
                currentState.copy(
                    isSubmitting = false,
                    loadErrorMessage = null,
                    formErrorMessage = null,
                    createdCourt = court,
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              _uiState.value = currentState.copy(isSubmitting = false)
              return@onFailure
            }

            _uiState.value =
                currentState.copy(
                    isSubmitting = false,
                    formErrorMessage = error.toSubmitUserMessage(),
                    createdCourt = null,
                )

            errorReporter.reportRecoverableFailure(
                name = "add_court_submit_failed",
                attributes = error.toReportAttributes(operation = "submit"),
            )
          }
    }
  }

  fun acknowledgeSuccess() {
    _uiState.value = _uiState.value.copy(createdCourt = null, formErrorMessage = null)
  }
}

private fun Throwable.toCatalogLoadUserMessage(): String =
    "No pudimos cargar los servicios de cancha. Intentá de nuevo."

private fun Throwable.toSubmitUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para agregar canchas a este complejo."
            404 -> "No encontramos el complejo seleccionado."
            else -> "No pudimos guardar la cancha. Intentá de nuevo."
          }
      else -> "No pudimos guardar la cancha. Intentá de nuevo."
    }

private fun Throwable.toReportAttributes(operation: String): Map<String, String> {
  val baseAttributes = mapOf("operation" to operation)

  return when (this) {
    is AppApiException ->
        baseAttributes +
            mapOf(
                "error_source" to "app_api",
                "status_code" to statusCode.toString(),
            )
    else -> baseAttributes + mapOf("error_source" to "unexpected")
  }
}
