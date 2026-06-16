package io.github.themonstersp4.mejengueros.presentation.pokedex

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsFirstPage() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository()
        val viewModel = PokemonListViewModel(repository)

        advanceUntilIdle()

        assertEquals(listOf(bulbasaur), viewModel.uiState.value.items)
        assertEquals(20, viewModel.uiState.value.nextOffset)
        assertEquals(
            listOf(PageRequest(limit = 20, offset = 0, query = "")),
            repository.pageRequests,
        )
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
      }

  @Test
  fun loadNextPageDoesNotStartDuplicateRequestWhileLoading() =
      runTest(dispatcher) {
        val firstPage = CompletableDeferred<PokemonPage>()
        val repository = FakePokemonRepository(deferredPages = mutableListOf(firstPage))
        val viewModel = PokemonListViewModel(repository)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, repository.pageRequests.size)
        assertTrue(viewModel.uiState.value.isLoading)

        firstPage.complete(PokemonPage(items = listOf(bulbasaur), nextOffset = 20))
        advanceUntilIdle()

        assertEquals(listOf(bulbasaur), viewModel.uiState.value.items)
      }

  @Test
  fun loadNextPageAppendsNextPage() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(bulbasaur, ivysaur), viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.nextOffset)
      }

  @Test
  fun syncFavoriteStatesUpdatesLoadedItemsFromRepository() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur, ivysaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.syncFavoriteStates()
        advanceUntilIdle()

        assertEquals(
            listOf(bulbasaur.copy(isFavorite = true), ivysaur.copy(isFavorite = false)),
            viewModel.uiState.value.items,
        )
      }

  @Test
  fun refreshReplacesItemsWithFirstPageForCurrentQuery() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = 20),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("ivy")
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(ivysaur), viewModel.uiState.value.items)
        assertEquals(
            PageRequest(limit = 20, offset = 0, query = "ivy"),
            repository.pageRequests.last(),
        )
        assertFalse(viewModel.uiState.value.isRefreshing)
      }

  @Test
  fun refreshDoesNotStartWhileLoading() =
      runTest(dispatcher) {
        val firstPage = CompletableDeferred<PokemonPage>()
        val repository = FakePokemonRepository(deferredPages = mutableListOf(firstPage))
        val viewModel = PokemonListViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.pageRequests.size)
        firstPage.complete(PokemonPage(items = listOf(bulbasaur), nextOffset = 20))
        advanceUntilIdle()
      }

  @Test
  fun toggleFavoriteUpdatesLoadedPokemon() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository()
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleFavorite(1)
        advanceUntilIdle()

        assertEquals(listOf(bulbasaur.copy(isFavorite = true)), viewModel.uiState.value.items)
      }

  @Test
  fun rapidSearchChangesOnlyLoadLatestQueryAfterDebounce() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("i")
        advanceTimeBy(100)
        viewModel.updateSearchQuery("iv")
        advanceTimeBy(100)
        viewModel.updateSearchQuery("ivy")
        advanceTimeBy(299)

        assertEquals("ivy", viewModel.uiState.value.searchQuery)
        assertEquals(1, repository.pageRequests.size)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf(ivysaur), viewModel.uiState.value.items)
        assertEquals(
            listOf(
                PageRequest(limit = 20, offset = 0, query = ""),
                PageRequest(limit = 20, offset = 0, query = "ivy"),
            ),
            repository.pageRequests,
        )
      }

  @Test
  fun updateSearchQueryTriggersRemoteSearchAndReplacesItems() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur, ivysaur), nextOffset = 20),
                        PokemonPage(items = listOf(bulbasaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("bulba")
        advanceTimeBy(299)

        assertEquals("bulba", viewModel.uiState.value.searchQuery)
        assertEquals(1, repository.pageRequests.size)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("bulba", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(bulbasaur), viewModel.uiState.value.items)
        assertEquals(viewModel.uiState.value.items, viewModel.uiState.value.visibleItems)
        assertEquals(
            PageRequest(limit = 20, offset = 0, query = "bulba"),
            repository.pageRequests.last(),
        )
      }

  @Test
  fun repeatedSameSearchQueryDoesNotScheduleDuplicateWork() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("ivy")
        advanceUntilIdle()
        val requestCount = repository.pageRequests.size

        viewModel.updateSearchQuery("ivy")
        advanceUntilIdle()

        assertEquals(requestCount, repository.pageRequests.size)
        assertEquals(listOf(ivysaur), viewModel.uiState.value.items)
      }

  @Test
  fun emptySearchResultKeepsSearchQueryAvailableForUi() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = emptyList(), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("missing")
        advanceUntilIdle()

        assertEquals("missing", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isSearching)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.nextOffset)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun clearSearchQueryReloadsDefaultFirstPage() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = null),
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("ivy")
        advanceUntilIdle()
        viewModel.updateSearchQuery("")
        advanceTimeBy(299)

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(2, repository.pageRequests.size)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(bulbasaur), viewModel.uiState.value.items)
        assertEquals(
            PageRequest(limit = 20, offset = 0, query = ""),
            repository.pageRequests.last(),
        )
      }

  @Test
  fun loadNextPageWhileSearchingAppendsNextQueryPage() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 100),
                        PokemonPage(items = listOf(ivysaur), nextOffset = null),
                    )
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("saur")
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(bulbasaur, ivysaur), viewModel.uiState.value.items)
        assertEquals(
            PageRequest(limit = 20, offset = 100, query = "saur"),
            repository.pageRequests.last(),
        )
      }

  @Test
  fun searchFailureKeepsQueryClearsLoadingFlagsAndShowsError() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository()
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        repository.failure = IllegalStateException("search down")
        viewModel.updateSearchQuery("missing")
        advanceUntilIdle()

        assertEquals("missing", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("search down", viewModel.uiState.value.errorMessage)
      }

  @Test
  fun cancelledSearchDoesNotOverwriteCurrentQueryResults() =
      runTest(dispatcher) {
        val slowSearch = CompletableDeferred<PokemonPage>()
        val repository =
            FakePokemonRepository(
                pages = mutableListOf(PokemonPage(items = listOf(bulbasaur), nextOffset = 20)),
                deferredPages = mutableListOf(slowSearch),
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("b")
        advanceUntilIdle()
        repository.pages += PokemonPage(items = listOf(ivysaur), nextOffset = null)
        viewModel.updateSearchQuery("ivy")
        advanceUntilIdle()
        slowSearch.complete(PokemonPage(items = listOf(bulbasaur), nextOffset = null))
        advanceUntilIdle()

        assertEquals("ivy", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(ivysaur), viewModel.uiState.value.items)
      }

  @Test
  fun switchingToFavoritesLoadsLocalFavoritesAndDoesNotPageRemote() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(favoriteItems = listOf(bulbasaur.copy(isFavorite = true)))
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()

        viewModel.setMode(PokemonListMode.Favorites)
        advanceUntilIdle()

        assertEquals(PokemonListMode.Favorites, viewModel.uiState.value.mode)
        assertEquals(listOf(bulbasaur.copy(isFavorite = true)), viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.nextOffset)
        assertEquals(1, repository.favoriteRequests)
        assertEquals(1, repository.pageRequests.size)
      }

  @Test
  fun loadNextPageDoesNothingInFavoritesMode() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(favoriteItems = listOf(bulbasaur.copy(isFavorite = true)))
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()
        viewModel.setMode(PokemonListMode.Favorites)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, repository.pageRequests.size)
        assertEquals(1, repository.favoriteRequests)
      }

  @Test
  fun unfavoriteInFavoritesModeRemovesItemFromGrid() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                favoriteItems =
                    listOf(bulbasaur.copy(isFavorite = true), ivysaur.copy(isFavorite = true)),
                favoriteToggleResults = mutableMapOf(1 to false),
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()
        viewModel.setMode(PokemonListMode.Favorites)
        advanceUntilIdle()

        viewModel.toggleFavorite(1)
        advanceUntilIdle()

        assertEquals(listOf(ivysaur.copy(isFavorite = true)), viewModel.uiState.value.items)
      }

  @Test
  fun switchingBackToAllReloadsNormalFirstPage() =
      runTest(dispatcher) {
        val repository =
            FakePokemonRepository(
                pages =
                    mutableListOf(
                        PokemonPage(items = listOf(bulbasaur), nextOffset = 20),
                        PokemonPage(items = listOf(ivysaur), nextOffset = 20),
                    ),
                favoriteItems = listOf(bulbasaur.copy(isFavorite = true)),
            )
        val viewModel = PokemonListViewModel(repository)
        advanceUntilIdle()
        viewModel.setMode(PokemonListMode.Favorites)
        advanceUntilIdle()

        viewModel.setMode(PokemonListMode.All)
        advanceUntilIdle()

        assertEquals(PokemonListMode.All, viewModel.uiState.value.mode)
        assertEquals(listOf(ivysaur), viewModel.uiState.value.items)
        assertEquals(
            PageRequest(limit = 20, offset = 0, query = ""),
            repository.pageRequests.last(),
        )
      }

  @Test
  fun loadFailureShowsError() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository(failure = IllegalStateException("network down"))
        val viewModel = PokemonListViewModel(repository)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals("network down", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  private class FakePokemonRepository(
      val pages: MutableList<PokemonPage> =
          mutableListOf(PokemonPage(items = listOf(bulbasaur), nextOffset = 20)),
      private val deferredPages: MutableList<CompletableDeferred<PokemonPage>> = mutableListOf(),
      private val favoriteItems: List<PokemonSummary> = emptyList(),
      private val favoriteToggleResults: MutableMap<Int, Boolean> = mutableMapOf(),
      var failure: Throwable? = null,
  ) : IPokemonRepository {
    val pageRequests = mutableListOf<PageRequest>()
    var favoriteRequests = 0

    override suspend fun getPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
      pageRequests.add(PageRequest(limit = limit, offset = offset, query = query))
      failure?.let { throw it }
      if (deferredPages.isNotEmpty()) return deferredPages.removeAt(0).await()
      return pages.removeAt(0)
    }

    override suspend fun getPokemonDetail(id: Int): PokemonDetail = error("Not used")

    override suspend fun getFavoritePokemonSummaries(): List<PokemonSummary> {
      favoriteRequests += 1
      return favoriteItems
    }

    override suspend fun toggleFavorite(id: Int): Boolean = favoriteToggleResults[id] ?: true

    override suspend fun isFavorite(id: Int): Boolean = id == 1
  }

  private data class PageRequest(val limit: Int, val offset: Int, val query: String)

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
  }
}
