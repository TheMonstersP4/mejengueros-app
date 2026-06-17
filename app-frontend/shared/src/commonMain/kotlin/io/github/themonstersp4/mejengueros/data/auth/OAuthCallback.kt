package io.github.themonstersp4.mejengueros.data.auth

import io.ktor.http.Url

data class OAuthCallback(val code: String, val state: String)

class OAuthCallbackParser {
  fun parse(callbackUrl: String): OAuthCallback {
    val url = Url(callbackUrl)
    url.parameters["error"]?.let { error ->
      val description = url.parameters["error_description"] ?: error
      throw IllegalStateException(description)
    }
    return OAuthCallback(
        code = url.parameters["code"] ?: error("Missing authorization code."),
        state = url.parameters["state"] ?: error("Missing OAuth state."),
    )
  }
}
