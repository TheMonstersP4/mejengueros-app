package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtAvailabilityRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository

class CourtAvailabilityRepository(
    private val remoteDataSource: ICourtAvailabilityRemoteDataSource,
) : ICourtAvailabilityRepository {
  override suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext {
    return remoteDataSource.getCourtAvailability(courtId)
  }

  override suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext {
    return remoteDataSource.saveCourtAvailability(courtId, availability)
  }
}
