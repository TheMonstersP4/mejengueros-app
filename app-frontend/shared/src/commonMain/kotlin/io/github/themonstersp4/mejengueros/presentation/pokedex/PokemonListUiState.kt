package io.github.themonstersp4.mejengueros.presentation.pokedex

import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary

enum class PokemonListMode {
  All,
  Favorites,
}

data class PokemonListUiState(
    val items: List<PokemonSummary> = emptyList(),
    val searchQuery: String = "",
    val mode: PokemonListMode = PokemonListMode.All,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val nextOffset: Int? = 0,
    val errorMessage: String? = null,
) {
  val visibleItems: List<PokemonSummary> = items
  val isFavoritesMode: Boolean = mode == PokemonListMode.Favorites
  val isSearching: Boolean = mode == PokemonListMode.All && searchQuery.isNotBlank()
  val isEmpty: Boolean = items.isEmpty() && !isLoading
  val endReached: Boolean = nextOffset == null
}
