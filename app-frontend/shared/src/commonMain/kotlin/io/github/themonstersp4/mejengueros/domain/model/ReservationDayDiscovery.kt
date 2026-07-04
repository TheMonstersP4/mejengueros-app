package io.github.themonstersp4.mejengueros.domain.model

internal const val DefaultReservableDaysWindow = 14

data class ReservationDayDiscovery(
    val fromUtc: String,
    val days: Int,
    val reservableDays: List<ReservableDay>,
)

data class ReservableDay(
    val dateUtc: String,
    val availabilityStatus: ReservationAvailabilityStatus,
    val availableSlotsCount: Int,
)
