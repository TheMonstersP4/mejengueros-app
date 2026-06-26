package io.github.themonstersp4.mejengueros.data.remote.dto

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import kotlinx.serialization.Serializable

@Serializable
data class CourtAvailabilityEnvelopeDto(
    val success: Boolean,
    val data: CourtAvailabilityResponseDto? = null,
)

@Serializable
data class CourtAvailabilityResponseDto(
    val court: CourtAvailabilityCourtDto,
    val availability: CourtAvailabilityConfigDto? = null,
)

@Serializable
data class CourtAvailabilityCourtDto(
    val id: String,
    val name: String,
    val complexId: String,
    val complexName: String,
)

@Serializable
data class CourtAvailabilityConfigDto(
    val days: List<CourtAvailabilityWeekday>,
    val startTime: String,
    val endTime: String,
)

@Serializable
data class SaveCourtAvailabilityRequestDto(
    val days: List<CourtAvailabilityWeekday>,
    val startTime: String,
    val endTime: String,
)
