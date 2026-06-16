package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary

interface IPokemonRepository {
  suspend fun getPokemonPage(limit: Int, offset: Int, query: String = ""): PokemonPage

  suspend fun getPokemonDetail(id: Int): PokemonDetail

  suspend fun getFavoritePokemonSummaries(): List<PokemonSummary>

  suspend fun toggleFavorite(id: Int): Boolean

  suspend fun isFavorite(id: Int): Boolean
}
