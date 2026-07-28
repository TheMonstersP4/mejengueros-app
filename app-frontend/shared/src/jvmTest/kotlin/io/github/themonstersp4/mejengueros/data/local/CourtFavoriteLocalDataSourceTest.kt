package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class CourtFavoriteLocalDataSourceTest {
  @Test
  fun favoriteWritesAreIdempotentAndIsolatedByUserAndCourt() = runTest {
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
  fun removingFavoriteOnlyDeletesTheRequestedUserAndCourtPair() = runTest {
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

  @Test
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun observeFavoriteCourtIdsEmitsDeterministicUpdatesForOnlyTheRequestedUser() = runTest {
    withDataSource { dataSource ->
      val emissions = mutableListOf<List<String>>()
      val job = launch { dataSource.observeFavoriteCourtIds("user-a").take(4).toList(emissions) }
      runCurrent()

      dataSource.setFavorite("user-a", "court-z", true)
      runCurrent()
      dataSource.setFavorite("user-b", "court-a", true)
      runCurrent()
      dataSource.setFavorite("user-a", "court-a", true)
      runCurrent()
      dataSource.setFavorite("user-a", "court-z", false)
      job.join()

      assertEquals(
          listOf(emptyList(), listOf("court-z"), listOf("court-a", "court-z"), listOf("court-a")),
          emissions,
      )
    }
  }

  @Test
  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun observeFavoriteCourtIdsDoesNotLoseMutationDuringSubscription() = runTest {
    withDataSource(onListenerRegistered = { it.setFavorite("user-a", "court-1", true) }) {
        dataSource ->
      val emissions = mutableListOf<List<String>>()
      val job = launch { dataSource.observeFavoriteCourtIds("user-a").take(1).toList(emissions) }

      runCurrent()
      job.join()

      assertEquals(listOf(listOf("court-1")), emissions)
    }
  }

  @Test
  fun favoritesPersistAcrossDataSourceInstancesUsingTheSameDatabase() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      AppDatabase.Schema.create(driver)
      CourtFavoriteLocalDataSource(AppDatabase(driver).courtFavoriteQueries)
          .setFavorite("user-a", "court-1", true)

      assertTrue(
          CourtFavoriteLocalDataSource(AppDatabase(driver).courtFavoriteQueries)
              .isFavorite("user-a", "court-1")
      )
    } finally {
      driver.close()
    }
  }

  private suspend fun withDataSource(
      onListenerRegistered: ((CourtFavoriteLocalDataSource) -> Unit)? = null,
      block: suspend (CourtFavoriteLocalDataSource) -> Unit,
  ) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      AppDatabase.Schema.create(driver)
      val database = AppDatabase(driver)
      val dataSource =
          onListenerRegistered?.let {
            CourtFavoriteLocalDataSource.withListenerRegistrationHook(
                database.courtFavoriteQueries,
                it,
            )
          } ?: CourtFavoriteLocalDataSource(database.courtFavoriteQueries)
      block(dataSource)
    } finally {
      driver.close()
    }
  }
}
