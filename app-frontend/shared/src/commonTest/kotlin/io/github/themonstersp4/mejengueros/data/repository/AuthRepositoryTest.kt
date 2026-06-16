package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRepositoryTest {

  @Test
  fun signInTrimsUsernameAndSavesSession() {
    val localDataSource = FakeAuthLocalDataSource()
    val repository = AuthRepository(localDataSource)

    val session = repository.signIn("  player-one  ")

    assertEquals(AuthSession(username = "player-one"), session)
    assertEquals(AuthSession(username = "player-one"), localDataSource.savedSession)
  }

  @Test
  fun getSessionReturnsLocalSession() {
    val localDataSource = FakeAuthLocalDataSource(AuthSession(username = "stored-user"))
    val repository = AuthRepository(localDataSource)

    val session = repository.getSession()

    assertEquals(AuthSession(username = "stored-user"), session)
  }

  @Test
  fun signOutClearsLocalSession() {
    val localDataSource = FakeAuthLocalDataSource(AuthSession(username = "stored-user"))
    val repository = AuthRepository(localDataSource)

    repository.signOut()

    assertNull(localDataSource.savedSession)
    assertEquals(1, localDataSource.clearCount)
  }

  private class FakeAuthLocalDataSource(initialSession: AuthSession? = null) :
      IAuthLocalDataSource {
    var savedSession: AuthSession? = initialSession
    var clearCount = 0

    override fun getSession(): AuthSession? = savedSession

    override fun saveSession(session: AuthSession) {
      savedSession = session
    }

    override fun clearSession() {
      savedSession = null
      clearCount++
    }
  }
}
