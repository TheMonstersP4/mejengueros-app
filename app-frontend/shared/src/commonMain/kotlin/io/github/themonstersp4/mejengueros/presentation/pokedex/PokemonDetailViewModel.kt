package io.github.themonstersp4.mejengueros.presentation.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonDetailViewModel(
    private val pokemonId: Int,
    private val pokemonRepository: IPokemonRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(PokemonDetailUiState(isLoading = true))
  val uiState: StateFlow<PokemonDetailUiState> = _uiState.asStateFlow()

  init {
    loadPokemon()
  }

  fun toggleFavorite() {
    val pokemon = _uiState.value.pokemon ?: return
    viewModelScope.launch {
      runCatching { pokemonRepository.toggleFavorite(pokemon.id) }
          .onSuccess { isFavorite ->
            _uiState.value = _uiState.value.copy(pokemon = pokemon.copy(isFavorite = isFavorite))
          }
    }
  }

  fun loadPokemon() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      runCatching { pokemonRepository.getPokemonDetail(pokemonId) }
          .onSuccess { pokemon ->
            _uiState.value = PokemonDetailUiState(pokemon = pokemon, isLoading = false)
          }
          .onFailure { error ->
            _uiState.value =
                PokemonDetailUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load Pokémon detail.",
                )
          }
    }
  }
}
