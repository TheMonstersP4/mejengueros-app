package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReservationEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReservationRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.MyReservationCardDto
import io.github.themonstersp4.mejengueros.data.remote.dto.MyReservationsEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.OwnerReservationCardDto
import io.github.themonstersp4.mejengueros.data.remote.dto.OwnerReservationsEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ReservableDaysEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ReservableSlotsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.MyReservationCard
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCard
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservableDay
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.model.toReservationAvailabilityStatus
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

class ReservationRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : IReservationRemoteDataSource {
  override suspend fun getReservableDays(
      courtId: String,
      fromUtcDate: String,
      days: Int,
  ): ReservationDayDiscovery {
    return try {
      val data =
          httpClient
              .get("/v1/courts/$courtId/reservable-days") {
                parameter("from", fromUtcDate)
                parameter("days", days)
              }
              .body<ReservableDaysEnvelopeDto>()
              .data

      ReservationDayDiscovery(
          fromUtc = data?.from ?: fromUtcDate,
          days = data?.days ?: days,
          reservableDays =
              data?.reservableDays?.map { day ->
                ReservableDay(
                    dateUtc = day.date,
                    availabilityStatus = day.availabilityStatus.toReservationAvailabilityStatus(),
                    availableSlotsCount = day.availableSlotsCount,
                )
              } ?: emptyList(),
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): ReservationDayAvailability {
    return try {
      val data =
          httpClient
              .get("/v1/courts/$courtId/reservable-slots") { parameter("date", dateUtc) }
              .body<ReservableSlotsEnvelopeDto>()
              .data

      ReservationDayAvailability(
          dateUtc = data?.date ?: dateUtc,
          availabilityStatus =
              data?.availabilityStatus?.toReservationAvailabilityStatus()
                  ?: io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
                      .Unknown,
          slots =
              data?.slots?.map { slot ->
                ReservableSlot(startsAtUtc = slot.startsAt, endsAtUtc = slot.endsAt)
              } ?: emptyList(),
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun createReservation(
      courtId: String,
      startsAtUtc: String,
  ): ReservationConfirmation {
    return try {
      val data =
          httpClient
              .post("/v1/reservations") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(CreateReservationRequestDto(courtId = courtId, startsAt = startsAtUtc))
              }
              .body<CreateReservationEnvelopeDto>()
              .data ?: error("Reservation response did not include data.")

      ReservationConfirmation(
          id = data.id,
          courtId = data.courtId,
          startsAtUtc = data.startsAt,
          endsAtUtc = data.endsAt,
          status = data.status,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getMyReservations(): MyReservations {
    return try {
      val data =
          httpClient.get("/v1/reservations/my").body<MyReservationsEnvelopeDto>().data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      MyReservations(
          upcoming = data.upcoming.map(MyReservationCardDto::toDomain),
          finalized = data.finalized.map(MyReservationCardDto::toDomain),
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getOwnerReservations(courtId: String?): OwnerReservations {
    return try {
      val data =
          httpClient
              .get("/v1/owners/me/reservations") {
                if (courtId != null) parameter("courtId", courtId)
              }
              .body<OwnerReservationsEnvelopeDto>()
              .data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      OwnerReservations(
          selectedCourtId = data.selectedCourtId,
          upcoming = data.upcoming.map(OwnerReservationCardDto::toDomain),
          finalized = data.finalized.map(OwnerReservationCardDto::toDomain),
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}

private fun OwnerReservationCardDto.toDomain(): OwnerReservationCard =
    OwnerReservationCard(
        id = id,
        complexName = complexName,
        courtName = courtName,
        imageUrl = imageUrl,
        startsAt = startsAt,
        endsAt = endsAt,
        status = status,
        section = section,
    )

private fun MyReservationCardDto.toDomain(): MyReservationCard =
    MyReservationCard(
        id = id,
        complexName = complexName,
        courtName = courtName,
        imageUrl = imageUrl,
        startsAt = startsAt,
        endsAt = endsAt,
        status = status,
        section = section,
        reviewStatus = reviewStatus,
        canReview = canReview,
        hasReview = hasReview,
        primaryActionKey = primaryActionKey,
        primaryActionLabel = primaryActionLabel,
        indicatorKey = indicatorKey,
        indicatorLabel = indicatorLabel,
    )
