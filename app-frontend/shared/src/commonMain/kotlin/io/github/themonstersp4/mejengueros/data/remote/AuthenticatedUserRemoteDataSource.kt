package io.github.themonstersp4.mejengueros.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class AuthenticatedUserRemoteDataSource(private val httpClient: HttpClient) :
    IAuthenticatedUserRemoteDataSource {
  override suspend fun syncCurrentUser() {
    httpClient.get("v1/users/me")
  }
}
