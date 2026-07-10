package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewCourt
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewer
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ReviewOwnerReviewsDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getOwnerReceivedReviewsMapsSummaryItemsAndPagination() = runTest {
    val requestParams = mutableListOf<Map<String, String>>()
    val requestPaths = mutableListOf<String>()
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      requestPaths += request.url.encodedPath
                      requestParams +=
                          request.url.parameters.entries().associate { (k, v) -> k to v.first() }
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "summary": {
                                    "selectedCourtId": "court-1",
                                    "totalReviews": 24,
                                    "averageRating": 4.5
                                  },
                                  "items": [
                                    {
                                      "reviewId": "review-1",
                                      "rating": 5,
                                      "comment": "Excelente cancha",
                                      "createdAt": "2026-07-01T20:00:00.000Z",
                                      "court": { "id": "court-1", "name": "Cancha A" },
                                      "reviewer": { "displayName": "Mariana P.", "initials": "MP" }
                                    }
                                  ]
                                },
                                "meta": {
                                  "pagination": {
                                    "page": 2,
                                    "pageSize": 10,
                                    "totalItems": 24,
                                    "totalPages": 3
                                  }
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
                  install(ContentNegotiation) { json(this@ReviewOwnerReviewsDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            json = json,
        )

    val page = dataSource.getOwnerReceivedReviews(courtId = "court-1", page = 2, pageSize = 10)

    assertEquals(listOf("/v1/owners/me/reviews"), requestPaths)
    assertEquals("court-1", requestParams.single()["courtId"])
    assertEquals("2", requestParams.single()["page"])
    assertEquals("10", requestParams.single()["pageSize"])
    assertEquals(
        ReceivedReviewPage(
            items =
                listOf(
                    ReceivedReview(
                        reviewId = "review-1",
                        rating = 5,
                        comment = "Excelente cancha",
                        createdAt = "2026-07-01T20:00:00.000Z",
                        court = ReceivedReviewCourt(id = "court-1", name = "Cancha A"),
                        reviewer =
                            ReceivedReviewer(
                                displayName = "Mariana P.",
                                initials = "MP",
                            ),
                    )
                ),
            summary =
                ReceivedReviewsSummary(
                    selectedCourtId = "court-1",
                    totalReviews = 24,
                    averageRating = 4.5,
                ),
            page = 2,
            pageSize = 10,
            totalItems = 24,
            totalPages = 3,
            hasNextPage = true,
        ),
        page,
    )
  }

  @Test
  fun getOwnerReceivedReviewsOmitsCourtIdWhenBlank() = runTest {
    val requestParams = mutableListOf<Map<String, String>>()
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      requestParams +=
                          request.url.parameters.entries().associate { (k, v) -> k to v.first() }
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "summary": { "totalReviews": 0, "averageRating": 0.0 },
                                  "items": []
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
                  install(ContentNegotiation) { json(this@ReviewOwnerReviewsDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            json = json,
        )

    val page = dataSource.getOwnerReceivedReviews(courtId = "  ", page = 1, pageSize = 10)

    assertTrue(requestParams.single().keys.none { it == "courtId" })
    assertEquals(0, page.items.size)
    assertEquals(1, page.page)
    assertEquals(false, page.hasNextPage)
  }

  @Test
  fun getOwnerReceivedReviewsToleratesNullAverageRatingAndNullComment() = runTest {
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
                                "data": {
                                  "summary": { "totalReviews": 2 },
                                  "items": [
                                    {
                                      "reviewId": "review-1",
                                      "rating": 4,
                                      "createdAt": "2026-07-01T20:00:00.000Z",
                                      "court": { "id": "court-1", "name": "Cancha A" },
                                      "reviewer": { "displayName": "Mariana P.", "initials": "MP" }
                                    },
                                    {
                                      "reviewId": "review-2",
                                      "rating": 5,
                                      "comment": null,
                                      "createdAt": "2026-07-02T20:00:00.000Z",
                                      "court": { "id": "court-1", "name": "Cancha A" },
                                      "reviewer": { "displayName": "Carlos R.", "initials": "CR" }
                                    }
                                  ]
                                },
                                "meta": { "pagination": { "page": 1, "pageSize": 10, "totalItems": 2, "totalPages": 1 } }
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
                  install(ContentNegotiation) { json(this@ReviewOwnerReviewsDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            json = json,
        )

    val page = dataSource.getOwnerReceivedReviews(courtId = null, page = 1, pageSize = 10)

    assertEquals(2, page.items.size)
    assertNull(page.items[0].comment)
    assertNull(page.items[1].comment)
    assertNull(page.summary.averageRating)
    assertEquals(2, page.summary.totalReviews)
  }

  @Test
  fun getOwnerReceivedReviewsMapsHttpErrorsToAppApiException() = runTest {
    val dataSource =
        ReviewRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content = """{"success":false,"error":{"message":"Forbidden owner"}}""",
                          status = HttpStatusCode.Forbidden,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) { json(this@ReviewOwnerReviewsDataSourceTest.json) }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.getOwnerReceivedReviews(courtId = null, page = 1, pageSize = 10)
        }
    assertEquals(403, error.statusCode)
  }
}
