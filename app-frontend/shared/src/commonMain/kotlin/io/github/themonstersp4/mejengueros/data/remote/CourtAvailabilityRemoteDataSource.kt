package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CourtAvailabilityEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.SaveCourtAvailabilityRequestDto
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class CourtAvailabilityRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ICourtAvailabilityRemoteDataSource {
  override suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext {
    return try {
      val response =
          httpClient.get("/v1/courts/$courtId/availability").body<CourtAvailabilityEnvelopeDto>()

      response.data?.toDomain()
          ?: throw AppApiException(
              statusCode = 502,
              message = "No se recibió la respuesta esperada del API.",
          )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext {
    return try {
      val response =
          httpClient
              .put("/v1/courts/$courtId/availability") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    SaveCourtAvailabilityRequestDto(
                        days = availability.days,
                        startTime = availability.startTime,
                        endTime = availability.endTime,
                    )
                )
              }
              .body<CourtAvailabilityEnvelopeDto>()

      response.data?.toDomain()
          ?: throw AppApiException(
              statusCode = 502,
              message = "No se recibió la respuesta esperada del API.",
          )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}

private fun io.github.themonstersp4.mejengueros.data.remote.dto.CourtAvailabilityResponseDto
    .toDomain(): CourtAvailabilityContext =
    CourtAvailabilityContext(
        courtId = court.id,
        courtName = court.name,
        complexName = court.complexName,
        availability =
            availability?.let {
              CourtAvailabilityConfig(
                  days = it.days,
                  startTime = it.startTime,
                  endTime = it.endTime,
              )
            },
    )
