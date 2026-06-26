package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext

interface ICourtAvailabilityRemoteDataSource {
  suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext

  suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext
}
