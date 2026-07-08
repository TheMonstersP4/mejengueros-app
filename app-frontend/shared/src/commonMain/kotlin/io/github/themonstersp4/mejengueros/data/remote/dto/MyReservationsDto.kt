package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MyReservationsEnvelopeDto(
    val success: Boolean,
    val data: MyReservationsResponseDto?,
)

@Serializable
data class MyReservationsResponseDto(
    val upcoming: List<MyReservationCardDto> = emptyList(),
    val finalized: List<MyReservationCardDto> = emptyList(),
)

@Serializable
data class MyReservationCardDto(
    val id: String,
    val complexName: String,
    val courtName: String,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val section: String,
    val reviewStatus: String,
    val canReview: Boolean,
    val hasReview: Boolean,
    val primaryActionKey: String? = null,
    val primaryActionLabel: String? = null,
    val indicatorKey: String? = null,
    val indicatorLabel: String? = null,
)
