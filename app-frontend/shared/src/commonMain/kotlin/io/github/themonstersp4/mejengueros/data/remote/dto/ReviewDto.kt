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

@Serializable
data class OwnerReceivedReviewsEnvelopeDto(
    val success: Boolean,
    val data: OwnerReceivedReviewsDataDto? = null,
    val meta: OwnerReceivedReviewsMetaDto? = null,
)

@Serializable
data class OwnerReceivedReviewsDataDto(
    val summary: OwnerReceivedReviewSummaryDto = OwnerReceivedReviewSummaryDto(),
    val items: List<OwnerReceivedReviewItemDto> = emptyList(),
)

@Serializable
data class OwnerReceivedReviewSummaryDto(
    val selectedCourtId: String? = null,
    val totalReviews: Int = 0,
    val averageRating: Double? = null,
)

@Serializable
data class OwnerReceivedReviewItemDto(
    val reviewId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String,
    val court: OwnerReceivedReviewCourtDto,
    val reviewer: OwnerReceivedReviewerDto,
)

@Serializable
data class OwnerReceivedReviewCourtDto(
    val id: String,
    val name: String,
)

@Serializable
data class OwnerReceivedReviewerDto(
    val displayName: String,
    val initials: String,
)

@Serializable
data class OwnerReceivedReviewsMetaDto(
    val pagination: OwnerReceivedReviewsPaginationDto? = null,
)

@Serializable
data class OwnerReceivedReviewsPaginationDto(
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalItems: Int = 0,
    val totalPages: Int = 1,
)
