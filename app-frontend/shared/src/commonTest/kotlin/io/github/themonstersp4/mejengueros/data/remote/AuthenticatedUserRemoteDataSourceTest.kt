package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class AuthenticatedUserRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun syncCurrentUserCallsUsersMeEndpoint() = runTest {
    var requestedPath: String? = null
    val dataSource =
        AuthenticatedUserRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  install(ContentNegotiation) { json(json) }
                  engine {
                    addHandler { request ->
                      requestedPath = request.url.encodedPath
                      respond(
                          content =
                              """{"success":true,"data":{"id":"user-id","roles":["PLAYER"]}}""",
                          status = HttpStatusCode.OK,
                          headers = headersOf("Content-Type", "application/json"),
                      )
                    }
                  }
                },
            json = json,
        )

    dataSource.syncCurrentUser()

    assertEquals("/v1/users/me", requestedPath)
  }

  @Test
  fun syncCurrentUserReturnsProfileWithRolesFromResponse() = runTest {
    val dataSource =
        AuthenticatedUserRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  install(ContentNegotiation) { json(json) }
                  engine {
                    addHandler {
                      respond(
                          content =
                              """{"success":true,"data":{"id":"owner-id","roles":["OWNER","PLAYER"]}}""",
                          status = HttpStatusCode.OK,
                          headers = headersOf("Content-Type", "application/json"),
                      )
                    }
                  }
                },
            json = json,
        )

    val profile = dataSource.syncCurrentUser()

    assertEquals("owner-id", profile.id)
    assertEquals(listOf(UserRoleKind.OWNER, UserRoleKind.PLAYER), profile.roles)
  }
}
