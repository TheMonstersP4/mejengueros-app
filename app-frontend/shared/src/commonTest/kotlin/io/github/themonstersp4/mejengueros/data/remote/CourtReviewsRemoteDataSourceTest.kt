package io.github.themonstersp4.mejengueros.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

class CourtReviewsRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  private fun dataSource(
      status: HttpStatusCode,
      body: String,
      onRequestPath: (String) -> Unit = {},
  ): CourtReviewsRemoteDataSource =
      CourtReviewsRemoteDataSource(
          httpClient =
              HttpClient(MockEngine) {
                expectSuccess = true
                engine {
                  addHandler { request ->
                    onRequestPath(request.url.encodedPath)
                    respond(
                        content = body,
                        status = status,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                  }
                }
                install(ContentNegotiation) { json(this@CourtReviewsRemoteDataSourceTest.json) }
                install(DefaultRequest) { url("https://api.example.test") }
              },
          json = json,
      )

  @Test
  fun getCourtReviewsMapsPublishedReviewsWithSafeAuthorAndReadableDate() = runTest {
    val requestPaths = mutableListOf<String>()
    val source =
        dataSource(
            status = HttpStatusCode.OK,
            body =
                """
                {
                  "success": true,
                  "data": {
                    "summary": { "totalReviews": 2, "averageRating": 4.5 },
                    "items": [
                      {
                        "reviewId": "review-a",
                        "rating": 5,
                        "comment": "  Cancha impecable, volvería.  ",
                        "createdAt": "2026-07-02T18:00:00.000Z",
                        "reviewer": { "displayName": "Diego R.", "initials": "DR" }
                      },
                      {
                        "reviewId": "review-b",
                        "rating": 4,
                        "comment": null,
                        "createdAt": null,
                        "reviewer": { "displayName": "Player", "initials": "PP" }
                      }
                    ]
                  }
                }
                """
                    .trimIndent(),
        ) {
          requestPaths += it
        }

    val reviews = source.getCourtReviews("court-1")

    assertEquals(listOf("/v1/courts/court-1/reviews"), requestPaths)
    assertEquals(2, reviews.size)

    val first = reviews[0]
    assertEquals("review-a", first.id)
    assertEquals(5, first.rating)
    assertEquals("Cancha impecable, volvería.", first.comment)
    assertEquals("Diego R.", first.authorName)
    assertEquals("DR", first.authorInitials)
    assertEquals("2 de julio de 2026", first.dateLabel)

    val second = reviews[1]
    assertNull(second.comment)
    assertNull(second.dateLabel)
    assertEquals("Player", second.authorName)
  }

  @Test
  fun getCourtReviewsReturnsEmptyListWhenCourtHasNoReviews() = runTest {
    val source =
        dataSource(
            status = HttpStatusCode.OK,
            body =
                """
                {
                  "success": true,
                  "data": { "summary": { "totalReviews": 0, "averageRating": null }, "items": [] }
                }
                """
                    .trimIndent(),
        )

    assertTrue(source.getCourtReviews("court-1").isEmpty())
  }

  @Test
  fun getCourtReviewsMapsHttpErrorsToAppApiException() = runTest {
    val source =
        dataSource(
            status = HttpStatusCode.NotFound,
            body =
                """{"success":false,"errors":[{"code":"RESOURCE_NOT_FOUND","message":"No encontramos la cancha indicada.","status":404}]}""",
        )

    val error = assertFailsWith<AppApiException> { source.getCourtReviews("court-1") }

    assertEquals(404, error.statusCode)
  }
}
