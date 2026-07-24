package io.github.themonstersp4.mejengueros.data.local

class CourtFavoriteLocalDataSource(
    private val queries: CourtFavoriteQueries,
) : ICourtFavoriteLocalDataSource {
  override fun isFavorite(userId: String, courtId: String): Boolean =
      queries.isCourtFavorite(userId, courtId).executeAsOne()

  override fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    if (isFavorite) {
      queries.upsertCourtFavorite(userId, courtId)
    } else {
      queries.deleteCourtFavorite(userId, courtId)
    }
  }
}
