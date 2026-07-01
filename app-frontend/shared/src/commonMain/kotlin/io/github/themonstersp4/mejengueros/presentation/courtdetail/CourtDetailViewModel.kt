package io.github.themonstersp4.mejengueros.presentation.courtdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourtDetailViewModel(
    private val courtId: String,
    private val repository: ICourtDetailRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(CourtDetailUiState())
  val uiState: StateFlow<CourtDetailUiState> = _uiState.asStateFlow()

  init {
    loadSlots()
  }

  fun retryLoad() {
    loadSlots()
  }

  private fun loadSlots() {
    scope.launch {
      _uiState.value = CourtDetailUiState(isLoadingSlots = true, slotsErrorMessage = null)
      try {
        val slots =
            repository.getReservableSlotsForToday(courtId).map { slot ->
              CourtDetailSlot(displayTime = slot.displayStartTime)
            }
        _uiState.value = CourtDetailUiState(isLoadingSlots = false, slots = slots)
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value =
            CourtDetailUiState(
                isLoadingSlots = false,
                slotsErrorMessage =
                    "No pudimos cargar la disponibilidad en este momento. Intentá nuevamente.",
            )
      }
    }
  }
}
