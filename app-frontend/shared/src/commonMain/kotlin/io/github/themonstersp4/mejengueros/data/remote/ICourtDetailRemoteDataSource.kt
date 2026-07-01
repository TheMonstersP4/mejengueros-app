package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot

interface ICourtDetailRemoteDataSource {
  suspend fun getReservableSlots(courtId: String, dateUtc: String): List<ReservableSlot>
}
