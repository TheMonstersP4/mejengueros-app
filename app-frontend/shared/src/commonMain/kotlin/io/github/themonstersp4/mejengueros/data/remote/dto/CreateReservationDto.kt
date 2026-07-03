package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReservationRequestDto(
    val courtId: String,
    val startsAt: String,
)

@Serializable
data class CreateReservationEnvelopeDto(
    val success: Boolean,
    val data: CreateReservationDataDto? = null,
)

@Serializable
data class CreateReservationDataDto(
    val id: String,
    val courtId: String,
    val startsAt: String,
    val endsAt: String,
    val status: String,
)
