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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CourtCatalogRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun getCatalogCourtsFetchesPublicCatalogAndMapsEnvelope() = runTest {
    var requestedPath = ""
    var requestedQuery = ""
    val dataSource =
        CourtCatalogRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": [
                            {
                              "courtId": "court-id",
                              "courtName": "Cancha 1",
                              "complexId": "complex-id",
                              "complexName": "Complejo Los Nogales",
                              "province": { "id": "province-id", "name": "San José" },
                              "canton": { "id": "canton-id", "name": "Escazú" },
                              "services": ["Sintetico", "Iluminacion"],
                              "rating": { "average": 4.5, "count": 2 },
                              "isReservableToday": true,
                              "imageUrl": null
                            }
                          ]
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureQuery = { requestedQuery = it.orEmpty() },
                ),
            json = json,
        )

    val courts =
        dataSource.getCatalogCourts(
            searchQuery = "nogales",
            provinceId = "province-id",
            cantonId = "canton-id",
        )

    assertEquals("/v1/courts/catalog", requestedPath)
    assertEquals("q=nogales&provinceId=province-id&cantonId=canton-id", requestedQuery)
    assertEquals("court-id", courts.single().id)
    assertEquals(listOf("Sintetico", "Iluminacion"), courts.single().services)
    assertEquals(4.5, courts.single().ratingAverage)
  }

  @Test
  fun getCatalogCourtsMapsNullRatingAverageWithoutDroppingCourt() = runTest {
    val dataSource =
        CourtCatalogRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": true,
                          "data": [
                            {
                              "courtId": "2283afcc-3c70-41b5-9300-b741349d5528",
                              "courtName": "test",
                              "complexId": "complex-test-id",
                              "complexName": "test",
                              "province": { "id": "province-san-jose", "name": "San Jose" },
                              "canton": { "id": "canton-san-jose", "name": "San Jose" },
                              "services": ["Sintetico", "Iluminacion"],
                              "rating": { "average": null, "count": 0 },
                              "isReservableToday": true,
                              "imageUrl": null
                            }
                          ]
                        }
                        """,
                ),
            json = json,
        )

    val courts = dataSource.getCatalogCourts(searchQuery = "test")

    assertEquals(1, courts.size)
    assertEquals("2283afcc-3c70-41b5-9300-b741349d5528", courts.single().id)
    assertEquals("test · test", courts.single().displayName)
    assertEquals(null, courts.single().ratingAverage)
    assertEquals(0, courts.single().ratingCount)
    assertEquals(true, courts.single().isReservableToday)
  }

  @Test
  fun getCatalogCourtsMapsErrorEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        CourtCatalogRemoteDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "success": false,
                          "errors": [
                            {
                              "code": "VALIDATION_FAILED",
                              "message": "Selected canton does not belong to province.",
                              "status": 400
                            }
                          ]
                        }
                        """,
                    status = HttpStatusCode.BadRequest,
                ),
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.getCatalogCourts(
              provinceId = "province-id",
              cantonId = "canton-id",
          )
        }

    assertEquals(400, error.statusCode)
    assertEquals("Selected canton does not belong to province.", error.message)
  }

  private fun mockClient(
      responseBody: String,
      status: HttpStatusCode = HttpStatusCode.OK,
      capturePath: (String?) -> Unit = {},
      captureQuery: (String?) -> Unit = {},
  ): HttpClient =
      HttpClient(MockEngine) {
        expectSuccess = true
        engine {
          addHandler { request ->
            capturePath(request.url.encodedPath)
            captureQuery(request.url.encodedQuery)
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
        install(ContentNegotiation) { json(this@CourtCatalogRemoteDataSourceTest.json) }
        install(DefaultRequest) { url("https://api.example.test") }
      }
}
