package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourtFavoriteLocalDataSourceTest {
  @Test
  fun favoriteWritesAreIdempotentAndIsolatedByUserAndCourt() {
    withDataSource { dataSource ->
      dataSource.setFavorite("user-a", "court-1", true)
      dataSource.setFavorite("user-a", "court-1", true)
      dataSource.setFavorite("user-b", "court-1", true)

      assertTrue(dataSource.isFavorite("user-a", "court-1"))
      assertTrue(dataSource.isFavorite("user-b", "court-1"))
      assertFalse(dataSource.isFavorite("user-a", "court-2"))
    }
  }

  @Test
  fun removingFavoriteOnlyDeletesTheRequestedUserAndCourtPair() {
    withDataSource { dataSource ->
      dataSource.setFavorite("user-a", "court-1", true)
      dataSource.setFavorite("user-a", "court-2", true)
      dataSource.setFavorite("user-b", "court-1", true)

      dataSource.setFavorite("user-a", "court-1", false)

      assertFalse(dataSource.isFavorite("user-a", "court-1"))
      assertTrue(dataSource.isFavorite("user-a", "court-2"))
      assertTrue(dataSource.isFavorite("user-b", "court-1"))
    }
  }

  private fun withDataSource(block: (CourtFavoriteLocalDataSource) -> Unit) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      AppDatabase.Schema.create(driver)
      val database = AppDatabase(driver)
      block(CourtFavoriteLocalDataSource(database.courtFavoriteQueries))
    } finally {
      driver.close()
    }
  }
}
