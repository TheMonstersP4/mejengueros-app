package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters

class CognitoAuthRemoteDataSource(
    private val httpClient: HttpClient,
    private val config: CognitoAuthConfig,
) : IAuthRemoteDataSource {
  override suspend fun exchangeCode(code: String, codeVerifier: String): CognitoTokenResponseDto =
      httpClient
          .submitForm(
              url = config.tokenEndpoint,
              formParameters =
                  Parameters.build {
                    append("grant_type", "authorization_code")
                    append("client_id", config.clientId)
                    append("code", code)
                    append("redirect_uri", config.redirectUri)
                    append("code_verifier", codeVerifier)
                  },
          )
          .body()
}
