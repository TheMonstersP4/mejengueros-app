package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReservationEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateReservationRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ReservableSlotsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
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
}
