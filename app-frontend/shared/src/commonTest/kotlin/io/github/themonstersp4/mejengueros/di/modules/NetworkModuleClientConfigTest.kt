package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.IAuthTokenProvider
import io.github.themonstersp4.mejengueros.data.remote.AppApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class NetworkModuleClientConfigTest {
  private val json = Json { ignoreUnknownKeys = true }
  private val apiConfig =
      AppApiConfig(baseUrl = "https://85u7xyr1p9.execute-api.us-east-2.amazonaws.com")

  @Test
  fun publicClientBuildsExactCatalogUrlWithoutAuthorizationHeader() = runTest {
    var requestedUrl = ""
    var authorizationHeader: String? = "placeholder"
    val client =
        mockClient(
                captureUrl = { requestedUrl = it },
                captureAuthorizationHeader = { authorizationHeader = it },
            )
            .withPublicAppApiDefaults(json, apiConfig)

    client.get("/v1/courts/catalog") { url.parameters.append("q", "test") }

    assertEquals(
        "https://85u7xyr1p9.execute-api.us-east-2.amazonaws.com/v1/courts/catalog?q=test",
        requestedUrl,
    )
    assertNull(authorizationHeader)
  }

  @Test
  fun authenticatedClientKeepsExactCatalogUrlAndAddsBearerToken() = runTest {
    var requestedUrl = ""
    var authorizationHeader: String? = null
    val client =
        mockClient(
                captureUrl = { requestedUrl = it },
                captureAuthorizationHeader = { authorizationHeader = it },
            )
            .withAuthenticatedAppApiDefaults(
                appJson = json,
                apiConfig = apiConfig,
                tokenProvider = FakeAuthTokenProvider("token-123"),
            )

    client.get("/v1/courts/catalog") { url.parameters.append("q", "test") }

    assertEquals(
        "https://85u7xyr1p9.execute-api.us-east-2.amazonaws.com/v1/courts/catalog?q=test",
        requestedUrl,
    )
    assertEquals("Bearer token-123", authorizationHeader)
  }

  private fun mockClient(
      captureUrl: (String) -> Unit,
      captureAuthorizationHeader: (String?) -> Unit,
  ): HttpClient =
      HttpClient(MockEngine) {
        engine {
          addHandler { request ->
            captureUrl(request.url.toString())
            captureAuthorizationHeader(request.headers[HttpHeaders.Authorization])
            respond(
                content = "{\"success\":true,\"data\":[]}",
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
            )
          }
        }
        install(ContentNegotiation) { json(this@NetworkModuleClientConfigTest.json) }
      }
}

private class FakeAuthTokenProvider(
    private val token: String?,
) : IAuthTokenProvider {
  override fun getBearerToken(): String? = token
}
