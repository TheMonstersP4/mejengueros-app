package io.github.themonstersp4.mejengueros.domain.model

data class ReservationDayAvailability(
    val dateUtc: String,
    val referenceDateUtc: String = dateUtc,
    val availabilityStatus: ReservationAvailabilityStatus,
    val slots: List<ReservableSlot>,
)

enum class ReservationAvailabilityStatus {
  Available,
  FullyBooked,
  Unavailable,
  Unknown,
}

internal fun String.toReservationAvailabilityStatus(): ReservationAvailabilityStatus =
    when (trim().uppercase()) {
      "AVAILABLE" -> ReservationAvailabilityStatus.Available
      "FULLY_BOOKED" -> ReservationAvailabilityStatus.FullyBooked
      "UNAVAILABLE" -> ReservationAvailabilityStatus.Unavailable
      else -> ReservationAvailabilityStatus.Unknown
    }
