package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CourtReviewsEnvelopeDto(
    val success: Boolean,
    val data: CourtReviewsDataDto = CourtReviewsDataDto(),
)

@Serializable
data class CourtReviewsDataDto(
    val summary: CourtReviewsSummaryDto = CourtReviewsSummaryDto(),
    val items: List<CourtReviewItemDto> = emptyList(),
)

@Serializable
data class CourtReviewsSummaryDto(
    val totalReviews: Int = 0,
    val averageRating: Double? = null,
)

@Serializable
data class CourtReviewItemDto(
    val reviewId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String? = null,
    val reviewer: CourtReviewerDto = CourtReviewerDto(),
)

@Serializable
data class CourtReviewerDto(
    val displayName: String = "",
    val initials: String = "",
)
