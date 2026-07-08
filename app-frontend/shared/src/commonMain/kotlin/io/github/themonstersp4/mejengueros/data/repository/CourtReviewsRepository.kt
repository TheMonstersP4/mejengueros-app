package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtReviewsRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtReview
import io.github.themonstersp4.mejengueros.domain.repository.ICourtReviewsRepository

class CourtReviewsRepository(
    private val remoteDataSource: ICourtReviewsRemoteDataSource,
) : ICourtReviewsRepository {
  override suspend fun getCourtReviews(courtId: String): List<CourtReview> =
      remoteDataSource.getCourtReviews(courtId)
}
