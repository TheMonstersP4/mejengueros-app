package io.github.themonstersp4.mejengueros.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AuthenticatedUserRemoteDataSourceTest {
  @Test
  fun syncCurrentUserCallsUsersMeEndpoint() = runTest {
    var requestedPath: String? = null
    val dataSource =
        AuthenticatedUserRemoteDataSource(
            HttpClient(MockEngine) {
              engine {
                addHandler { request ->
                  requestedPath = request.url.encodedPath
                  respond(content = "{}", status = HttpStatusCode.OK)
                }
              }
            }
        )

    dataSource.syncCurrentUser()

    assertEquals("/v1/users/me", requestedPath)
  }
}
