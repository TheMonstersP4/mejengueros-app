package io.github.themonstersp4.mejengueros.presentation.courtdetail

import io.github.themonstersp4.mejengueros.domain.model.CourtReview

data class CourtDetailUiState(
    val isLoadingSlots: Boolean = true,
    val slots: List<CourtDetailSlot> = emptyList(),
    val slotsErrorMessage: String? = null,
    val availabilityHeadline: String? = null,
    val isLoadingReviews: Boolean = true,
    val reviews: List<CourtReview> = emptyList(),
    val reviewsErrorMessage: String? = null,
)

data class CourtDetailSlot(
    val displayTime: String,
)
