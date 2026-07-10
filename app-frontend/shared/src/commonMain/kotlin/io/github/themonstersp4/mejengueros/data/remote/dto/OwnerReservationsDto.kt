package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OwnerReservationsEnvelopeDto(
    val success: Boolean,
    val data: OwnerReservationsResponseDto?,
)

@Serializable
data class OwnerReservationsResponseDto(
    val selectedCourtId: String? = null,
    val upcoming: List<OwnerReservationCardDto> = emptyList(),
    val finalized: List<OwnerReservationCardDto> = emptyList(),
)

@Serializable
data class OwnerReservationCardDto(
    val id: String,
    val complexName: String,
    val courtName: String,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val section: String,
)
