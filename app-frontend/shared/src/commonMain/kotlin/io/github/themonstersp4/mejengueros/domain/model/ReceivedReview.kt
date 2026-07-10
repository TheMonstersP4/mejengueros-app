package io.github.themonstersp4.mejengueros.domain.model

data class ReceivedReviewCourt(val id: String, val name: String)

data class ReceivedReviewer(val displayName: String, val initials: String)

data class ReceivedReview(
    val reviewId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val court: ReceivedReviewCourt,
    val reviewer: ReceivedReviewer,
)

data class ReceivedReviewsSummary(
    val selectedCourtId: String?,
    val totalReviews: Int,
    val averageRating: Double?,
)

data class ReceivedReviewPage(
    val items: List<ReceivedReview>,
    val summary: ReceivedReviewsSummary,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
)

data class OwnerReceivedCourtFilter(val courtId: String, val name: String)
