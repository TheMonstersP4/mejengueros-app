package io.github.themonstersp4.mejengueros.presentation.myreservations

data class MyReservationsUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val upcoming: List<MyReservationCardUiModel> = emptyList(),
    val finalized: List<MyReservationCardUiModel> = emptyList(),
) {
  val isEmpty: Boolean
    get() = !isLoading && loadErrorMessage == null && upcoming.isEmpty() && finalized.isEmpty()
}

data class MyReservationCardUiModel(
    val id: String,
    val complexName: String,
    val courtName: String,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val section: String,
    val reviewStatus: String,
    val canReview: Boolean,
    val hasReview: Boolean,
    val primaryActionKey: String? = null,
    val primaryActionLabel: String? = null,
    val indicatorKey: String? = null,
    val indicatorLabel: String? = null,
    val title: String,
    val reservationLabel: String,
)
