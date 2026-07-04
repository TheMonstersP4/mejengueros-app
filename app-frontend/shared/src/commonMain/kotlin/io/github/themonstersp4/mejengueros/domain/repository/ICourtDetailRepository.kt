package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability

interface ICourtDetailRepository {
  suspend fun getUpcomingReservableSlotsPreview(courtId: String): ReservationDayAvailability?
}
