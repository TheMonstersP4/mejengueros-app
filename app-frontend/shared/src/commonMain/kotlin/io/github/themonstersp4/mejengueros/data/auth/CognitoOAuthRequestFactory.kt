package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.ktor.http.URLBuilder

class CognitoOAuthRequestFactory(private val config: CognitoAuthConfig) {
  fun createSignInRequest(
      provider: AuthProvider,
      state: String,
      codeChallenge: String,
  ): AuthSignInRequest {
    val url =
        URLBuilder(config.authorizationEndpoint)
            .apply {
              parameters.append("client_id", config.clientId)
              parameters.append("redirect_uri", config.redirectUri)
              parameters.append("response_type", "code")
              parameters.append("scope", config.scopes.joinToString(" "))
              parameters.append("identity_provider", provider.cognitoIdentityProvider)
              parameters.append("state", state)
              parameters.append("code_challenge", codeChallenge)
              parameters.append("code_challenge_method", "S256")
            }
            .buildString()
    return AuthSignInRequest(authorizationUrl = url)
  }

  fun createSignOutRequest(): AuthSignOutRequest {
    val url =
        URLBuilder(config.logoutEndpoint)
            .apply {
              parameters.append("client_id", config.clientId)
              parameters.append("logout_uri", config.logoutUri)
            }
            .buildString()
    return AuthSignOutRequest(logoutUrl = url)
  }
}
