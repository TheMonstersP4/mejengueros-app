package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.UserProfileEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class AuthenticatedUserRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : IAuthenticatedUserRemoteDataSource {
  override suspend fun syncCurrentUser(): UserProfile {
    return try {
      val envelope = httpClient.get("v1/users/me").body<UserProfileEnvelopeDto>()
      val data =
          envelope.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )
      UserProfile(
          id = data.id,
          roles =
              data.roles.mapNotNull { rawRole -> UserRoleKind.entries.find { it.name == rawRole } },
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
