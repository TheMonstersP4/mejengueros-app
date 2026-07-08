package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReviewEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReviewRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.LatestReviewableReservationEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.OwnerReceivedReviewItemDto
import io.github.themonstersp4.mejengueros.data.remote.dto.OwnerReceivedReviewSummaryDto
import io.github.themonstersp4.mejengueros.data.remote.dto.OwnerReceivedReviewsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewCourt
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewer
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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

  override suspend fun getOwnerReceivedReviews(
      courtId: String?,
      page: Int,
      pageSize: Int,
  ): ReceivedReviewPage {
    return try {
      val response =
          httpClient
              .get("/v1/owners/me/reviews") {
                courtId?.takeIf { it.isNotBlank() }?.let { parameter("courtId", it) }
                parameter("page", page)
                parameter("pageSize", pageSize)
              }
              .body<OwnerReceivedReviewsEnvelopeDto>()

      val data = response.data
      val pagination = response.meta?.pagination
      val items = data?.items.orEmpty().map { it.toDomain() }
      val summary = (data?.summary ?: OwnerReceivedReviewSummaryDto()).toDomain()
      val resolvedPage = pagination?.page ?: page
      val resolvedPageSize = pagination?.pageSize ?: pageSize
      val resolvedTotalItems = pagination?.totalItems ?: items.size
      val resolvedTotalPages = pagination?.totalPages ?: 0
      val resolvedHasNextPage = resolvedPage < resolvedTotalPages

      ReceivedReviewPage(
          items = items,
          summary = summary,
          page = resolvedPage,
          pageSize = resolvedPageSize,
          totalItems = resolvedTotalItems,
          totalPages = resolvedTotalPages,
          hasNextPage = resolvedHasNextPage,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}

private fun OwnerReceivedReviewItemDto.toDomain(): ReceivedReview =
    ReceivedReview(
        reviewId = reviewId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
        court = ReceivedReviewCourt(id = court.id, name = court.name),
        reviewer =
            ReceivedReviewer(
                displayName = reviewer.displayName,
                initials = reviewer.initials,
            ),
    )

private fun OwnerReceivedReviewSummaryDto.toDomain(): ReceivedReviewsSummary =
    ReceivedReviewsSummary(
        selectedCourtId = selectedCourtId,
        totalReviews = totalReviews,
        averageRating = averageRating,
    )
