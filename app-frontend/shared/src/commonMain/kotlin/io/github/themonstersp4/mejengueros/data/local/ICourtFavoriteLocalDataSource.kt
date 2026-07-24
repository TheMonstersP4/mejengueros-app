package io.github.themonstersp4.mejengueros.data.local

interface ICourtFavoriteLocalDataSource {
  fun isFavorite(userId: String, courtId: String): Boolean

  fun setFavorite(userId: String, courtId: String, isFavorite: Boolean)
}
