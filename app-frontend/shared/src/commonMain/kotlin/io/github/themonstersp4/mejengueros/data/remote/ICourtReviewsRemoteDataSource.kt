package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CourtReview

interface ICourtReviewsRemoteDataSource {
  suspend fun getCourtReviews(courtId: String): List<CourtReview>
}
