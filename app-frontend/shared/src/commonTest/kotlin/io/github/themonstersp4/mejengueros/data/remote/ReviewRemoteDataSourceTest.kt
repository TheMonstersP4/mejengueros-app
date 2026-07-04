package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ReviewRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getLatestReviewableReservationReturnsReservationContextFromApi() = runTest {
    val requestPaths = mutableListOf<String>()
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      requestPaths += request.url.encodedPath
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "reservationId": "reservation-id",
                                  "complexName": "Moravia FC",
                                  "courtName": "Cancha A",
                                  "startsAt": "2026-07-02T20:00:00.000Z",
                                  "endsAt": "2026-07-02T21:00:00.000Z",
                                  "imageUrl": "https://read.example.test/court-a.png"
                                }
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.OK,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) { json(this@ReviewRemoteDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
                  }
                },
            json = json,
        )

    val result = dataSource.getLatestReviewableReservation()

    assertEquals("reservation-id", result?.reservationId)
    assertEquals("Moravia FC", result?.complexName)
    assertEquals("Cancha A", result?.courtName)
    assertEquals(listOf("/v1/reviews/latest-eligible-reservation"), requestPaths)
  }

  @Test
  fun getLatestReviewableReservationReturnsNullWhenEnvelopeDataIsNull() = runTest {
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": null
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.OK,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) { json(this@ReviewRemoteDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
                  }
                },
            json = json,
        )

    val result = dataSource.getLatestReviewableReservation()

    assertNull(result)
  }

  @Test
  fun createReviewPostsReservationIdRatingCommentAndEvidenceImage() = runTest {
    val requestBodies = mutableListOf<String>()
    val requestMethods = mutableListOf<String>()
    val requestPaths = mutableListOf<String>()
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      requestMethods += request.method.value
                      requestPaths += request.url.encodedPath
                      requestBodies += request.body.readText()
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "id": "review-id",
                                  "reservationId": "reservation-id",
                                  "rating": 1,
                                  "comment": "La iluminación falló toda la hora.",
                                  "evidenceImageUploadId": "evidence-image-id",
                                  "createdAt": "2026-07-03T02:00:00.000Z"
                                }
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.Created,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) { json(this@ReviewRemoteDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
                  }
                },
            json = json,
        )

    val result =
        dataSource.createReview(
            CreateReviewRequest(
                reservationId = "reservation-id",
                rating = 1,
                comment = "La iluminación falló toda la hora.",
                evidenceImageUploadId = "evidence-image-id",
            )
        )

    assertEquals("review-id", result.id)
    assertEquals(1, result.rating)
    assertEquals(1, requestBodies.size)
    assertEquals(listOf("POST"), requestMethods)
    assertEquals(listOf("/v1/reviews"), requestPaths)
    assertTrue(requestBodies.single().contains("\"reservationId\":\"reservation-id\""))
    assertTrue(requestBodies.single().contains("\"rating\":1"))
    assertTrue(
        requestBodies.single().contains("\"comment\":\"La iluminación falló toda la hora.\"")
    )
    assertTrue(requestBodies.single().contains("\"evidenceImageUploadId\":\"evidence-image-id\""))
  }

  @Test
  fun createReviewMapsHttpErrorsToAppApiException() = runTest {
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content = """{"success":false,"error":{"message":"Invalid review"}}""",
                          status = HttpStatusCode.BadRequest,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) { json(this@ReviewRemoteDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.createReview(
              CreateReviewRequest(
                  reservationId = "reservation-id",
                  rating = 1,
                  comment = "La iluminación falló toda la hora.",
                  evidenceImageUploadId = "evidence-image-id",
              )
          )
        }

    assertEquals(400, error.statusCode)
  }

  private fun OutgoingContent.readText(): String =
      when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.NoContent -> ""
        else -> error("Unsupported request body type in test: ${this::class}")
      }
}
