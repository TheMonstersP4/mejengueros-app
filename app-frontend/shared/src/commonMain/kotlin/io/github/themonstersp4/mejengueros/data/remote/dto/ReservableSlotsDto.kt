package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReservableSlotsEnvelopeDto(
    val success: Boolean,
    val data: ReservableSlotsDataDto? = null,
)

@Serializable
data class ReservableSlotsDataDto(
    val court: ReservableSlotsCourtDto,
    val date: String,
    val availabilityStatus: String,
    val slots: List<ReservableSlotDto> = emptyList(),
)

@Serializable
data class ReservableSlotsCourtDto(
    val id: String,
    val name: String,
    val status: String,
)

@Serializable
data class ReservableSlotDto(
    val startsAt: String,
    val endsAt: String,
)
