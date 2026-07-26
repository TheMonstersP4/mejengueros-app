package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class CourtFavoriteLocalDataSource
private constructor(
    private val queries: CourtFavoriteQueries,
    private val onListenerRegistered: ((CourtFavoriteLocalDataSource) -> Unit)?,
) : ICourtFavoriteLocalDataSource {
  constructor(queries: CourtFavoriteQueries) : this(queries, null)

  internal companion object {
    fun withListenerRegistrationHook(
        queries: CourtFavoriteQueries,
        onListenerRegistered: (CourtFavoriteLocalDataSource) -> Unit,
    ) = CourtFavoriteLocalDataSource(queries, onListenerRegistered)
  }

  override fun isFavorite(userId: String, courtId: String): Boolean =
      queries.isCourtFavorite(userId, courtId).executeAsOne()

  override fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    if (isFavorite) {
      queries.upsertCourtFavorite(userId, courtId)
    } else {
      queries.deleteCourtFavorite(userId, courtId)
    }
  }

  override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> =
      callbackFlow {
            val query = queries.selectFavoriteCourtIds(userId)
            val listener = Query.Listener { trySend(query.executeAsList()) }

            query.addListener(listener)
            onListenerRegistered?.invoke(this@CourtFavoriteLocalDataSource)
            trySend(query.executeAsList())
            awaitClose { query.removeListener(listener) }
          }
          .distinctUntilChanged()
}
