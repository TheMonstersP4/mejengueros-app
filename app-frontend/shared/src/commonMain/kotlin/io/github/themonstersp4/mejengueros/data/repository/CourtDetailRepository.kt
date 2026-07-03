package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtDetailRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.time.todayUtcDateString

class CourtDetailRepository(
    private val remoteDataSource: ICourtDetailRemoteDataSource,
    private val todayDateProvider: () -> String = { todayUtcDateString() },
) : ICourtDetailRepository {
  override suspend fun getReservableSlotsForToday(courtId: String): List<ReservableSlot> =
      remoteDataSource.getReservableSlots(courtId, todayDateProvider())
}
