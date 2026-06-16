package io.github.themonstersp4.mejengueros.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlinx.serialization.json.Json

@Suppress("DEPRECATION")
class AndroidAuthSecureStorage(context: Context, private val json: Json) : IAuthSecureStorage {
  private val preferences =
      EncryptedSharedPreferences.create(
          context,
          PreferencesName,
          MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )

  override suspend fun getSession(): AuthSession? =
      preferences.getString(SessionKey, null)?.let { json.decodeFromString<AuthSession>(it) }

  override suspend fun saveSession(session: AuthSession) {
    preferences.edit().putString(SessionKey, json.encodeToString(session)).apply()
  }

  override suspend fun clearSession() {
    preferences.edit().remove(SessionKey).apply()
  }

  override suspend fun getOAuthState(): PendingOAuthState? =
      preferences.getString(OAuthStateKey, null)?.let {
        json.decodeFromString<PendingOAuthState>(it)
      }

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    preferences.edit().putString(OAuthStateKey, json.encodeToString(state)).apply()
  }

  override suspend fun clearOAuthState() {
    preferences.edit().remove(OAuthStateKey).apply()
  }

  private companion object {
    const val PreferencesName = "mejengueros_auth_secure_storage"
    const val SessionKey = "auth_session"
    const val OAuthStateKey = "oauth_state"
  }
}
