package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability

interface IReservationRemoteDataSource {
  suspend fun getReservableSlots(courtId: String, dateUtc: String): ReservationDayAvailability

  suspend fun createReservation(courtId: String, startsAtUtc: String): ReservationConfirmation
}
