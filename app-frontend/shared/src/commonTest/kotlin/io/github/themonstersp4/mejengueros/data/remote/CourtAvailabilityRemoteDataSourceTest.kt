package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
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

class CourtAvailabilityRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getCourtAvailabilityFetchesOwnedCourtContext() = runTest {
    var requestedPath = ""
    var authorizationHeader = ""
    val dataSource =
        CourtAvailabilityRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": {
                            "court": {
                              "id": "court-id",
                              "name": "Cancha 1",
                              "complexId": "complex-id",
                              "complexName": "Mejengas CR"
                            },
                            "availability": {
                              "days": ["MONDAY", "WEDNESDAY"],
                              "startTime": "06:00",
                              "endTime": "09:00"
                            }
                          }
                        }
                        """
                            .trimIndent(),
                    capturePath = { requestedPath = it.orEmpty() },
                    captureAuthorizationHeader = { authorizationHeader = it.orEmpty() },
                    authToken = "owner-token",
                ),
            json = json,
        )

    val context = dataSource.getCourtAvailability("court-id")

    assertEquals("/v1/courts/court-id/availability", requestedPath)
    assertEquals("Bearer owner-token", authorizationHeader)
    assertEquals("Cancha 1", context.courtName)
    assertEquals(
        listOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.WEDNESDAY),
        context.availability?.days,
    )
  }

  @Test
  fun saveCourtAvailabilityPostsRealPayloadAndMapsEnvelope() = runTest {
    var requestedPath = ""
    var requestedMethod = HttpMethod.Get
    var requestBody = ""
    val dataSource =
        CourtAvailabilityRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": {
                            "court": {
                              "id": "court-id",
                              "name": "Cancha 1",
                              "complexId": "complex-id",
                              "complexName": "Mejengas CR"
                            },
                            "availability": {
                              "days": ["MONDAY", "FRIDAY"],
                              "startTime": "07:00",
                              "endTime": "10:00"
                            }
                          }
                        }
                        """
                            .trimIndent(),
                    status = HttpStatusCode.OK,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureMethod = { requestedMethod = it ?: HttpMethod.Get },
                    captureBody = { requestBody = it.orEmpty() },
                ),
            json = json,
        )

    val context =
        dataSource.saveCourtAvailability(
            courtId = "court-id",
            availability =
                CourtAvailabilityConfig(
                    days = listOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.FRIDAY),
                    startTime = "07:00",
                    endTime = "10:00",
                ),
        )

    assertEquals(HttpMethod.Put, requestedMethod)
    assertEquals("/v1/courts/court-id/availability", requestedPath)
    assertEquals(
        "{\"days\":[\"MONDAY\",\"FRIDAY\"],\"startTime\":\"07:00\",\"endTime\":\"10:00\"}",
        requestBody,
    )
    assertEquals("Mejengas CR", context.complexName)
    assertEquals("10:00", context.availability?.endTime)
  }

  @Test
  fun saveCourtAvailabilityMapsForbiddenEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        CourtAvailabilityRemoteDataSource(
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
                        """
                            .trimIndent(),
                    status = HttpStatusCode.Forbidden,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.saveCourtAvailability(
              courtId = "court-id",
              availability =
                  CourtAvailabilityConfig(
                      days = listOf(CourtAvailabilityWeekday.MONDAY),
                      startTime = "06:00",
                      endTime = "07:00",
                  ),
          )
        }

    assertEquals(403, error.statusCode)
    assertEquals("Forbidden", error.message)
  }
}

private fun mockClient(
    responseBody: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    capturePath: (String?) -> Unit = {},
    captureMethod: (HttpMethod?) -> Unit = {},
    captureBody: (String?) -> Unit = {},
    captureAuthorizationHeader: (String?) -> Unit = {},
    authToken: String? = null,
): HttpClient =
    HttpClient(MockEngine) {
      expectSuccess = true
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      install(DefaultRequest) {
        url("https://api.mejengueros.test")
        authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
      }
      engine {
        addHandler { request ->
          capturePath(request.url.encodedPath)
          captureMethod(request.method)
          captureBody(request.body.readText())
          captureAuthorizationHeader(request.headers[HttpHeaders.Authorization])
          respond(
              content = responseBody,
              status = status,
              headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      }
    }

private fun OutgoingContent.readText(): String =
    when (this) {
      is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
      is OutgoingContent.NoContent -> ""
      else -> error("Unsupported request body type in test: ${this::class}")
    }
