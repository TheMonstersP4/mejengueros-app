package io.github.themonstersp4.mejengueros.presentation.courtdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtReviewsRepository
import io.github.themonstersp4.mejengueros.domain.time.parseUtcCalendarDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourtDetailViewModel(
    private val courtId: String,
    private val repository: ICourtDetailRepository,
    private val reviewsRepository: ICourtReviewsRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(CourtDetailUiState())
  val uiState: StateFlow<CourtDetailUiState> = _uiState.asStateFlow()

  init {
    loadSlots()
    loadReviews()
  }

  fun retryLoad() {
    loadSlots()
  }

  fun retryLoadReviews() {
    loadReviews()
  }

  private fun loadSlots() {
    scope.launch {
      _uiState.update { it.copy(isLoadingSlots = true, slotsErrorMessage = null) }
      try {
        val availabilityPreview = repository.getUpcomingReservableSlotsPreview(courtId)
        val slots =
            availabilityPreview
                ?.slots
                ?.map { slot -> CourtDetailSlot(displayTime = slot.displayStartTime) }
                .orEmpty()
        _uiState.update {
          it.copy(
              isLoadingSlots = false,
              slots = slots,
              slotsErrorMessage = null,
              availabilityHeadline = availabilityPreview?.toAvailabilityHeadline(),
          )
        }
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.update {
          it.copy(
              isLoadingSlots = false,
              slots = emptyList(),
              availabilityHeadline = null,
              slotsErrorMessage =
                  "No pudimos cargar la disponibilidad en este momento. Intentá nuevamente.",
          )
        }
      }
    }
  }

  private fun loadReviews() {
    scope.launch {
      _uiState.update { it.copy(isLoadingReviews = true, reviewsErrorMessage = null) }
      try {
        val reviews = reviewsRepository.getCourtReviews(courtId)
        _uiState.update {
          it.copy(isLoadingReviews = false, reviews = reviews, reviewsErrorMessage = null)
        }
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.update {
          it.copy(
              isLoadingReviews = false,
              reviews = emptyList(),
              reviewsErrorMessage =
                  "No pudimos cargar las reseñas en este momento. Intentá nuevamente.",
          )
        }
      }
    }
  }
}

private fun ReservationDayAvailability.toAvailabilityHeadline(): String {
  val date = parseUtcCalendarDate(dateUtc)
  return if (dateUtc == referenceDateUtc) {
    "Hoy · slots de 1 hora"
  } else {
    "Próximo día disponible · ${date.shortWeekdayLabel()}, ${date.day} de ${date.monthName()}"
  }
}
