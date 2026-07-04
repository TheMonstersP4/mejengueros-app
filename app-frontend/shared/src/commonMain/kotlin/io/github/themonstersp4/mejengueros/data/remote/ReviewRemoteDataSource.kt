package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReviewEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReviewRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.LatestReviewableReservationEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class ReviewRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : IReviewRemoteDataSource {
  override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
    return try {
      httpClient
          .get("/v1/reviews/latest-eligible-reservation")
          .body<LatestReviewableReservationEnvelopeDto>()
          .data
          ?.let { data ->
            ReviewableReservation(
                reservationId = data.reservationId,
                complexName = data.complexName,
                courtName = data.courtName,
                startsAt = data.startsAt,
                endsAt = data.endsAt,
                imageUrl = data.imageUrl,
            )
          }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
    return try {
      val data =
          httpClient
              .post("/v1/reviews") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    CreateReviewRequestDto(
                        reservationId = request.reservationId,
                        rating = request.rating,
                        comment = request.comment,
                        evidenceImageUploadId = request.evidenceImageUploadId,
                    )
                )
              }
              .body<CreateReviewEnvelopeDto>()
              .data ?: error("Review response did not include data.")

      CreatedReview(
          id = data.id,
          reservationId = data.reservationId,
          rating = data.rating,
          comment = data.comment,
          evidenceImageUploadId = data.evidenceImageUploadId,
          createdAt = data.createdAt,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
