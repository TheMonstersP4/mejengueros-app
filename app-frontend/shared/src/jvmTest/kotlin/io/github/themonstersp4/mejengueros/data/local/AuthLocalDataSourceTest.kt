package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthLocalDataSourceTest {

  @Test
  fun getSessionReturnsNullWhenCacheIsEmpty() {
    val dataSource = createDataSource()

    val session = dataSource.getSession()

    assertNull(session)
  }

  @Test
  fun saveSessionPersistsCognitoSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(sampleSession("stored-user"))

    assertEquals(sampleSession("stored-user"), dataSource.getSession())
  }

  @Test
  fun saveSessionReplacesExistingSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(sampleSession("first-user"))
    dataSource.saveSession(sampleSession("second-user"))

    assertEquals(sampleSession("second-user"), dataSource.getSession())
  }

  @Test
  fun clearSessionRemovesPersistedSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(sampleSession("stored-user"))
    dataSource.clearSession()

    assertNull(dataSource.getSession())
  }

  @Test
  fun saveOAuthStatePersistsPendingLoginState() {
    val dataSource = createDataSource()

    dataSource.saveOAuthState(PendingOAuthState(state = "state", codeVerifier = "verifier"))

    assertEquals(
        PendingOAuthState(state = "state", codeVerifier = "verifier"),
        dataSource.getOAuthState(),
    )
  }

  @Test
  fun clearOAuthStateRemovesPendingLoginState() {
    val dataSource = createDataSource()

    dataSource.saveOAuthState(PendingOAuthState(state = "state", codeVerifier = "verifier"))
    dataSource.clearOAuthState()

    assertNull(dataSource.getOAuthState())
  }

  private fun createDataSource(): AuthLocalDataSource {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    val database = AppDatabase(driver)
    return AuthLocalDataSource(database.authSessionQueries)
  }

  private fun sampleSession(sub: String): AuthSession =
      AuthSession(
          sub = sub,
          email = "$sub@example.com",
          displayName = "Player",
          provider = "Google",
          idToken = "id-token-$sub",
          accessToken = "access-token-$sub",
          refreshToken = "refresh-token-$sub",
          expiresAtEpochSeconds = 4102444800,
      )
}
