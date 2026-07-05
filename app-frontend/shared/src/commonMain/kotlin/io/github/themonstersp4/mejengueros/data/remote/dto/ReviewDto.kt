package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LatestReviewableReservationEnvelopeDto(
    val success: Boolean,
    val data: LatestReviewableReservationDto?,
)

@Serializable
data class LatestReviewableReservationDto(
    val reservationId: String,
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
    val imageUrl: String? = null,
)

@Serializable
data class CreateReviewRequestDto(
    val reservationId: String,
    val rating: Int,
    val comment: String? = null,
    val evidenceImageUploadId: String? = null,
)

@Serializable
data class CreateReviewEnvelopeDto(
    val success: Boolean,
    val data: CreateReviewResponseDto?,
)

@Serializable
data class CreateReviewResponseDto(
    val id: String,
    val reservationId: String,
    val rating: Int,
    val comment: String? = null,
    val evidenceImageUploadId: String? = null,
    val createdAt: String,
)
