package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IReviewRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository

class ReviewRepository(
    private val remoteDataSource: IReviewRemoteDataSource,
) : IReviewRepository {
  override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
    return remoteDataSource.getLatestReviewableReservation()
  }

  override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
    return remoteDataSource.createReview(request)
  }
}
