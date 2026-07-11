package io.github.themonstersp4.mejengueros.domain.model

data class OwnerReservations(
    val selectedCourtId: String?,
    val upcoming: List<OwnerReservationCard>,
    val finalized: List<OwnerReservationCard>,
)

data class OwnerReservationCard(
    val id: String,
    val complexName: String,
    val courtName: String,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val section: String,
)

data class OwnerReservationCourtFilter(val courtId: String, val name: String)
