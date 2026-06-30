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

  @Test
  fun getCatalogCourtsMapsDeployedCatalogPayloadWithNullRatingAndBlankSearch() = runTest {
    var requestedPath = ""
    var requestedQuery = "placeholder"
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
                              "complexId": "3221268f-e7dc-4cc6-93d0-b49b372d6d1b",
                              "complexName": "test",
                              "province": { "id": "427068af-53eb-4ef4-a7bc-ea9e98636296", "name": "San Jose" },
                              "canton": { "id": "e0e68731-9ae0-49b7-abe2-8f8042c551ae", "name": "San Jose" },
                              "services": ["Pasto sintético", "Sintetico", "Hibrido", "Natural", "Iluminacion", "Iluminación"],
                              "rating": { "average": null, "count": 0 },
                              "isReservableToday": true,
                              "imageUrl": null
                            }
                          ],
                          "errors": [],
                          "meta": {
                            "requestId": "req-3",
                            "path": "/v1/courts/catalog",
                            "timestamp": "2026-06-30T18:02:51.674Z"
                          }
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureQuery = { requestedQuery = it.orEmpty() },
                ),
            json = json,
        )

    val courts = dataSource.getCatalogCourts(searchQuery = "   ")

    assertEquals("/v1/courts/catalog", requestedPath)
    assertEquals("", requestedQuery)
    assertEquals("test", courts.single().courtName)
    assertEquals(null, courts.single().ratingAverage)
    assertEquals(0, courts.single().ratingCount)
    assertEquals("San Jose", courts.single().provinceName)
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
