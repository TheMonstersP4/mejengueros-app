package io.github.themonstersp4.mejengueros.presentation.ownerreservations

import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCourtFilter

data class OwnerReservationsUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val availableCourts: List<OwnerReservationCourtFilter> = emptyList(),
    val selectedCourtId: String? = null,
    val upcoming: List<OwnerReservationCardUiModel> = emptyList(),
    val finalized: List<OwnerReservationCardUiModel> = emptyList(),
) {
  val isEmpty: Boolean
    get() = !isLoading && loadErrorMessage == null && upcoming.isEmpty() && finalized.isEmpty()
}

data class OwnerReservationCardUiModel(
    val id: String,
    val complexName: String,
    val courtName: String,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val section: String,
    val title: String,
    val reservationLabel: String,
)
