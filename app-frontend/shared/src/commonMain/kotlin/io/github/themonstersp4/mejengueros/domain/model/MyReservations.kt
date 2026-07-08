package io.github.themonstersp4.mejengueros.domain.model

data class MyReservations(
    val upcoming: List<MyReservationCard>,
    val finalized: List<MyReservationCard>,
)

data class MyReservationCard(
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
)
