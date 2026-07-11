package io.github.themonstersp4.mejengueros.presentation.ownerreviews

import io.github.themonstersp4.mejengueros.domain.model.OwnerReceivedCourtFilter
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary

data class OwnerReceivedReviewsUiState(
    val availableCourts: List<OwnerReceivedCourtFilter> = emptyList(),
    val selectedCourtId: String? = null,
    val items: List<ReceivedReview> = emptyList(),
    val summary: ReceivedReviewsSummary =
        ReceivedReviewsSummary(
            selectedCourtId = null,
            totalReviews = 0,
            averageRating = null,
        ),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadErrorMessage: String? = null,
    val loadMoreErrorMessage: String? = null,
    val page: Int = 1,
    val pageSize: Int = DefaultPageSize,
    val totalPages: Int = 0,
    val hasNextPage: Boolean = false,
) {
  val isEmpty: Boolean
    get() = !isLoading && loadErrorMessage == null && items.isEmpty()

  val canLoadMore: Boolean
    get() =
        hasNextPage && !isLoading && !isLoadingMore && !isRefreshing && loadMoreErrorMessage == null
}

internal const val OwnerReceivedReviewsDefaultPageSize: Int = 10

internal val OwnerReceivedReviewsUiState.effectivePageSize: Int
  get() = if (pageSize > 0) pageSize else OwnerReceivedReviewsDefaultPageSize

internal const val DefaultPageSize: Int = OwnerReceivedReviewsDefaultPageSize
