package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.ICourtFavoriteLocalDataSource
import io.github.themonstersp4.mejengueros.domain.repository.ICourtFavoriteRepository
import kotlinx.coroutines.flow.Flow

class CourtFavoriteRepository(
    private val localDataSource: ICourtFavoriteLocalDataSource,
) : ICourtFavoriteRepository {
  override suspend fun isFavorite(userId: String, courtId: String): Boolean =
      localDataSource.isFavorite(userId, courtId)

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    localDataSource.setFavorite(userId, courtId, isFavorite)
  }

  override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> =
      localDataSource.observeFavoriteCourtIds(userId)
}
