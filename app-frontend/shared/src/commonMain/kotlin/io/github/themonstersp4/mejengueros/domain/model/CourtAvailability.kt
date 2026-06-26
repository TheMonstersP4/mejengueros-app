package io.github.themonstersp4.mejengueros.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CourtAvailabilityWeekday {
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY,
  SUNDAY,
}

data class CourtAvailabilityConfig(
    val days: List<CourtAvailabilityWeekday>,
    val startTime: String,
    val endTime: String,
)

data class CourtAvailabilityContext(
    val courtId: String,
    val courtName: String,
    val complexName: String,
    val availability: CourtAvailabilityConfig?,
)
