package io.github.themonstersp4.mejengueros.presentation.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.DefaultReservableDaysWindow
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.time.parseUtcCalendarDate
import io.github.themonstersp4.mejengueros.domain.time.todayUtcDateString
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReservationViewModel(
    private val context: ReservationContext,
    private val repository: IReservationRepository,
    private val reservableDatesLoader: suspend () -> List<ReservationDateUiModel> = {
      repository
          .getReservableDays(
              courtId = context.courtId,
              fromUtcDate = todayUtcDateString(),
              days = DefaultReservableDaysWindow,
          )
          .toReservationDateUiModels()
    },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(ReservationUiState())
  private var latestSlotsRequestId = 0L
  val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

  init {
    loadReservableDates()
  }

  fun selectDate(index: Int) {
    val dates = _uiState.value.dates
    if (index !in dates.indices || index == _uiState.value.selectedDateIndex) {
      return
    }

    _uiState.value =
        _uiState.value.copy(
            selectedDateIndex = index,
            selectedSlotId = null,
            selectionSummary = null,
            selectionSupportingText = null,
            loadErrorMessage = null,
            slots = emptyList(),
            mode = ReservationUiMode.Selection,
        )
    loadSelectedDateSlots()
  }

  fun selectSlot(slotId: String) {
    val state = _uiState.value
    if (state.mode != ReservationUiMode.Selection) return

    val selectedSlot = state.slots.firstOrNull { it.id == slotId } ?: return
    if (selectedSlot.state == MejenguerosSlotState.Occupied) return

    val selectedDate = state.dates.getOrNull(state.selectedDateIndex) ?: return
    _uiState.value =
        state.copy(
            selectedSlotId = slotId,
            slots =
                state.slots.map { slot ->
                  slot.copy(
                      state =
                          when {
                            slot.id == slotId -> MejenguerosSlotState.Selected
                            slot.state == MejenguerosSlotState.Occupied ->
                                MejenguerosSlotState.Occupied
                            else -> MejenguerosSlotState.Available
                          }
                  )
                },
            selectionSummary =
                "${selectedDate.summaryLabel} · ${selectedSlot.rangeLabel} · ${context.courtName}",
            selectionSupportingText = context.complexName,
        )
  }

  fun confirmReservation() {
    val state = _uiState.value
    if (!state.canConfirm) return

    val selectedSlot = state.slots.firstOrNull { it.id == state.selectedSlotId } ?: return
    _uiState.value = state.copy(isSubmitting = true, loadErrorMessage = null)

    scope.launch {
      try {
        val confirmation =
            repository.createReservation(courtId = context.courtId, startsAtUtc = selectedSlot.id)
        _uiState.value =
            _uiState.value.copy(
                isSubmitting = false,
                mode =
                    ReservationUiMode.Success(
                        ticket =
                            ReservationTicketUiModel(
                                courtLabel = "${context.complexName} · ${context.courtName}",
                                locationLabel = context.locationLabel,
                                dateLabel = currentSelectedDateTicketLabel(),
                                timeLabel =
                                    ReservableSlot(
                                            startsAtUtc = confirmation.startsAtUtc,
                                            endsAtUtc = confirmation.endsAtUtc,
                                        )
                                        .displayRange,
                            )
                    ),
            )
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        if (error is AppApiException && error.statusCode == 409) {
          _uiState.value =
              _uiState.value.copy(
                  isSubmitting = false,
                  mode = ReservationUiMode.Conflict(selectedSlot.rangeLabel),
              )
        } else {
          _uiState.value =
              _uiState.value.copy(
                  isSubmitting = false,
                  loadErrorMessage = error.toReservationSubmitUserMessage(),
              )
        }
      }
    }
  }

  fun retryLoad() {
    _uiState.value =
        _uiState.value.copy(
            loadErrorMessage = null,
            selectedSlotId = null,
            selectionSummary = null,
            selectionSupportingText = null,
            mode = ReservationUiMode.Selection,
        )
    if (_uiState.value.dates.isEmpty()) {
      loadReservableDates()
    } else {
      loadSelectedDateSlots()
    }
  }

  fun viewOtherHours() {
    _uiState.value =
        _uiState.value.copy(
            mode = ReservationUiMode.Selection,
            selectedSlotId = null,
            selectionSummary = null,
            selectionSupportingText = null,
        )
    loadSelectedDateSlots()
  }

  private fun loadReservableDates() {
    scope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoadingSlots = true,
              isSubmitting = false,
              dates = emptyList(),
              selectedDateIndex = 0,
              slots = emptyList(),
              selectedSlotId = null,
              selectionSummary = null,
              selectionSupportingText = null,
              loadErrorMessage = null,
              mode = ReservationUiMode.Selection,
          )
      try {
        val dates = reservableDatesLoader()
        if (dates.isEmpty()) {
          _uiState.value =
              _uiState.value.copy(
                  isLoadingSlots = false,
                  dates = emptyList(),
                  selectedDateIndex = 0,
                  slots = emptyList(),
              )
          return@launch
        }

        _uiState.value =
            _uiState.value.copy(
                dates = dates,
                selectedDateIndex = 0,
            )
        loadSelectedDateSlots()
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value =
            _uiState.value.copy(
                isLoadingSlots = false,
                loadErrorMessage = error.toReservationLoadUserMessage(),
                dates = emptyList(),
                selectedDateIndex = 0,
                slots = emptyList(),
                mode = ReservationUiMode.Selection,
            )
      }
    }
  }

  private fun loadSelectedDateSlots() {
    val selectedDate = _uiState.value.dates.getOrNull(_uiState.value.selectedDateIndex) ?: return
    val selectedIndex = _uiState.value.selectedDateIndex
    val requestId = ++latestSlotsRequestId
    scope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoadingSlots = true,
              isSubmitting = false,
              loadErrorMessage = null,
              selectedSlotId = null,
              selectionSummary = null,
              selectionSupportingText = null,
              slots = emptyList(),
          )
      try {
        val availability =
            repository.getReservableSlots(courtId = context.courtId, dateUtc = selectedDate.utcDate)
        if (
            requestId != latestSlotsRequestId || _uiState.value.selectedDateIndex != selectedIndex
        ) {
          return@launch
        }
        _uiState.value =
            _uiState.value.copy(
                isLoadingSlots = false,
                slots = availability.toSlotUiModels(),
                mode = ReservationUiMode.Selection,
            )
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        if (
            requestId != latestSlotsRequestId || _uiState.value.selectedDateIndex != selectedIndex
        ) {
          return@launch
        }
        _uiState.value =
            _uiState.value.copy(
                isLoadingSlots = false,
                loadErrorMessage = error.toReservationLoadUserMessage(),
                slots = emptyList(),
                mode = ReservationUiMode.Selection,
            )
      }
    }
  }

  private fun currentSelectedDateTicketLabel(): String =
      _uiState.value.dates.getOrNull(_uiState.value.selectedDateIndex)?.ticketLabel.orEmpty()
}

