package io.github.themonstersp4.mejengueros.domain.repository

interface ICourtFavoriteRepository {
  suspend fun isFavorite(userId: String, courtId: String): Boolean

  suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean)
}
