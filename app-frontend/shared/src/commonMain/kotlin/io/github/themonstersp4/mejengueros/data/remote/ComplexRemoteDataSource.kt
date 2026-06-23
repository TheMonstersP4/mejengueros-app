package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.ApiErrorEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestPayloadDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateCourtRequestPayloadDto
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class ComplexRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : IComplexRemoteDataSource {
  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
    try {
      val response =
          httpClient
              .post("/v1/complexes") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    CreateComplexRequestDto(
                        complex =
                            CreateComplexRequestPayloadDto(
                                name = request.complexName,
                                address = request.complexAddress,
                            ),
                        firstCourt = CreateCourtRequestPayloadDto(name = request.firstCourtName),
                    )
                )
              }
              .body<CreateComplexEnvelopeDto>()

      val data =
          response.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      return CreatedComplex(
          complexId = data.complex.id,
          complexName = data.complex.name,
          complexAddress = data.complex.address,
          firstCourtId = data.firstCourt.id,
          firstCourtName = data.firstCourt.name,
      )
    } catch (error: ClientRequestException) {
      throw error.toAppApiException(json)
    }
  }
}

class AppApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

private suspend fun ClientRequestException.toAppApiException(json: Json): AppApiException {
  val rawBody = response.bodyAsText()
  val envelope = runCatching { json.decodeFromString<ApiErrorEnvelopeDto>(rawBody) }.getOrNull()
  val message = envelope?.errors?.firstOrNull()?.message ?: response.status.description

  return AppApiException(statusCode = response.status.value, message = message)
}
