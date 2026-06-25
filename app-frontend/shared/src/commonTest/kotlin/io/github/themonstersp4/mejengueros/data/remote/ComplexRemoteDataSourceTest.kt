package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestDto
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexDetails
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateFirstCourtDetails
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ComplexRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getProvincesFetchesCatalogAndMapsItems() = runTest {
    var requestedPath = ""
    var requestedMethod = HttpMethod.Get
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": [
                            { "id": "province-id", "code": "SJ", "name": "San José" }
                          ]
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureMethod = { requestedMethod = it ?: HttpMethod.Get },
                ),
            json = json,
        )

    val provinces = dataSource.getProvinces()

    assertEquals(HttpMethod.Get, requestedMethod)
    assertEquals("/v1/locations/provinces", requestedPath)
    assertEquals("province-id", provinces.single().id)
    assertEquals("San José", provinces.single().name)
  }

  @Test
  fun getCantonsFetchesProvinceCatalogAndMapsItems() = runTest {
    var requestedPath = ""
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": [
                            {
                              "id": "canton-id",
                              "provinceId": "province-id",
                              "code": "SJ-ESC",
                              "name": "Escazú"
                            }
                          ]
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                ),
            json = json,
        )

    val cantons = dataSource.getCantons("province-id")

    assertEquals("/v1/locations/provinces/province-id/cantons", requestedPath)
    assertEquals("province-id", cantons.single().provinceId)
    assertEquals("Escazú", cantons.single().name)
  }

  @Test
  fun getServicesFetchesScopeQueryAndMapsItems() = runTest {
    var requestedPath = ""
    var requestedScope = ""
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": [
                            { "id": "service-id", "name": "Lighting", "scope": "COURT" }
                          ]
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureScope = { requestedScope = it.orEmpty() },
                ),
            json = json,
        )

    val services = dataSource.getServices(ServiceScope.COURT)

    assertEquals("/v1/services", requestedPath)
    assertEquals("COURT", requestedScope)
    assertEquals(ServiceScope.COURT, services.single().scope)
  }

  @Test
  fun createComplexPostsExpandedPayloadAndMapsEnvelope() = runTest {
    var requestedPath = ""
    var requestedMethod = HttpMethod.Get
    var requestBody = ""
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": {
                            "complex": {
                              "id": "complex-id",
                              "name": "North Sports Center",
                              "provinceId": "province-id",
                              "cantonId": "canton-id",
                              "address": "123 Main Street",
                              "latitude": 9.935,
                              "longitude": -84.091,
                              "serviceIds": ["complex-service-id"]
                            },
                            "firstCourt": {
                              "id": "court-id",
                              "complexId": "complex-id",
                              "name": "Court A",
                              "serviceIds": ["court-service-id"]
                            }
                          }
                        }
                        """,
                    status = HttpStatusCode.Created,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureMethod = { requestedMethod = it ?: HttpMethod.Get },
                    captureBody = { requestBody = it.orEmpty() },
                ),
            json = json,
        )

    val created =
        dataSource.createComplex(
            CreateComplexRequest(
                complex =
                    CreateComplexDetails(
                        name = "North Sports Center",
                        provinceId = "province-id",
                        cantonId = "canton-id",
                        address = "123 Main Street",
                        latitude = 9.935,
                        longitude = -84.091,
                        serviceIds = listOf("complex-service-id"),
                    ),
                firstCourt =
                    CreateFirstCourtDetails(
                        name = "Court A",
                        serviceIds = listOf("court-service-id"),
                    ),
            )
        )

    assertEquals(HttpMethod.Post, requestedMethod)
    assertEquals("/v1/complexes", requestedPath)
    val requestDto = json.decodeFromString<CreateComplexRequestDto>(requestBody)
    assertEquals("province-id", requestDto.complex.provinceId)
    assertEquals("canton-id", requestDto.complex.cantonId)
    assertEquals(listOf("complex-service-id"), requestDto.complex.serviceIds)
    assertEquals(listOf("court-service-id"), requestDto.firstCourt.serviceIds)
    assertEquals("complex-id", created.complexId)
    assertEquals("Court A", created.firstCourtName)
  }

  @Test
  fun createComplexMapsForbiddenEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "errors": [
                            {
                              "code": "FORBIDDEN",
                              "message": "Forbidden",
                              "status": 403
                            }
                          ]
                        }
                        """,
                    status = HttpStatusCode.Forbidden,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.createComplex(
              CreateComplexRequest(
                  complex =
                      CreateComplexDetails(
                          name = "North Sports Center",
                          provinceId = "province-id",
                          cantonId = "canton-id",
                          address = "123 Main Street",
                          latitude = null,
                          longitude = null,
                          serviceIds = emptyList(),
                      ),
                  firstCourt =
                      CreateFirstCourtDetails(
                          name = "Court A",
                          serviceIds = listOf("court-service-id"),
                      ),
              )
          )
        }

    assertEquals(403, error.statusCode)
    assertEquals("Forbidden", error.message)
  }

  @Test
  fun createComplexMapsServerEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "errors": [
                            {
                              "code": "INTERNAL_SERVER_ERROR",
                              "message": "Backend exploded",
                              "status": 500
                            }
                          ]
                        }
                        """,
                    status = HttpStatusCode.InternalServerError,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.createComplex(
              CreateComplexRequest(
                  complex =
                      CreateComplexDetails(
                          name = "North Sports Center",
                          provinceId = "province-id",
                          cantonId = "canton-id",
                          address = "123 Main Street",
                          latitude = null,
                          longitude = null,
                          serviceIds = emptyList(),
                      ),
                  firstCourt =
                      CreateFirstCourtDetails(
                          name = "Court A",
                          serviceIds = listOf("court-service-id"),
                      ),
              )
          )
        }

    assertEquals(500, error.statusCode)
    assertEquals("Backend exploded", error.message)
  }

  private fun mockClient(
      responseBody: String,
      status: HttpStatusCode = HttpStatusCode.OK,
      capturePath: (String?) -> Unit = {},
      captureMethod: (HttpMethod?) -> Unit = {},
      captureScope: (String?) -> Unit = {},
      captureBody: (String?) -> Unit = {},
  ): HttpClient =
      HttpClient(MockEngine) {
        expectSuccess = true
        engine {
          addHandler { request ->
            capturePath(request.url.encodedPath)
            captureMethod(request.method)
            captureScope(request.url.parameters["scope"])
            captureBody(request.body.readText())
            respond(
                content = responseBody.trimIndent(),
                status = status,
                headers =
                    headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
            )
          }
        }
        install(ContentNegotiation) { json(this@ComplexRemoteDataSourceTest.json) }
        install(DefaultRequest) { url("https://api.example.test") }
      }

  private fun OutgoingContent.readText(): String =
      when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.ReadChannelContent ->
            error("Unsupported request body type in test: ${this::class}")
        is OutgoingContent.WriteChannelContent ->
            error("Unsupported request body type in test: ${this::class}")
        is OutgoingContent.NoContent -> ""
        is OutgoingContent.ProtocolUpgrade ->
            error("Unsupported request body type in test: ${this::class}")
        else -> error("Unsupported request body type in test: ${this::class}")
      }
}
