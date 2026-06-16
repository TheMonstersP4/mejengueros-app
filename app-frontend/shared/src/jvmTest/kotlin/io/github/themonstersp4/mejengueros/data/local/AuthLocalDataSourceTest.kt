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
  fun saveSessionPersistsSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(AuthSession(username = "stored-user"))

    assertEquals(AuthSession(username = "stored-user"), dataSource.getSession())
  }

  @Test
  fun saveSessionReplacesExistingSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(AuthSession(username = "first-user"))
    dataSource.saveSession(AuthSession(username = "second-user"))

    assertEquals(AuthSession(username = "second-user"), dataSource.getSession())
  }

  @Test
  fun clearSessionRemovesPersistedSession() {
    val dataSource = createDataSource()

    dataSource.saveSession(AuthSession(username = "stored-user"))
    dataSource.clearSession()

    assertNull(dataSource.getSession())
  }

  private fun createDataSource(): AuthLocalDataSource {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    val database = AppDatabase(driver)
    return AuthLocalDataSource(database.authSessionQueries)
  }
}
