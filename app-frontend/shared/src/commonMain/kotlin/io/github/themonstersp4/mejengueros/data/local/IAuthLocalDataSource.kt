package io.github.themonstersp4.mejengueros.data.local

import io.github.themonstersp4.mejengueros.domain.model.AuthSession

interface IAuthLocalDataSource {
  fun getSession(): AuthSession?

  fun saveSession(session: AuthSession)

  fun clearSession()

  fun getOAuthState(): PendingOAuthState?

  fun saveOAuthState(state: PendingOAuthState)

  fun clearOAuthState()
}
