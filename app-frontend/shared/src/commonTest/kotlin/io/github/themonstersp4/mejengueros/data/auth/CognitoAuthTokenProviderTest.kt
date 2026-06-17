package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class CognitoAuthTokenProviderTest {
  @Test
  fun getBearerTokenReturnsStoredIdTokenWhenSessionIsActive() = runTest {
    val storage = InMemoryAuthSecureStorage()
    storage.saveSession(sampleSession(expiresAtEpochSeconds = 4102444800))
    val provider = CognitoAuthTokenProvider(storage)

    assertEquals("id-token", provider.getBearerToken())
  }

  @Test
  fun getBearerTokenReturnsNullWhenSessionIsExpired() = runTest {
    val storage = InMemoryAuthSecureStorage()
    storage.saveSession(sampleSession(expiresAtEpochSeconds = 1))
    val provider = CognitoAuthTokenProvider(storage)

    assertNull(provider.getBearerToken())
  }

  @Test
  fun getBearerTokenReturnsNullWhenSessionIsMissing() {
    val provider = CognitoAuthTokenProvider(InMemoryAuthSecureStorage())

    assertNull(provider.getBearerToken())
  }

  private fun sampleSession(expiresAtEpochSeconds: Long): AuthSession =
      AuthSession(
          sub = "user-sub",
          email = "player@example.com",
          displayName = "Player",
          provider = "Google",
          idToken = "id-token",
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresAtEpochSeconds = expiresAtEpochSeconds,
      )
}
