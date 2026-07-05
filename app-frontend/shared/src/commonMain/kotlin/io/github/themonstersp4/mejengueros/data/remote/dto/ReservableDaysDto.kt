package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReservableDaysEnvelopeDto(
    val success: Boolean,
    val data: ReservableDaysDataDto? = null,
)

@Serializable
data class ReservableDaysDataDto(
    val court: ReservableDaysCourtDto,
    val from: String,
    val days: Int,
    val reservableDays: List<ReservableDayDto> = emptyList(),
)

@Serializable
data class ReservableDaysCourtDto(
    val id: String,
    val name: String,
    val status: String,
)

@Serializable
data class ReservableDayDto(
    val date: String,
    val availabilityStatus: String,
    val availableSlotsCount: Int,
)
