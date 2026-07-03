package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.ApiErrorEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CantonCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestPayloadDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateCourtEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateCourtRequestPayloadDto
import io.github.themonstersp4.mejengueros.data.remote.dto.MyComplexHubEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ProvinceCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ServiceCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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
  override suspend fun getProvinces(): List<Province> {
    return try {
      httpClient.get("/v1/locations/provinces").body<ProvinceCatalogEnvelopeDto>().data.map {
          province ->
        Province(id = province.id, code = province.code, name = province.name)
      }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getCantons(provinceId: String): List<Canton> {
    return try {
      httpClient
          .get("/v1/locations/provinces/$provinceId/cantons")
          .body<CantonCatalogEnvelopeDto>()
          .data
          .map { canton ->
            Canton(
                id = canton.id,
                provinceId = canton.provinceId,
                code = canton.code,
                name = canton.name,
            )
          }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> {
    return try {
      httpClient
          .get("/v1/services") { parameter("scope", scope.name) }
          .body<ServiceCatalogEnvelopeDto>()
          .data
          .map { service ->
            ServiceCatalogItem(id = service.id, name = service.name, scope = service.scope)
          }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

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
                                name = request.complex.name,
                                provinceId = request.complex.provinceId,
                                cantonId = request.complex.cantonId,
                                address = request.complex.address,
                                latitude = request.complex.latitude,
                                longitude = request.complex.longitude,
                                serviceIds = request.complex.serviceIds,
                            ),
                        firstCourt =
                            CreateCourtRequestPayloadDto(
                                name = request.firstCourt.name,
                                serviceIds = request.firstCourt.serviceIds,
                                imageUploadId = request.firstCourt.imageUploadId,
                            ),
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
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt {
    try {
      val response =
          httpClient
              .post("/v1/complexes/$complexId/courts") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    CreateCourtRequestPayloadDto(
                        name = request.name,
                        serviceIds = request.serviceIds,
                        imageUploadId = request.imageUploadId,
                    )
                )
              }
              .body<CreateCourtEnvelopeDto>()

      val data =
          response.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      return CreatedCourt(
          id = data.court.id,
          complexId = data.court.complexId,
          name = data.court.name,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getMyComplexHub(): MyComplexHub {
    return try {
      val response = httpClient.get("/v1/complexes/my-hub").body<MyComplexHubEnvelopeDto>()
      val data =
          response.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      MyComplexHub(
          complexes =
              data.complexes.map { complex ->
                MyComplexHubComplex(
                    id = complex.id,
                    name = complex.name,
                    address = complex.address,
                    provinceId = complex.provinceId,
                    cantonId = complex.cantonId,
                    latitude = complex.latitude,
                    longitude = complex.longitude,
                    status = complex.status,
                    courts =
                        complex.courts.map { court ->
                          MyComplexHubCourt(
                              id = court.id,
                              name = court.name,
                              status = court.status,
                              availabilityStatus = court.availabilityStatus,
                          )
                        },
                )
              }
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}

class AppApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

internal suspend fun ResponseException.toAppApiException(json: Json): AppApiException {
  val rawBody = response.bodyAsText()
  val envelope = runCatching { json.decodeFromString<ApiErrorEnvelopeDto>(rawBody) }.getOrNull()
  val message = envelope?.errors?.firstOrNull()?.message ?: response.status.description

  return AppApiException(statusCode = response.status.value, message = message)
}
