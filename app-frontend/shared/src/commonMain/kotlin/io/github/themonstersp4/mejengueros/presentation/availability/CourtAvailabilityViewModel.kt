package io.github.themonstersp4.mejengueros.presentation.availability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourtAvailabilityViewModel(
    private val courtId: String,
    initialCourtName: String,
    initialComplexName: String,
    private val repository: ICourtAvailabilityRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState =
      MutableStateFlow(
          CourtAvailabilityUiState(
              courtId = courtId,
              courtName = initialCourtName,
              complexName = initialComplexName,
          )
      )
  val uiState: StateFlow<CourtAvailabilityUiState> = _uiState.asStateFlow()

  init {
    load()
  }

  fun load() {
    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

      runCatching { repository.getCourtAvailability(courtId) }
          .onSuccess(::applyLoadedContext)
          .onFailure { error ->
            if (error is CancellationException) return@onFailure
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.toLoadUserMessage(),
                    successMessage = null,
                )
          }
    }
  }

  fun toggleDay(day: CourtAvailabilityWeekday) {
    val selectedDays = _uiState.value.selectedDays
    val nextDays = if (day in selectedDays) selectedDays - day else selectedDays + day
    updateSelection(days = nextDays)
  }

  fun updateStartTime(value: String) {
    updateSelection(startTime = value)
  }

  fun updateEndTime(value: String) {
    updateSelection(endTime = value)
  }

  fun save() {
    val currentState = _uiState.value
    if (currentState.selectedDays.isEmpty()) {
      _uiState.value =
          currentState.copy(
              errorMessage = "Elegí al menos un día disponible.",
              successMessage = null,
          )
      return
    }

    if (currentState.previewSlots.isEmpty()) {
      _uiState.value =
          currentState.copy(
              errorMessage = "Elegí un rango horario exacto de una hora o más.",
              successMessage = null,
          )
      return
    }

    coroutineScope.launch {
      _uiState.value =
          currentState.copy(isSaving = true, errorMessage = null, successMessage = null)

      runCatching {
            repository.saveCourtAvailability(
                courtId = courtId,
                availability =
                    CourtAvailabilityConfig(
                        days = currentState.selectedDays.toList().sortedBy { it.ordinal },
                        startTime = currentState.startTime,
                        endTime = currentState.endTime,
                    ),
            )
          }
          .onSuccess { context ->
            val refreshedState = context.toUiState(currentState = _uiState.value)
            _uiState.value =
                refreshedState.copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = null,
                    successMessage = "Disponibilidad guardada correctamente.",
                )
          }
          .onFailure { error ->
            if (error is CancellationException) return@onFailure
            _uiState.value =
                _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.toSaveUserMessage(),
                    successMessage = null,
                )
          }
    }
  }

  private fun applyLoadedContext(context: CourtAvailabilityContext) {
    _uiState.value =
        context
            .toUiState(currentState = _uiState.value)
            .copy(
                isLoading = false,
                isSaving = false,
                errorMessage = null,
                successMessage = null,
            )
  }

  private fun updateSelection(
      days: Set<CourtAvailabilityWeekday> = _uiState.value.selectedDays,
      startTime: String = _uiState.value.startTime,
      endTime: String = _uiState.value.endTime,
  ) {
    _uiState.value =
        _uiState.value.copy(
            selectedDays = days,
            startTime = startTime,
            endTime = endTime,
            previewSlots = generatePreviewSlots(startTime, endTime) ?: emptyList(),
            errorMessage = null,
            successMessage = null,
        )
  }
}

private fun CourtAvailabilityContext.toUiState(
    currentState: CourtAvailabilityUiState,
): CourtAvailabilityUiState {
  val availability = availability
  val selectedDays = availability?.days?.toSet() ?: currentState.selectedDays
  val startTime = availability?.startTime ?: currentState.startTime
  val endTime = availability?.endTime ?: currentState.endTime

  return currentState.copy(
      courtId = courtId,
      courtName = courtName,
      complexName = complexName,
      selectedDays = selectedDays,
      startTime = startTime,
      endTime = endTime,
      previewSlots = generatePreviewSlots(startTime, endTime) ?: emptyList(),
  )
}

private fun Throwable.toLoadUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para ver la disponibilidad de esta cancha."
            404 -> "No encontramos la cancha indicada para configurar disponibilidad."
            else -> "No pudimos cargar la disponibilidad en este momento. Intentá de nuevo."
          }
      else -> "No pudimos cargar la disponibilidad en este momento. Intentá de nuevo."
    }

private fun Throwable.toSaveUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para configurar la disponibilidad de esta cancha."
            in 400..499 -> "Revisá los días y el rango horario antes de guardar."
            else -> "No pudimos guardar la disponibilidad en este momento. Intentá de nuevo."
          }
      else -> "No pudimos guardar la disponibilidad en este momento. Intentá de nuevo."
    }
