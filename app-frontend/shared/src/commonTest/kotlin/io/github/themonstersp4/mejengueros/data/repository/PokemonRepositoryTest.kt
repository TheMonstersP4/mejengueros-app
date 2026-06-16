package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.IPokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class PokemonRepositoryTest {

  @Test
  fun getPokemonPageSavesRemoteResults() = runTest {
    val remote = FakePokemonRemoteDataSource(page = PokemonPage(listOf(bulbasaur), nextOffset = 20))
    val local = FakePokemonLocalDataSource()
    val repository = PokemonRepository(remote, local)

    val page = repository.getPokemonPage(limit = 20, offset = 0)

    assertEquals(listOf(bulbasaur), page.items)
    assertEquals(20, page.nextOffset)
    assertEquals(listOf(bulbasaur), local.summaries)
  }

  @Test
  fun getPokemonPageWithQueryUsesRemoteResultsAndPreservesFavoriteState() = runTest {
    val remote =
        FakePokemonRemoteDataSource(page = PokemonPage(listOf(bulbasaur), nextOffset = 100))
    val local = FakePokemonLocalDataSource(favoriteIds = mutableSetOf(1))
    val repository = PokemonRepository(remote, local)

    val page = repository.getPokemonPage(limit = 20, offset = 0, query = "bulba")

    assertEquals(listOf(bulbasaur.copy(isFavorite = true)), page.items)
    assertEquals(100, page.nextOffset)
    assertEquals(listOf(bulbasaur), local.summaries)
    assertEquals(Triple(20, 0, "bulba"), remote.pageRequests.single())
  }

  @Test
  fun getPokemonPageWithQueryTrimsSpacesBeforeRemoteSearch() = runTest {
    val remote =
        FakePokemonRemoteDataSource(page = PokemonPage(listOf(bulbasaur), nextOffset = null))
    val local = FakePokemonLocalDataSource()
    val repository = PokemonRepository(remote, local)

    repository.getPokemonPage(limit = 20, offset = 0, query = " bulba ")

    assertEquals(Triple(20, 0, "bulba"), remote.pageRequests.single())
  }

  @Test
  fun getPokemonPageFallsBackToCacheWhenRemoteFails() = runTest {
    val remote = FakePokemonRemoteDataSource(pageFailure = IllegalStateException("network down"))
    val local = FakePokemonLocalDataSource(summaries = mutableListOf(bulbasaur))
    val repository = PokemonRepository(remote, local)

    val page = repository.getPokemonPage(limit = 20, offset = 0)

    assertEquals(listOf(bulbasaur), page.items)
    assertEquals(null, page.nextOffset)
  }

  @Test
  fun getFavoritePokemonSummariesUsesLocalDataSourceOnly() = runTest {
    val remote = FakePokemonRemoteDataSource(pageFailure = IllegalStateException("remote unused"))
    val local =
        FakePokemonLocalDataSource(favoriteSummaries = listOf(bulbasaur.copy(isFavorite = true)))
    val repository = PokemonRepository(remote, local)

    val favorites = repository.getFavoritePokemonSummaries()

    assertEquals(listOf(bulbasaur.copy(isFavorite = true)), favorites)
    assertEquals(emptyList(), remote.pageRequests)
  }

  @Test
  fun toggleFavoritePersistsNewFavoriteValue() = runTest {
    val local = FakePokemonLocalDataSource()
    val repository = PokemonRepository(FakePokemonRemoteDataSource(), local)

    val isFavorite = repository.toggleFavorite(1)

    assertEquals(true, isFavorite)
    assertEquals(setOf(1), local.favoriteIds)
  }

  @Test
  fun getPokemonDetailSavesRemoteDetail() = runTest {
    val remote = FakePokemonRemoteDataSource(detail = bulbasaurDetail)
    val local = FakePokemonLocalDataSource()
    val repository = PokemonRepository(remote, local)

    val detail = repository.getPokemonDetail(1)

    assertEquals(bulbasaurDetail, detail)
    assertEquals(bulbasaurDetail, local.details[1])
  }

  @Test
  fun getPokemonDetailFallsBackToCacheWhenRemoteFails() = runTest {
    val remote = FakePokemonRemoteDataSource(detailFailure = IllegalStateException("network down"))
    val local = FakePokemonLocalDataSource(details = mutableMapOf(1 to bulbasaurDetail))
    val repository = PokemonRepository(remote, local)

    val detail = repository.getPokemonDetail(1)

    assertEquals(bulbasaurDetail, detail)
  }

  private class FakePokemonRemoteDataSource(
      private val page: PokemonPage = PokemonPage(emptyList(), nextOffset = null),
      private val detail: PokemonDetail = bulbasaurDetail,
      private val pageFailure: Throwable? = null,
      private val detailFailure: Throwable? = null,
  ) : IPokemonRemoteDataSource {
    val pageRequests = mutableListOf<Triple<Int, Int, String>>()

    override suspend fun getPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
      pageRequests.add(Triple(limit, offset, query))
      pageFailure?.let { throw it }
      return page
    }

    override suspend fun getPokemonDetail(id: Int): PokemonDetail {
      detailFailure?.let { throw it }
      return detail
    }
  }

  private class FakePokemonLocalDataSource(
      val summaries: MutableList<PokemonSummary> = mutableListOf(),
      val favoriteSummaries: List<PokemonSummary> = emptyList(),
      val details: MutableMap<Int, PokemonDetail> = mutableMapOf(),
      val favoriteIds: MutableSet<Int> = mutableSetOf(),
  ) : IPokemonLocalDataSource {
    override fun getPokemonSummaries(limit: Int, offset: Int): List<PokemonSummary> =
        summaries.drop(offset).take(limit)

    override fun savePokemonSummaries(items: List<PokemonSummary>) {
      summaries.addAll(items)
    }

    override fun getCachedPokemonCount(): Int = summaries.size

    override fun getFavoritePokemonSummaries(): List<PokemonSummary> = favoriteSummaries

    override fun getPokemonDetail(id: Int): PokemonDetail? = details[id]

    override fun savePokemonDetail(detail: PokemonDetail) {
      details[detail.id] = detail
    }

    override fun isFavorite(id: Int): Boolean = id in favoriteIds

    override fun setFavorite(id: Int, isFavorite: Boolean) {
      if (isFavorite) {
        favoriteIds.add(id)
      } else {
        favoriteIds.remove(id)
      }
    }
  }

  private companion object {
    val bulbasaur =
        PokemonSummary(
            id = 1,
            name = "bulbasaur",
            imageUrl = "https://example.com/bulbasaur.png",
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
