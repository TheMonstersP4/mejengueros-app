package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation

interface IReviewRemoteDataSource {
  suspend fun getLatestReviewableReservation(): ReviewableReservation?

  suspend fun createReview(request: CreateReviewRequest): CreatedReview

  suspend fun getOwnerReceivedReviews(
      courtId: String?,
      page: Int,
      pageSize: Int,
  ): ReceivedReviewPage
}
