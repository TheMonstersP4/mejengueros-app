package io.github.themonstersp4.mejengueros.presentation.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonListViewModel(private val pokemonRepository: IPokemonRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(PokemonListUiState())
  val uiState: StateFlow<PokemonListUiState> = _uiState.asStateFlow()

  private var pageLoadJob: Job? = null
  private var searchJob: Job? = null

  init {
    loadNextPage()
  }

  fun setMode(mode: PokemonListMode) {
    if (mode == _uiState.value.mode) return

    pageLoadJob?.cancel()
    searchJob?.cancel()

    when (mode) {
      PokemonListMode.All -> {
        _uiState.value =
            _uiState.value.copy(
                mode = PokemonListMode.All,
                items = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                isRefreshing = false,
                nextOffset = 0,
                errorMessage = null,
            )
        loadPage(
            offset = 0,
            query = _uiState.value.searchQuery,
            replaceItems = true,
            refreshing = false,
        )
      }
      PokemonListMode.Favorites -> loadFavorites(refreshing = false)
    }
  }

  fun updateSearchQuery(query: String) {
    if (_uiState.value.isFavoritesMode || query == _uiState.value.searchQuery) return

    pageLoadJob?.cancel()
    searchJob?.cancel()
    _uiState.value =
        _uiState.value.copy(
            items = emptyList(),
            searchQuery = query,
            isLoading = true,
            isLoadingMore = false,
            isRefreshing = false,
            nextOffset = 0,
            errorMessage = null,
        )
    searchJob =
        viewModelScope.launch {
          delay(SearchDebounceMillis)
          loadPage(
              offset = 0,
              query = query,
              replaceItems = true,
              refreshing = false,
          )
        }
  }

  fun syncFavoriteStates() {
    val state = _uiState.value
    if (state.isFavoritesMode) {
      loadFavorites(refreshing = false)
      return
    }

    val items = state.items
    if (items.isEmpty()) return

    viewModelScope.launch {
      val updatedItems =
          items.map { item -> item.copy(isFavorite = pokemonRepository.isFavorite(item.id)) }
      _uiState.value = _uiState.value.copy(items = updatedItems)
    }
  }

  fun refresh() {
    val state = _uiState.value
    if (state.isLoading || state.isLoadingMore || state.isRefreshing) return

    pageLoadJob?.cancel()
    searchJob?.cancel()

    if (state.isFavoritesMode) {
      loadFavorites(refreshing = true)
    } else {
      loadPage(
          offset = 0,
          query = state.searchQuery,
          replaceItems = true,
          refreshing = true,
      )
    }
  }

  fun toggleFavorite(id: Int) {
    viewModelScope.launch {
      runCatching { pokemonRepository.toggleFavorite(id) }
          .onSuccess { isFavorite ->
            val state = _uiState.value
            _uiState.value =
                state.copy(
                    items =
                        if (state.isFavoritesMode && !isFavorite) {
                          state.items.filterNot { item -> item.id == id }
                        } else {
                          state.items.map { item ->
                            if (item.id == id) item.copy(isFavorite = isFavorite) else item
                          }
                        }
                )
          }
    }
  }

  fun loadNextPage() {
    val state = _uiState.value
    if (state.isFavoritesMode) return

    val offset = state.nextOffset ?: return
    if (state.isLoading || state.isLoadingMore || state.isRefreshing) return

    loadPage(
        offset = offset,
        query = state.searchQuery,
        replaceItems = state.items.isEmpty(),
        refreshing = false,
    )
  }

  private fun loadFavorites(refreshing: Boolean) {
    val state = _uiState.value
    _uiState.value =
        state.copy(
            mode = PokemonListMode.Favorites,
            items = if (refreshing) state.items else emptyList(),
            isLoading = !refreshing,
            isLoadingMore = false,
            isRefreshing = refreshing,
            nextOffset = null,
            errorMessage = null,
        )

    pageLoadJob =
        viewModelScope.launch {
          runCatching { pokemonRepository.getFavoritePokemonSummaries() }
              .onSuccess { favorites ->
                _uiState.value =
                    _uiState.value.copy(
                        items = favorites,
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        nextOffset = null,
                        errorMessage = null,
                    )
              }
              .onFailure { error ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        nextOffset = null,
                        errorMessage = error.message ?: "Unable to load favorite Pokémon.",
                    )
              }
        }
  }

  private fun loadPage(offset: Int, query: String, replaceItems: Boolean, refreshing: Boolean) {
    val state = _uiState.value
    if (state.isFavoritesMode) return

    val isInitialLoad = replaceItems && state.items.isEmpty() && !refreshing
    _uiState.value =
        state.copy(
            mode = PokemonListMode.All,
            isLoading = isInitialLoad,
            isLoadingMore = !replaceItems && !refreshing,
            isRefreshing = refreshing,
            errorMessage = null,
        )

    pageLoadJob =
        viewModelScope.launch {
          runCatching {
                pokemonRepository.getPokemonPage(
                    limit = PageSize,
                    offset = offset,
                    query = query,
                )
              }
              .onSuccess { page ->
                val currentState = _uiState.value
                if (currentState.isFavoritesMode || query != currentState.searchQuery)
                    return@onSuccess

                _uiState.value =
                    currentState.copy(
                        items =
                            if (replaceItems) {
                              page.items
                            } else {
                              (currentState.items + page.items).distinctBy { it.id }
                            },
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        nextOffset = page.nextOffset,
                        errorMessage = null,
                    )
              }
              .onFailure { error ->
                val currentState = _uiState.value
                if (currentState.isFavoritesMode || query != currentState.searchQuery) {
                  return@onFailure
                }

                _uiState.value =
                    currentState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Unable to load Pokémon.",
                    )
              }
        }
  }

  companion object {
    private const val PageSize = 20
    private const val SearchDebounceMillis = 300L
  }
}
