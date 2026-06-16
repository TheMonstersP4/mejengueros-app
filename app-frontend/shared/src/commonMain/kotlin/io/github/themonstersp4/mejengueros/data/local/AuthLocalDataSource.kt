package io.github.themonstersp4.mejengueros.data.local

import io.github.themonstersp4.mejengueros.domain.model.AuthSession

class AuthLocalDataSource(private val queries: AuthSessionQueries) : IAuthLocalDataSource {
  override fun getSession(): AuthSession? =
      queries.selectSession().executeAsOneOrNull()?.let { row ->
        AuthSession(
            sub = row.sub,
            email = row.email,
            displayName = row.displayName,
            provider = row.provider,
            idToken = row.idToken,
            accessToken = row.accessToken,
            refreshToken = row.refreshToken,
            expiresAtEpochSeconds = row.expiresAtEpochSeconds,
        )
      }

  override fun saveSession(session: AuthSession) {
    queries.upsertSession(
        sub = session.sub,
        email = session.email,
        displayName = session.displayName,
        provider = session.provider,
        idToken = session.idToken,
        accessToken = session.accessToken,
        refreshToken = session.refreshToken,
        expiresAtEpochSeconds = session.expiresAtEpochSeconds,
    )
  }

  override fun clearSession() {
    queries.clearSession()
  }

  override fun getOAuthState(): PendingOAuthState? =
      queries.selectOAuthState().executeAsOneOrNull()?.let { row ->
        PendingOAuthState(state = row.state, codeVerifier = row.codeVerifier)
      }

  override fun saveOAuthState(state: PendingOAuthState) {
    queries.upsertOAuthState(state = state.state, codeVerifier = state.codeVerifier)
  }

  override fun clearOAuthState() {
    queries.clearOAuthState()
  }
}
