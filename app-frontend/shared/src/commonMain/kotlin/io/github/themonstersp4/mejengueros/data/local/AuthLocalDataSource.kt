package io.github.themonstersp4.mejengueros.data.local

import io.github.themonstersp4.mejengueros.domain.model.AuthSession

class AuthLocalDataSource(private val queries: AuthSessionQueries) : IAuthLocalDataSource {
  override fun getSession(): AuthSession? =
      queries.selectSession().executeAsOneOrNull()?.let(::AuthSession)

  override fun saveSession(session: AuthSession) {
    queries.upsertSession(username = session.username)
  }

  override fun clearSession() {
    queries.clearSession()
  }
}
