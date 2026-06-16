package io.github.themonstersp4.mejengueros.data.local

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary

class PokemonLocalDataSource(private val queries: PokemonCacheQueries) : IPokemonLocalDataSource {
  override fun getPokemonSummaries(limit: Int, offset: Int): List<PokemonSummary> =
      queries.selectPokemonSummaries(limit.toLong(), offset.toLong()).executeAsList().map {
        PokemonSummary(
            id = it.id.toInt(),
            name = it.name,
            imageUrl = it.imageUrl,
            isFavorite = it.isFavorite,
        )
      }

  override fun savePokemonSummaries(items: List<PokemonSummary>) {
    queries.transaction {
      items.forEach { item ->
        queries.upsertPokemonSummary(
            id = item.id.toLong(),
            name = item.name,
            imageUrl = item.imageUrl,
        )
      }
    }
  }

  override fun getCachedPokemonCount(): Int =
      queries.selectPokemonSummaryCount().executeAsOne().toInt()

  override fun getFavoritePokemonSummaries(): List<PokemonSummary> =
      queries.selectFavoritePokemonSummaries().executeAsList().map {
        PokemonSummary(
            id = it.id.toInt(),
            name = it.name,
            imageUrl = it.imageUrl,
            isFavorite = true,
        )
      }

  override fun getPokemonDetail(id: Int): PokemonDetail? =
      queries.selectPokemonDetail(id.toLong()).executeAsOneOrNull()?.let {
        PokemonDetail(
            id = it.id.toInt(),
            name = it.name,
            height = it.height.toInt(),
            weight = it.weight.toInt(),
            imageUrl = it.imageUrl,
            types = it.types.split(',').filter(String::isNotBlank),
            isFavorite = it.isFavorite,
        )
      }

  override fun savePokemonDetail(detail: PokemonDetail) {
    queries.upsertPokemonDetail(
        id = detail.id.toLong(),
        name = detail.name,
        height = detail.height.toLong(),
        weight = detail.weight.toLong(),
        imageUrl = detail.imageUrl,
        types = detail.types.joinToString(","),
    )
  }

  override fun isFavorite(id: Int): Boolean = queries.isFavorite(id.toLong()).executeAsOne()

  override fun setFavorite(id: Int, isFavorite: Boolean) {
    if (isFavorite) {
      queries.upsertFavorite(id.toLong())
    } else {
      queries.deleteFavorite(id.toLong())
    }
  }
}
