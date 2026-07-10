package io.github.themonstersp4.mejengueros.presentation.ownerreservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCard
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCourtFilter
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
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

class OwnerReservationsViewModel(
    private val reservationRepository: IReservationRepository,
    private val complexRepository: IComplexRepository,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(OwnerReservationsUiState())
  private var loadJob: Job? = null
  private var courtsJob: Job? = null
  private var loadGeneration: Long = 0
  val uiState: StateFlow<OwnerReservationsUiState> = _uiState.asStateFlow()

  init {
    loadCourts()
    load()
  }

  fun selectCourt(courtId: String?) {
    val normalized = courtId?.takeIf { it.isNotBlank() }
    if (normalized == _uiState.value.selectedCourtId) return

    _uiState.value =
        _uiState.value.copy(
            selectedCourtId = normalized,
            upcoming = emptyList(),
            finalized = emptyList(),
            isLoading = true,
            loadErrorMessage = null,
        )
    load()
  }

  fun refresh() {
    _uiState.value = _uiState.value.copy(isLoading = true, loadErrorMessage = null)
    loadCourts()
    load()
  }

  private fun load() {
    val courtId = _uiState.value.selectedCourtId
    val generation = ++loadGeneration
    loadJob?.cancel()
    loadJob =
        scope.launch {
          runCatching { reservationRepository.getOwnerReservations(courtId) }
              .onSuccess { reservations ->
                if (generation != loadGeneration) return@onSuccess
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        loadErrorMessage = null,
                        selectedCourtId = reservations.selectedCourtId,
                        upcoming = reservations.upcoming.map(OwnerReservationCard::toUiModel),
                        finalized = reservations.finalized.map(OwnerReservationCard::toUiModel),
                    )
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (generation != loadGeneration) return@onFailure

                errorReporter.reportRecoverableFailure(
                    name = "owner_reservations_load_failed",
                    attributes = error.toReportAttributes(),
                )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        upcoming = emptyList(),
                        finalized = emptyList(),
                        loadErrorMessage = error.toUserMessage(),
                    )
              }
        }
  }

  private fun loadCourts() {
    courtsJob?.cancel()
    courtsJob =
        scope.launch {
          // Courts feed the filter dropdown; a failure here degrades gracefully to
          // the "Todas las canchas" option while the reservations list still loads.
          runCatching { complexRepository.getMyComplexHub() }
              .onSuccess { hub ->
                val courts = hub.ownedCourts()
                val stillSelectable = courts.any { it.courtId == _uiState.value.selectedCourtId }
                _uiState.value =
                    _uiState.value.copy(
                        availableCourts = courts,
                        selectedCourtId =
                            if (stillSelectable) _uiState.value.selectedCourtId else null,
                    )
              }
              .onFailure { error -> if (error is CancellationException) return@onFailure }
        }
  }
}

private fun MyComplexHub.ownedCourts(): List<OwnerReservationCourtFilter> =
    complexes
        .flatMap { complex -> complex.courts }
        .map { court -> OwnerReservationCourtFilter(courtId = court.id, name = court.name) }
        .distinctBy { it.courtId }
        .sortedBy { it.name.lowercase() }

private fun OwnerReservationCard.toUiModel(): OwnerReservationCardUiModel =
    OwnerReservationCardUiModel(
        id = id,
        complexName = complexName,
        courtName = courtName,
        imageUrl = imageUrl,
        startsAt = startsAt,
        endsAt = endsAt,
        status = status,
        section = section,
        title = "$complexName · $courtName",
        reservationLabel =
            "Reserva del ${startsAt.toCostaRicaDateLabel()} · ${startsAt.toCostaRicaTimeLabel()} – ${endsAt.toCostaRicaTimeLabel()}",
    )

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "Necesitás volver a iniciar sesión para ver las reservas de tus canchas."
            else -> "No pudimos cargar las reservas de tus canchas. Intentá nuevamente."
          }
      else -> "No pudimos cargar las reservas de tus canchas. Intentá nuevamente."
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
