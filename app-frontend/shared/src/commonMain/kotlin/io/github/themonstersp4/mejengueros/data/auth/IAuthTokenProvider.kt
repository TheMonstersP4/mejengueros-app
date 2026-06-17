package io.github.themonstersp4.mejengueros.data.auth

interface IAuthTokenProvider {
  fun getBearerToken(): String?
}
