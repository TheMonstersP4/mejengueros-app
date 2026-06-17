package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession

interface IAuthSecureStorage {
  suspend fun getSession(): AuthSession?

  suspend fun saveSession(session: AuthSession)

  suspend fun clearSession()

  suspend fun getOAuthState(): PendingOAuthState?

  suspend fun saveOAuthState(state: PendingOAuthState)

  suspend fun clearOAuthState()
}
