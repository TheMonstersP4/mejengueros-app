package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot

interface ICourtDetailRepository {
  suspend fun getReservableSlotsForToday(courtId: String): List<ReservableSlot>
}
