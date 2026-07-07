package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtReview

interface ICourtReviewsRepository {
  suspend fun getCourtReviews(courtId: String): List<CourtReview>
}
