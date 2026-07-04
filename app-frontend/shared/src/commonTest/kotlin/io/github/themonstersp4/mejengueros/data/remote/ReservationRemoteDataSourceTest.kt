package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
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
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ReservationRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getReservableDaysMapsDiscoveryIntoDomainModel() = runTest {
    var requestedPath = ""
    var requestedFrom = ""
    var requestedDays = ""
    val dataSource =
        ReservationRemoteDataSource(
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
                              "status": "ACTIVE"
                            },
                            "from": "2026-07-04",
                            "days": 14,
                            "reservableDays": [
                              {
                                "date": "2026-07-05",
                                "availabilityStatus": "AVAILABLE",
                                "availableSlotsCount": 4
                              },
                              {
                                "date": "2026-07-07",
                                "availabilityStatus": "AVAILABLE",
                                "availableSlotsCount": 2
                              }
                            ]
                          }
                        }
                        """
                            .trimIndent(),
                    capturePath = { requestedPath = it.orEmpty() },
                    captureQueryFrom = { requestedFrom = it.orEmpty() },
                    captureQueryDays = { requestedDays = it.orEmpty() },
                ),
            json = json,
        )

    val result = dataSource.getReservableDays("court-id", "2026-07-04", 14)

    assertEquals("/v1/courts/court-id/reservable-days", requestedPath)
    assertEquals("2026-07-04", requestedFrom)
    assertEquals("14", requestedDays)
    assertEquals("2026-07-04", result.fromUtc)
    assertEquals(14, result.days)
    assertEquals(listOf("2026-07-05", "2026-07-07"), result.reservableDays.map { it.dateUtc })
    assertEquals(listOf(4, 2), result.reservableDays.map { it.availableSlotsCount })
  }

  @Test
  fun getReservableDaysPropagatesApiErrors() = runTest {
    val dataSource =
        ReservationRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "errors": [
                            {
                              "code": "COURT_NOT_FOUND",
                              "message": "Court not found.",
                              "status": 404
                            }
                          ]
                        }
                        """
                            .trimIndent(),
                    status = HttpStatusCode.NotFound,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.getReservableDays("court-id", "2026-07-04", 14)
        }

    assertEquals(404, error.statusCode)
    assertEquals("Court not found.", error.message)
  }

  @Test
  fun getReservableSlotsMapsReservationWindowIntoDomainAvailability() = runTest {
    var requestedPath = ""
    var requestedDate = ""
    var authorizationHeader = ""
    val dataSource =
        ReservationRemoteDataSource(
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
                              "status": "ACTIVE"
                            },
                            "date": "2026-07-16",
                            "availabilityStatus": "AVAILABLE",
                            "slots": [
                              {
                                "startsAt": "2026-07-16T18:00:00.000Z",
                                "endsAt": "2026-07-16T19:00:00.000Z"
                              },
                              {
                                "startsAt": "2026-07-16T20:00:00.000Z",
                                "endsAt": "2026-07-16T21:00:00.000Z"
                              }
                            ]
                          }
                        }
                        """
                            .trimIndent(),
                    capturePath = { requestedPath = it.orEmpty() },
                    captureMethod = {},
                    captureQueryDate = { requestedDate = it.orEmpty() },
                    captureAuthorizationHeader = { authorizationHeader = it.orEmpty() },
                    authToken = "player-token",
                ),
            json = json,
        )

    val result = dataSource.getReservableSlots(courtId = "court-id", dateUtc = "2026-07-16")

    assertEquals("/v1/courts/court-id/reservable-slots", requestedPath)
    assertEquals("2026-07-16", requestedDate)
    assertEquals("Bearer player-token", authorizationHeader)
    assertEquals(ReservationAvailabilityStatus.Available, result.availabilityStatus)
    assertEquals(listOf("18:00", "20:00"), result.slots.map { it.displayStartTime })
  }

  @Test
  fun createReservationPostsUtcPayloadAndMapsConfirmation() = runTest {
    var requestedPath = ""
    var requestedMethod = HttpMethod.Get
    var requestBody = ""
    val dataSource =
        ReservationRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": {
                            "id": "reservation-id",
                            "courtId": "court-id",
                            "startsAt": "2026-07-16T19:00:00.000Z",
                            "endsAt": "2026-07-16T20:00:00.000Z",
                            "status": "CONFIRMED"
                          }
                        }
                        """
                            .trimIndent(),
                    capturePath = { requestedPath = it.orEmpty() },
                    captureMethod = { requestedMethod = it ?: HttpMethod.Get },
                    captureBody = { requestBody = it.orEmpty() },
                ),
            json = json,
        )

    val result =
        dataSource.createReservation(
            courtId = "court-id",
            startsAtUtc = "2026-07-16T19:00:00.000Z",
        )

    assertEquals(HttpMethod.Post, requestedMethod)
    assertEquals("/v1/reservations", requestedPath)
    assertEquals(
        "{\"courtId\":\"court-id\",\"startsAt\":\"2026-07-16T19:00:00.000Z\"}",
        requestBody,
    )
    assertEquals("reservation-id", result.id)
    assertEquals("20:00", result.endsAtUtc.substring(11, 16))
  }

  @Test
  fun createReservationMapsConflictEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        ReservationRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "errors": [
                            {
                              "code": "RESERVATION_CONFLICT",
                              "message": "This court already has a confirmed reservation for the selected start time.",
                              "status": 409
                            }
                          ]
                        }
                        """
                            .trimIndent(),
                    status = HttpStatusCode.Conflict,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.createReservation(
              courtId = "court-id",
              startsAtUtc = "2026-07-16T19:00:00.000Z",
          )
        }

    assertEquals(409, error.statusCode)
    assertEquals(
        "This court already has a confirmed reservation for the selected start time.",
        error.message,
    )
  }
}

private fun mockClient(
    responseBody: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    capturePath: (String?) -> Unit = {},
    captureMethod: (HttpMethod?) -> Unit = {},
    captureQueryDate: (String?) -> Unit = {},
    captureQueryFrom: (String?) -> Unit = {},
    captureQueryDays: (String?) -> Unit = {},
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
          captureQueryDate(request.url.parameters["date"])
          captureQueryFrom(request.url.parameters["from"])
          captureQueryDays(request.url.parameters["days"])
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
      is TextContent -> text
      is OutgoingContent.NoContent -> ""
      else -> error("Unsupported request body type in test: ${this::class}")
    }
