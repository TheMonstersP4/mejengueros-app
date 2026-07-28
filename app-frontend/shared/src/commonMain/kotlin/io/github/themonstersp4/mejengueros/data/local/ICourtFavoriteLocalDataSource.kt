package io.github.themonstersp4.mejengueros.data.local

import kotlinx.coroutines.flow.Flow

interface ICourtFavoriteLocalDataSource {
  fun isFavorite(userId: String, courtId: String): Boolean

  fun setFavorite(userId: String, courtId: String, isFavorite: Boolean)

  fun observeFavoriteCourtIds(userId: String): Flow<List<String>>
}
