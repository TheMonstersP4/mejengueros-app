package io.github.themonstersp4.mejengueros.presentation.pokedex

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail

data class PokemonDetailUiState(
    val pokemon: PokemonDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