private fun ReservationDayAvailability.toSlotUiModels(): List<ReservationSlotUiModel> {
  if (slots.isEmpty()) return emptyList()

  val availableSlotsByLabel = slots.associateBy(ReservableSlot::displayStartTime)
  val orderedLabels = buildOrderedHourLabels(availableSlotsByLabel.keys.toList())

  return orderedLabels.map { hourLabel ->
    val slot = availableSlotsByLabel[hourLabel]
    if (slot != null) {
      ReservationSlotUiModel(
          id = slot.startsAtUtc,
          label = slot.displayStartTime,
          rangeLabel = slot.displayRange,
          state = MejenguerosSlotState.Available,
      )
    } else {
      ReservationSlotUiModel(
          id = "occupied-$hourLabel",
          label = hourLabel,
          rangeLabel = hourLabel,
          state = MejenguerosSlotState.Occupied,
      )
    }
  }
}

private fun buildOrderedHourLabels(labels: List<String>): List<String> {
  if (labels.isEmpty()) return emptyList()
  val hourValues = labels.mapNotNull(::parseHourOfDay)
  if (hourValues.size != labels.size) return labels.sorted()

  val startHour = hourValues.minOrNull() ?: return labels.sorted()
  val endHour = hourValues.maxOrNull() ?: return labels.sorted()
  return (startHour..endHour).map(::formatHourLabel)
}

private fun parseHourOfDay(label: String): Int? = label.substringBefore(':').toIntOrNull()

private fun formatHourLabel(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"

private fun Throwable.toReservationLoadUserMessage(): String =
    when (this) {
      is AppApiException ->
          when {
            statusCode == 401 || statusCode == 403 ->
                "Tu sesión no permite consultar horarios en este momento."
            statusCode == 404 -> "No encontramos la cancha que intentás reservar."
            else -> "No pudimos cargar los horarios disponibles. Intentá nuevamente."
          }
      else -> "No pudimos cargar los horarios disponibles. Intentá nuevamente."
    }

private fun Throwable.toReservationSubmitUserMessage(): String =
    when (this) {
      is AppApiException ->
          when {
            statusCode == 401 || statusCode == 403 ->
                "Tu sesión no permite confirmar reservas en este momento."
            statusCode in 400..499 ->
                "No pudimos confirmar la reserva con ese horario. Revisá la selección e intentá de nuevo."
            else -> "No pudimos confirmar la reserva en este momento. Intentá nuevamente."
          }
      else -> "No pudimos confirmar la reserva en este momento. Intentá nuevamente."
    }

internal fun buildReservationDateOptions(
    startDateUtc: String,
    days: Int = 6,
): List<ReservationDateUiModel> {
  val startDate = parseUtcCalendarDate(startDateUtc)
  return (0 until days).map { offset ->
    val currentDate = startDate.plusDays(offset)
    currentDate.toReservationDateUiModel(referenceDateUtc = startDateUtc)
  }
}

private fun ReservationDayDiscovery.toReservationDateUiModels(): List<ReservationDateUiModel> =
    reservableDays.map { day ->
      parseUtcCalendarDate(day.dateUtc).toReservationDateUiModel(referenceDateUtc = fromUtc)
    }

private fun io.github.themonstersp4.mejengueros.domain.time.UtcCalendarDate
    .toReservationDateUiModel(referenceDateUtc: String): ReservationDateUiModel {
  val utcDate = toIsoDate()
  val shortWeekday = shortWeekdayLabel()
  val isReferenceDate = utcDate == referenceDateUtc
  return ReservationDateUiModel(
      utcDate = utcDate,
      dayLabel = if (isReferenceDate) "Hoy" else shortWeekday,
      dateLabel = day.toString().padStart(2, '0'),
      summaryLabel =
          if (isReferenceDate) {
            "Hoy ${day.toString().padStart(2, '0')}"
          } else {
            "$shortWeekday ${day.toString().padStart(2, '0')}"
          },
      ticketLabel =
          if (isReferenceDate) {
            "Hoy, $day de ${monthName()}"
          } else {
            "$shortWeekday, $day de ${monthName()}"
          },
  )
}
