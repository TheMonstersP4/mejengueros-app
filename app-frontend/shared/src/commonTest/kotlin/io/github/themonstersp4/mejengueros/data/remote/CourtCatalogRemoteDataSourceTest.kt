package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
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
                              "imageUrl": "https://read.example.test/courts/court-id.jpg"
                            }
                          ],
                          "meta": {
                            "pagination": {
                              "page": 2,
                              "pageSize": 20,
                              "totalItems": 45,
                              "totalPages": 3
                            }
                          }
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureQuery = { requestedQuery = it.orEmpty() },
                ),
            json = json,
        )

    val page =
        dataSource.getCatalogCourts(
            searchQuery = "nogales",
            provinceId = "province-id",
            cantonId = "canton-id",
            serviceIds = listOf("service-a", "service-b"),
            page = 2,
            pageSize = 20,
        )

    assertEquals("/v1/courts/catalog", requestedPath)
    assertEquals(
        "q=nogales&provinceId=province-id&cantonId=canton-id&serviceIds=service-a&serviceIds=service-b&page=2&pageSize=20",
        requestedQuery,
    )
    assertEquals("court-id", page.items.single().id)
    assertEquals(listOf("Sintetico", "Iluminacion"), page.items.single().services)
    assertEquals(4.5, page.items.single().ratingAverage)
    assertEquals("https://read.example.test/courts/court-id.jpg", page.items.single().imageUrl)
    assertEquals(2, page.page)
    assertEquals(45, page.totalItems)
    assertEquals(3, page.totalPages)
    assertEquals(true, page.hasNextPage)
  }

  @Test
  fun getServiceCatalogFetchesActiveServicesWithoutScopeAndMapsEnvelope() = runTest {
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
                            { "id": "service-1", "name": "Sintetico", "scope": "COURT" },
                            { "id": "service-2", "name": "Parqueo", "scope": "COMPLEX" }
                          ]
                        }
                        """,
                    capturePath = { requestedPath = it.orEmpty() },
                    captureQuery = { requestedQuery = it.orEmpty() },
                ),
            json = json,
        )

    val services = dataSource.getServiceCatalog()

    assertEquals("/v1/services", requestedPath)
    // No scope filter so every active service is offered as a catalog filter option.
    assertEquals("", requestedQuery)
    assertEquals(listOf("service-1", "service-2"), services.map { it.id })
    assertEquals(listOf("Sintetico", "Parqueo"), services.map { it.name })
    assertEquals(ServiceScope.COURT, services.first().scope)
  }

  @Test
  fun getCatalogCourtsFallsBackToRequestedWindowWhenPaginationMetaIsMissing() = runTest {
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
                              "services": [],
                              "rating": { "average": null, "count": 0 },
                              "isReservableToday": false,
                              "imageUrl": null
                            }
                          ]
                        }
                        """,
                ),
            json = json,
        )

    val page = dataSource.getCatalogCourts(page = 4, pageSize = 20)

    assertEquals(4, page.page)
    assertEquals(20, page.pageSize)
    assertEquals(1, page.items.size)
    // Without metadata the datasource assumes the current page is the last one.
    assertEquals(4, page.totalPages)
    assertEquals(false, page.hasNextPage)
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
