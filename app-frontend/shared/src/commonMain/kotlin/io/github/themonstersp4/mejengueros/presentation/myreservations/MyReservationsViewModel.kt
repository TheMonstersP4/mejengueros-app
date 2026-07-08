package io.github.themonstersp4.mejengueros.presentation.myreservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.MyReservationCard
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaDateLabel
import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaTimeLabel
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyReservationsViewModel(
    private val reservationRepository: IReservationRepository,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(MyReservationsUiState())
  private var refreshJob: Job? = null
  private var refreshGeneration: Long = 0
  val uiState: StateFlow<MyReservationsUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    refreshJob?.cancel()
    val generation = ++refreshGeneration

    _uiState.value =
        _uiState.value.copy(
            isLoading = true,
            loadErrorMessage = null,
        )

    refreshJob =
        scope.launch {
          runCatching { reservationRepository.getMyReservations() }
              .onSuccess { reservations ->
                if (generation != refreshGeneration) return@onSuccess

                _uiState.value =
                    MyReservationsUiState(
                        isLoading = false,
                        upcoming = reservations.upcoming.map(MyReservationCard::toUiModel),
                        finalized = reservations.finalized.map(MyReservationCard::toUiModel),
                    )
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (generation != refreshGeneration) return@onFailure

                errorReporter.reportRecoverableFailure(
                    name = "my_reservations_load_failed",
                    attributes = error.toReportAttributes(),
                )

                _uiState.value =
                    MyReservationsUiState(
                        isLoading = false,
                        loadErrorMessage = error.toUserMessage(),
                    )
              }
        }
  }
}

private fun MyReservationCard.toUiModel(): MyReservationCardUiModel =
    MyReservationCardUiModel(
        id = id,
        complexName = complexName,
        courtName = courtName,
        imageUrl = imageUrl,
        startsAt = startsAt,
        endsAt = endsAt,
        status = status,
        section = section,
        reviewStatus = reviewStatus,
        canReview = canReview,
        hasReview = hasReview,
        primaryActionKey = primaryActionKey,
        primaryActionLabel = primaryActionLabel,
        indicatorKey = indicatorKey,
        indicatorLabel = indicatorLabel,
        title = "$complexName · $courtName",
        reservationLabel =
            "Reserva del ${startsAt.toCostaRicaDateLabel()} · ${startsAt.toCostaRicaTimeLabel()} – ${endsAt.toCostaRicaTimeLabel()}",
    )

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "Necesitás volver a iniciar sesión para ver tus reservas."
            else -> "No pudimos cargar tus reservas. Intentá nuevamente."
          }
      else -> "No pudimos cargar tus reservas. Intentá nuevamente."
    }

private fun Throwable.toReportAttributes(): Map<String, String> =
    when (this) {
      is AppApiException ->
          mapOf(
              "error_source" to "app_api",
              "status_code" to statusCode.toString(),
          )
      else -> mapOf("error_source" to "unexpected")
    }
