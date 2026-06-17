package io.github.themonstersp4.mejengueros.data.auth

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking

class CognitoAuthTokenProvider(private val secureStorage: IAuthSecureStorage) : IAuthTokenProvider {
  override fun getBearerToken(): String? = runBlocking {
    secureStorage.getSession()?.takeIf { it.expiresAtEpochSeconds > currentEpochSeconds() }?.idToken
  }

  @OptIn(ExperimentalTime::class)
  private fun currentEpochSeconds(): Long = Clock.System.now().epochSeconds
}
