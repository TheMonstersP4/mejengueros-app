package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class JsonBackedAuthSecureStorage(
    private val json: Json,
    private val store: AuthSecureStringStore,
) : IAuthSecureStorage {
  override suspend fun getSession(): AuthSession? =
      readDecoded(SessionKey) { json.decodeFromString<AuthSession>(it) }

  override suspend fun saveSession(session: AuthSession) {
    writeEncoded(SessionKey, json.encodeToString(session), "auth session")
  }

  override suspend fun clearSession() {
    store.delete(SessionKey)
  }

  override suspend fun getOAuthState(): PendingOAuthState? =
      readDecoded(OAuthStateKey) { json.decodeFromString<PendingOAuthState>(it) }

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    writeEncoded(OAuthStateKey, json.encodeToString(state), "OAuth state")
  }

  override suspend fun clearOAuthState() {
    store.delete(OAuthStateKey)
  }

  private inline fun <T> readDecoded(key: String, decode: (String) -> T): T? {
    val payload =
        when (val result = store.read(key)) {
          AuthSecureStringReadResult.Missing -> return null
          is AuthSecureStringReadResult.Failure -> return null
          is AuthSecureStringReadResult.Value -> result.payload
        }

    return try {
      decode(payload)
    } catch (_: IllegalArgumentException) {
      store.delete(key)
      null
    } catch (_: SerializationException) {
      store.delete(key)
      null
    }
  }

  private fun writeEncoded(key: String, payload: String, subject: String) {
    when (store.write(key, payload)) {
      AuthSecureStringWriteResult.Success -> Unit
      is AuthSecureStringWriteResult.Failure -> {
        throw AuthSecureStorageWriteException("Failed to securely persist $subject.")
      }
    }
  }

  internal companion object {
    const val SessionKey = "auth_session"
    const val OAuthStateKey = "oauth_state"
  }
}

internal class AuthSecureStorageWriteException(message: String) : IllegalStateException(message)

internal sealed interface AuthSecureStringReadResult {
  data object Missing : AuthSecureStringReadResult

  data class Value(val payload: String) : AuthSecureStringReadResult

  data class Failure(val operation: String, val status: Int? = null) : AuthSecureStringReadResult
}

internal sealed interface AuthSecureStringWriteResult {
  data object Success : AuthSecureStringWriteResult

  data class Failure(val operation: String, val status: Int? = null) : AuthSecureStringWriteResult
}

internal interface AuthSecureStringStore {
  fun read(key: String): AuthSecureStringReadResult

  fun write(key: String, value: String): AuthSecureStringWriteResult

  fun delete(key: String)
}
