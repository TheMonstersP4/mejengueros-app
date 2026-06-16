package io.github.themonstersp4.mejengueros.presentation.pokedex

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailViewModelTest {
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
  fun initLoadsPokemonDetail() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository(detail = bulbasaurDetail)
        val viewModel = PokemonDetailViewModel(pokemonId = 1, pokemonRepository = repository)

        advanceUntilIdle()

        assertEquals(bulbasaurDetail, viewModel.uiState.value.pokemon)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1), repository.detailRequests)
      }

  @Test
  fun toggleFavoriteUpdatesLoadedPokemon() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository(detail = bulbasaurDetail)
        val viewModel = PokemonDetailViewModel(pokemonId = 1, pokemonRepository = repository)
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(bulbasaurDetail.copy(isFavorite = true), viewModel.uiState.value.pokemon)
      }

  @Test
  fun loadPokemonShowsErrorWhenRepositoryFails() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository(failure = IllegalStateException("not found"))
        val viewModel = PokemonDetailViewModel(pokemonId = 25, pokemonRepository = repository)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pokemon)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("not found", viewModel.uiState.value.errorMessage)
      }

  @Test
  fun retryLoadsPokemonAfterFailure() =
      runTest(dispatcher) {
        val repository = FakePokemonRepository(failFirstRequest = true, detail = bulbasaurDetail)
        val viewModel = PokemonDetailViewModel(pokemonId = 1, pokemonRepository = repository)
        advanceUntilIdle()

        viewModel.loadPokemon()
        advanceUntilIdle()

        assertEquals(bulbasaurDetail, viewModel.uiState.value.pokemon)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(1, 1), repository.detailRequests)
      }

  private class FakePokemonRepository(
      private val detail: PokemonDetail = bulbasaurDetail,
      private val failure: Throwable? = null,
      private val failFirstRequest: Boolean = false,
  ) : IPokemonRepository {
    val detailRequests = mutableListOf<Int>()
    private var requestCount = 0

    override suspend fun getPokemonPage(limit: Int, offset: Int, query: String): PokemonPage =
        error("Not used")

    override suspend fun getPokemonDetail(id: Int): PokemonDetail {
      detailRequests.add(id)
      requestCount++
      if (failFirstRequest && requestCount == 1) throw IllegalStateException("not found")
      failure?.let { throw it }
      return detail
    }

    override suspend fun getFavoritePokemonSummaries() =
        emptyList<io.github.themonstersp4.mejengueros.domain.model.PokemonSummary>()

    override suspend fun toggleFavorite(id: Int): Boolean = true

    override suspend fun isFavorite(id: Int): Boolean = id == 1
  }

  private companion object {
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
