package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext

interface ICourtAvailabilityRepository {
  suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext

  suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext
}
