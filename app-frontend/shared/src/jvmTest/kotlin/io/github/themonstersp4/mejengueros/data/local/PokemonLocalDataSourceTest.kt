package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PokemonLocalDataSourceTest {

  @Test
  fun getPokemonSummariesReturnsCachedPage() {
    val dataSource = createDataSource()
    dataSource.savePokemonSummaries(listOf(bulbasaur, ivysaur))

    val page = dataSource.getPokemonSummaries(limit = 1, offset = 1)

    assertEquals(listOf(ivysaur), page)
  }

  @Test
  fun setFavoriteMarksSummaryAsFavorite() {
    val dataSource = createDataSource()
    dataSource.savePokemonSummaries(listOf(bulbasaur))

    dataSource.setFavorite(id = 1, isFavorite = true)

    assertTrue(dataSource.isFavorite(1))
    assertEquals(
        listOf(bulbasaur.copy(isFavorite = true)),
        dataSource.getPokemonSummaries(limit = 20, offset = 0),
    )
  }

  @Test
  fun setFavoriteCanRemoveFavorite() {
    val dataSource = createDataSource()

    dataSource.setFavorite(id = 1, isFavorite = true)
    dataSource.setFavorite(id = 1, isFavorite = false)

    assertFalse(dataSource.isFavorite(1))
  }

  @Test
  fun getFavoritePokemonSummariesReturnsOnlyFavorites() {
    val dataSource = createDataSource()
    dataSource.savePokemonSummaries(listOf(bulbasaur, ivysaur))
    dataSource.setFavorite(id = 1, isFavorite = true)

    val favorites = dataSource.getFavoritePokemonSummaries()

    assertEquals(listOf(bulbasaur.copy(isFavorite = true)), favorites)
  }

  @Test
  fun getFavoritePokemonSummariesReturnsEmptyWhenNoFavorites() {
    val dataSource = createDataSource()
    dataSource.savePokemonSummaries(listOf(bulbasaur))

    val favorites = dataSource.getFavoritePokemonSummaries()

    assertEquals(emptyList(), favorites)
  }

  @Test
  fun getFavoritePokemonSummariesCanUseCachedDetailWhenSummaryIsMissing() {
    val dataSource = createDataSource()
    dataSource.savePokemonDetail(bulbasaurDetail)
    dataSource.setFavorite(id = 1, isFavorite = true)

    val favorites = dataSource.getFavoritePokemonSummaries()

    assertEquals(listOf(bulbasaur.copy(isFavorite = true)), favorites)
  }

  @Test
  fun getFavoritePokemonSummariesSkipsFavoriteWithoutCachedData() {
    val dataSource = createDataSource()
    dataSource.setFavorite(id = 99, isFavorite = true)

    val favorites = dataSource.getFavoritePokemonSummaries()

    assertEquals(emptyList(), favorites)
  }

  @Test
  fun getPokemonDetailReturnsNullWhenMissing() {
    val dataSource = createDataSource()

    val detail = dataSource.getPokemonDetail(1)

    assertNull(detail)
  }

  @Test
  fun savePokemonDetailPersistsDetail() {
    val dataSource = createDataSource()

    dataSource.savePokemonDetail(bulbasaurDetail)
    dataSource.setFavorite(id = 1, isFavorite = true)

    assertEquals(bulbasaurDetail.copy(isFavorite = true), dataSource.getPokemonDetail(1))
  }

  private fun createDataSource(): PokemonLocalDataSource {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    val database = AppDatabase(driver)
    return PokemonLocalDataSource(database.pokemonCacheQueries)
  }

  private companion object {
    val bulbasaur =
        PokemonSummary(
            id = 1,
            name = "bulbasaur",
            imageUrl = "https://example.com/bulbasaur.png",
        )
    val ivysaur =
        PokemonSummary(
            id = 2,
            name = "ivysaur",
            imageUrl = "https://example.com/ivysaur.png",
        )
    val bulbasaurDetail =
        PokemonDetail(
            id = 1,
            name = "bulbasaur",
            height = 7,
            weight = 69,
            imageUrl = "https://example.com/bulbasaur.png",
            types = listOf("grass", "poison"),
        )
  }
}
