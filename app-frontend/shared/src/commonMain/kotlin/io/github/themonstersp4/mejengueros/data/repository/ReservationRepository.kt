package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IReservationRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository

class ReservationRepository(
    private val remoteDataSource: IReservationRemoteDataSource,
) : IReservationRepository {
  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): ReservationDayAvailability = remoteDataSource.getReservableSlots(courtId, dateUtc)

  override suspend fun createReservation(
      courtId: String,
      startsAtUtc: String,
  ): ReservationConfirmation = remoteDataSource.createReservation(courtId, startsAtUtc)
}
