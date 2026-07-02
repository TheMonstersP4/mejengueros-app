package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession

class InMemoryAuthSecureStorage : IAuthSecureStorage {
  private var session: AuthSession? = null
  private var oauthState: PendingOAuthState? = null
  private val ownerViewPreferences = mutableMapOf<String, OwnerViewPreference>()

  override suspend fun getSession(): AuthSession? = session

  override suspend fun saveSession(session: AuthSession) {
    this.session = session
  }

  override suspend fun clearSession() {
    session = null
  }

  override suspend fun getOAuthState(): PendingOAuthState? = oauthState

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    oauthState = state
  }

  override suspend fun clearOAuthState() {
    oauthState = null
  }

  override suspend fun getOwnerViewPreference(userId: String): OwnerViewPreference? =
      ownerViewPreferences[userId.trim()]

  override suspend fun saveOwnerViewPreference(userId: String, preference: OwnerViewPreference) {
    ownerViewPreferences[userId.trim()] = preference
  }

  override suspend fun clearOwnerViewPreference(userId: String) {
    ownerViewPreferences.remove(userId.trim())
  }
}
