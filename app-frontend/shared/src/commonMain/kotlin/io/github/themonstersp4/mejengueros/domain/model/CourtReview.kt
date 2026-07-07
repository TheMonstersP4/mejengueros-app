package io.github.themonstersp4.mejengueros.domain.model

/**
 * A single published review shown on the public court detail. The author is already anonymized by
 * the backend to a safe display name (e.g. "Diego R.").
 */
data class CourtReview(
    val id: String,
    val rating: Int,
    val comment: String?,
    val authorName: String,
    val authorInitials: String,
    val dateLabel: String?,
)
