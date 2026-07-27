package io.github.themonstersp4.mejengueros.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ICourtFavoriteRepository {
  suspend fun isFavorite(userId: String, courtId: String): Boolean

  suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean)

  fun observeFavoriteCourtIds(userId: String): Flow<List<String>> = emptyFlow()
}
