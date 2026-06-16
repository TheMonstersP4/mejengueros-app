package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class InMemoryAuthSecureStorageTest {
  @Test
  fun saveSessionKeepsCurrentSessionUntilCleared() = runTest {
    val storage = InMemoryAuthSecureStorage()

    storage.saveSession(sampleSession())

    assertEquals(sampleSession(), storage.getSession())
    storage.clearSession()
    assertNull(storage.getSession())
  }

  @Test
  fun saveOAuthStateKeepsPendingStateUntilCleared() = runTest {
    val storage = InMemoryAuthSecureStorage()
    val state = PendingOAuthState(state = "state", codeVerifier = "verifier")

    storage.saveOAuthState(state)

    assertEquals(state, storage.getOAuthState())
    storage.clearOAuthState()
    assertNull(storage.getOAuthState())
  }

  private fun sampleSession(): AuthSession =
      AuthSession(
          sub = "user-sub",
          email = "player@example.com",
          displayName = "Player",
          provider = "Google",
          idToken = "id-token",
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresAtEpochSeconds = 4102444800,
      )
}
