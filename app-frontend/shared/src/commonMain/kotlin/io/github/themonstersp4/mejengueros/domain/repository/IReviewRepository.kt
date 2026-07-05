package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation

interface IReviewRepository {
  suspend fun getLatestReviewableReservation(): ReviewableReservation?

  suspend fun createReview(request: CreateReviewRequest): CreatedReview
}
