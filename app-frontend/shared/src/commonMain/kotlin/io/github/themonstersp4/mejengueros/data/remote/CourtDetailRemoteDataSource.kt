package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.ReservableSlotsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json

class CourtDetailRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ICourtDetailRemoteDataSource {
  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): List<ReservableSlot> {
    return try {
      httpClient
          .get("/v1/courts/$courtId/reservable-slots") { parameter("date", dateUtc) }
          .body<ReservableSlotsEnvelopeDto>()
          .data
          ?.slots
          ?.map { ReservableSlot(startsAtUtc = it.startsAt, endsAtUtc = it.endsAt) }
          ?: emptyList()
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
