package io.github.themonstersp4.mejengueros.presentation.reservation

import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotState

data class ReservationUiState(
    val isLoadingSlots: Boolean = true,
    val isSubmitting: Boolean = false,
    val dates: List<ReservationDateUiModel> = emptyList(),
    val selectedDateIndex: Int = 0,
    val slots: List<ReservationSlotUiModel> = emptyList(),
    val selectedSlotId: String? = null,
    val selectionSummary: String? = null,
    val selectionSupportingText: String? = null,
    val loadErrorMessage: String? = null,
    val mode: ReservationUiMode = ReservationUiMode.Selection,
) {
  val canConfirm: Boolean
    get() =
        selectedSlotId != null &&
            !isLoadingSlots &&
            !isSubmitting &&
            mode == ReservationUiMode.Selection
}

data class ReservationDateUiModel(
    val utcDate: String,
    val dayLabel: String,
    val dateLabel: String,
    val summaryLabel: String,
    val ticketLabel: String,
)

data class ReservationSlotUiModel(
    val id: String,
    val label: String,
    val rangeLabel: String,
    val state: MejenguerosSlotState,
)

data class ReservationTicketUiModel(
    val courtLabel: String,
    val locationLabel: String,
    val dateLabel: String,
    val timeLabel: String,
)

sealed interface ReservationUiMode {
  data object Selection : ReservationUiMode

  data class Success(val ticket: ReservationTicketUiModel) : ReservationUiMode

  data class Conflict(val attemptedTimeLabel: String) : ReservationUiMode
}
