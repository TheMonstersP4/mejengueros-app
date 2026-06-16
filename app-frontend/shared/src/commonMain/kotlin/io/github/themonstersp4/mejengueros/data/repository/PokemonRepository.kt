package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.IPokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository

class PokemonRepository(
    private val remoteDataSource: IPokemonRemoteDataSource,
    private val localDataSource: IPokemonLocalDataSource,
) : IPokemonRepository {
  override suspend fun getPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isBlank()) {
      getUnfilteredPokemonPage(limit = limit, offset = offset)
    } else {
      getFilteredPokemonPage(limit = limit, offset = offset, query = normalizedQuery)
    }
  }

  private suspend fun getUnfilteredPokemonPage(limit: Int, offset: Int): PokemonPage =
      runCatching {
            val remotePage = remoteDataSource.getPokemonPage(limit = limit, offset = offset)
            localDataSource.savePokemonSummaries(remotePage.items)
            remotePage.copy(
                items = localDataSource.getPokemonSummaries(limit = limit, offset = offset)
            )
          }
          .getOrElse {
            val cachedItems = localDataSource.getPokemonSummaries(limit = limit, offset = offset)
            PokemonPage(
                items = cachedItems,
                nextOffset =
                    if (
                        cachedItems.size == limit &&
                            localDataSource.getCachedPokemonCount() > offset + limit
                    ) {
                      offset + limit
                    } else {
                      null
                    },
            )
          }

  private suspend fun getFilteredPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
    val remotePage = remoteDataSource.getPokemonPage(limit = limit, offset = offset, query = query)
    localDataSource.savePokemonSummaries(remotePage.items)
    return remotePage.copy(
        items =
            remotePage.items.map { item ->
              item.copy(isFavorite = localDataSource.isFavorite(item.id))
            }
    )
  }

  override suspend fun getPokemonDetail(id: Int): PokemonDetail =
      runCatching {
            val remoteDetail = remoteDataSource.getPokemonDetail(id)
            localDataSource.savePokemonDetail(remoteDetail)
            localDataSource.getPokemonDetail(id) ?: remoteDetail
          }
          .getOrElse { localDataSource.getPokemonDetail(id) ?: throw it }

  override suspend fun getFavoritePokemonSummaries() = localDataSource.getFavoritePokemonSummaries()

  override suspend fun toggleFavorite(id: Int): Boolean {
    val newFavoriteValue = !localDataSource.isFavorite(id)
    localDataSource.setFavorite(id = id, isFavorite = newFavoriteValue)
    return newFavoriteValue
  }

  override suspend fun isFavorite(id: Int): Boolean = localDataSource.isFavorite(id)
}
