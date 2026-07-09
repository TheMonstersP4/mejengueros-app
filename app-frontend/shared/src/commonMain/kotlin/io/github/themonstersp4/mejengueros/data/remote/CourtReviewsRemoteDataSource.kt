package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CourtReviewsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.CourtReview
import io.github.themonstersp4.mejengueros.domain.time.monthDayYearLabelOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class CourtReviewsRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ICourtReviewsRemoteDataSource {
  override suspend fun getCourtReviews(courtId: String): List<CourtReview> {
    return try {
      httpClient
          .get("/v1/courts/$courtId/reviews")
          .body<CourtReviewsEnvelopeDto>()
          .data
          .items
          .map { item ->
            CourtReview(
                id = item.reviewId,
                rating = item.rating,
                comment = item.comment?.trim()?.takeIf { it.isNotEmpty() },
                authorName = item.reviewer.displayName,
                authorInitials = item.reviewer.initials,
                dateLabel = monthDayYearLabelOrNull(item.createdAt),
            )
          }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
