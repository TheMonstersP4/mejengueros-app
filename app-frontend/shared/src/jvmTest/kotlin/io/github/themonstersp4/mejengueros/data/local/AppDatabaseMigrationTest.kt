package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDatabaseMigrationTest {

  @Test
  fun migrationFromVersionOneToFourClearsLegacyAuthSessionAndCreatesPokemonTables() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      createVersionOneSchema(driver)
      driver.execute(
          identifier = null,
          sql = "INSERT INTO AuthSession(id, username) VALUES (1, 'brock')",
          parameters = 0,
      )

      AppDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 4)
      val database = AppDatabase(driver)

      assertEquals(null, database.authSessionQueries.selectSession().executeAsOneOrNull())
      assertEquals(0, database.pokemonCacheQueries.selectPokemonSummaryCount().executeAsOne())
      assertFalse(database.pokemonCacheQueries.isFavorite(1).executeAsOne())

      database.pokemonCacheQueries.upsertPokemonSummary(
          id = 1,
          name = "bulbasaur",
          imageUrl = "https://example.com/bulbasaur.png",
      )
      database.pokemonCacheQueries.upsertPokemonDetail(
          id = 1,
          name = "bulbasaur",
          height = 7,
          weight = 69,
          imageUrl = "https://example.com/bulbasaur.png",
          types = "grass,poison",
      )
      database.pokemonCacheQueries.upsertFavorite(1)

      assertEquals(1, database.pokemonCacheQueries.selectPokemonSummaryCount().executeAsOne())
      assertTrue(database.pokemonCacheQueries.isFavorite(1).executeAsOne())
    } finally {
      driver.close()
    }
  }

  @Test
  fun migrationFromVersionOneCreatesPokemonCacheTables() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      createVersionOneSchema(driver)

      AppDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
      val database = AppDatabase(driver)

      assertEquals(0, database.pokemonCacheQueries.selectPokemonSummaryCount().executeAsOne())
    } finally {
      driver.close()
    }
  }

  @Test
  fun migrationFromVersionTwoToThreePreservesPokemonCacheAndCreatesFavoriteTable() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      createVersionTwoSchema(driver)
      driver.execute(
          identifier = null,
          sql =
              """
              INSERT INTO PokemonSummaryEntity(id, name, imageUrl)
              VALUES (1, 'bulbasaur', 'https://example.com/bulbasaur.png')
              """
                  .trimIndent(),
          parameters = 0,
      )
      driver.execute(
          identifier = null,
          sql =
              """
              INSERT INTO PokemonDetailEntity(id, name, height, weight, imageUrl, types)
              VALUES (1, 'bulbasaur', 7, 69, 'https://example.com/bulbasaur.png', 'grass,poison')
              """
                  .trimIndent(),
          parameters = 0,
      )

      AppDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
      val database = AppDatabase(driver)

      val summary = database.pokemonCacheQueries.selectPokemonSummaries(20, 0).executeAsOne()
      val detail = database.pokemonCacheQueries.selectPokemonDetail(1).executeAsOne()

      assertEquals(1, summary.id)
      assertEquals("bulbasaur", summary.name)
      assertFalse(summary.isFavorite)
      assertEquals(1, detail.id)
      assertEquals("grass,poison", detail.types)
      assertFalse(database.pokemonCacheQueries.isFavorite(1).executeAsOne())

      database.pokemonCacheQueries.upsertFavorite(1)
      assertTrue(database.pokemonCacheQueries.isFavorite(1).executeAsOne())
    } finally {
      driver.close()
    }
  }

  @Test
  fun migrationFromVersionTwoCreatesFavoriteTable() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      createVersionTwoSchema(driver)

      AppDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
      val database = AppDatabase(driver)

      assertFalse(database.pokemonCacheQueries.isFavorite(1).executeAsOne())
    } finally {
      driver.close()
    }
  }

  @Test
  fun migrationFromVersionThreeToFourCreatesNonSensitiveAuthProfileTable() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      createVersionTwoSchema(driver)
      driver.execute(
          identifier = null,
          sql = "CREATE TABLE FavoritePokemonEntity(id INTEGER NOT NULL PRIMARY KEY)",
          parameters = 0,
      )

      AppDatabase.Schema.migrate(driver, oldVersion = 3, newVersion = 4)
      val database = AppDatabase(driver)

      assertEquals(null, database.authSessionQueries.selectSession().executeAsOneOrNull())
    } finally {
      driver.close()
    }
  }

  private fun createVersionOneSchema(driver: JdbcSqliteDriver) {
    driver.execute(
        identifier = null,
        sql =
            """
            CREATE TABLE AuthSession (
                id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                username TEXT NOT NULL
            )
            """
                .trimIndent(),
        parameters = 0,
    )
  }

  private fun createVersionTwoSchema(driver: JdbcSqliteDriver) {
    createVersionOneSchema(driver)
    driver.execute(
        identifier = null,
        sql =
            """
            CREATE TABLE PokemonSummaryEntity(
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              imageUrl TEXT NOT NULL
            )
            """
                .trimIndent(),
        parameters = 0,
    )
    driver.execute(
        identifier = null,
        sql =
            """
            CREATE TABLE PokemonDetailEntity(
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              height INTEGER NOT NULL,
              weight INTEGER NOT NULL,
              imageUrl TEXT NOT NULL,
              types TEXT NOT NULL
            )
            """
                .trimIndent(),
        parameters = 0,
    )
  }
}
