package io.github.themonstersp4.mejengueros.data.local

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary

interface IPokemonLocalDataSource {
  fun getPokemonSummaries(limit: Int, offset: Int): List<PokemonSummary>

  fun savePokemonSummaries(items: List<PokemonSummary>)

  fun getCachedPokemonCount(): Int

  fun getFavoritePokemonSummaries(): List<PokemonSummary>

  fun getPokemonDetail(id: Int): PokemonDetail?

  fun savePokemonDetail(detail: PokemonDetail)

  fun isFavorite(id: Int): Boolean

  fun setFavorite(id: Int, isFavorite: Boolean)
}
