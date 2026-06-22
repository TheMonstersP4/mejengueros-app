package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class JsonBackedAuthSecureStorageTest {
  @Test
  fun saveSessionThenReadRoundTripsThroughStringStore() = runTest {
    val store = FakeAuthSecureStringStore()
    val storage = JsonBackedAuthSecureStorage(Json, store)
    val session = sampleSession()

    storage.saveSession(session)

    assertEquals(session, storage.getSession())
    assertEquals(0, store.deleteCounts[JsonBackedAuthSecureStorage.SessionKey] ?: 0)
  }

  @Test
  fun saveSessionThrowsWhenStringStoreWriteFails() = runTest {
    val store =
        FakeAuthSecureStringStore(
            writeFailures =
                mutableMapOf(
                    JsonBackedAuthSecureStorage.SessionKey to
                        AuthSecureStringWriteResult.Failure(
                            operation = "SecItemUpdate",
                            status = -34018,
                        )
                )
        )
    val storage = JsonBackedAuthSecureStorage(Json, store)

    val error =
        assertFailsWith<AuthSecureStorageWriteException> { storage.saveSession(sampleSession()) }

    assertEquals("Failed to securely persist auth session.", error.message)
    assertEquals(0, store.deleteCounts[JsonBackedAuthSecureStorage.SessionKey] ?: 0)
    assertEquals(
        AuthSecureStringReadResult.Missing,
        store.readWithoutFailures(JsonBackedAuthSecureStorage.SessionKey),
    )
  }

  @Test
  fun clearSessionDeletesStoredValue() = runTest {
    val store = FakeAuthSecureStringStore()
    val storage = JsonBackedAuthSecureStorage(Json, store)

    storage.saveSession(sampleSession())

    storage.clearSession()

    assertNull(storage.getSession())
    assertEquals(1, store.deleteCounts[JsonBackedAuthSecureStorage.SessionKey])
  }

  @Test
  fun saveOAuthStateThenReadRoundTripsThroughStringStore() = runTest {
    val store = FakeAuthSecureStringStore()
    val storage = JsonBackedAuthSecureStorage(Json, store)
    val state = PendingOAuthState(state = "oauth-state", codeVerifier = "verifier")

    storage.saveOAuthState(state)

    assertEquals(state, storage.getOAuthState())
    assertEquals(0, store.deleteCounts[JsonBackedAuthSecureStorage.OAuthStateKey] ?: 0)
  }

  @Test
  fun saveOAuthStateThrowsWhenStringStoreWriteFails() = runTest {
    val store =
        FakeAuthSecureStringStore(
            writeFailures =
                mutableMapOf(
                    JsonBackedAuthSecureStorage.OAuthStateKey to
                        AuthSecureStringWriteResult.Failure(operation = "EncodeValueData")
                )
        )
    val storage = JsonBackedAuthSecureStorage(Json, store)

    val error =
        assertFailsWith<AuthSecureStorageWriteException> {
          storage.saveOAuthState(
              PendingOAuthState(state = "oauth-state", codeVerifier = "verifier")
          )
        }

    assertEquals("Failed to securely persist OAuth state.", error.message)
    assertEquals(0, store.deleteCounts[JsonBackedAuthSecureStorage.OAuthStateKey] ?: 0)
    assertEquals(
        AuthSecureStringReadResult.Missing,
        store.readWithoutFailures(JsonBackedAuthSecureStorage.OAuthStateKey),
    )
  }

  @Test
  fun clearOAuthStateDeletesStoredValue() = runTest {
    val store = FakeAuthSecureStringStore()
    val storage = JsonBackedAuthSecureStorage(Json, store)

    storage.saveOAuthState(PendingOAuthState(state = "oauth-state", codeVerifier = "verifier"))

    storage.clearOAuthState()

    assertNull(storage.getOAuthState())
    assertEquals(1, store.deleteCounts[JsonBackedAuthSecureStorage.OAuthStateKey])
  }

  @Test
  fun malformedSessionPayloadReturnsNullAndDeletesStoredEntry() = runTest {
    val store =
        FakeAuthSecureStringStore(
            mutableMapOf(JsonBackedAuthSecureStorage.SessionKey to "{not-valid-json")
        )
    val storage = JsonBackedAuthSecureStorage(Json, store)

    assertNull(storage.getSession())
    assertEquals(1, store.deleteCounts[JsonBackedAuthSecureStorage.SessionKey])
    assertEquals(
        AuthSecureStringReadResult.Missing,
        store.read(JsonBackedAuthSecureStorage.SessionKey),
    )
  }

  @Test
  fun malformedOAuthPayloadReturnsNullAndDeletesStoredEntry() = runTest {
    val store =
        FakeAuthSecureStringStore(
            mutableMapOf(JsonBackedAuthSecureStorage.OAuthStateKey to "{not-valid-json")
        )
    val storage = JsonBackedAuthSecureStorage(Json, store)

    assertNull(storage.getOAuthState())
    assertEquals(1, store.deleteCounts[JsonBackedAuthSecureStorage.OAuthStateKey])
    assertEquals(
        AuthSecureStringReadResult.Missing,
        store.read(JsonBackedAuthSecureStorage.OAuthStateKey),
    )
  }

  @Test
  fun unexpectedStoreReadFailureReturnsNullWithoutDeletingStoredEntry() = runTest {
    val store =
        FakeAuthSecureStringStore(
            mutableMapOf(JsonBackedAuthSecureStorage.SessionKey to sampleSessionPayload()),
            readFailures =
                mutableMapOf(
                    JsonBackedAuthSecureStorage.SessionKey to
                        AuthSecureStringReadResult.Failure(
                            operation = "SecItemCopyMatching",
                            status = -34018,
                        )
                ),
        )
    val storage = JsonBackedAuthSecureStorage(Json, store)

    assertNull(storage.getSession())
    assertEquals(0, store.deleteCounts[JsonBackedAuthSecureStorage.SessionKey] ?: 0)
    assertEquals(
        AuthSecureStringReadResult.Value(sampleSessionPayload()),
        store.readWithoutFailures(JsonBackedAuthSecureStorage.SessionKey),
    )
  }

  private fun sampleSession(): AuthSession =
      AuthSession(
          sub = "misty",
          email = "misty@example.com",
          displayName = "Misty",
          provider = "Google",
          idToken = "id-token",
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresAtEpochSeconds = 4102444800,
      )

  private fun sampleSessionPayload(): String = Json.encodeToString(sampleSession())

  private class FakeAuthSecureStringStore(
      private val values: MutableMap<String, String> = mutableMapOf(),
      private val readFailures: MutableMap<String, AuthSecureStringReadResult.Failure> =
          mutableMapOf(),
      private val writeFailures: MutableMap<String, AuthSecureStringWriteResult.Failure> =
          mutableMapOf(),
  ) : AuthSecureStringStore {
    val deleteCounts: MutableMap<String, Int> = mutableMapOf()

    override fun read(key: String): AuthSecureStringReadResult =
        readFailures[key] ?: readWithoutFailures(key)

    fun readWithoutFailures(key: String): AuthSecureStringReadResult =
        values[key]?.let(AuthSecureStringReadResult::Value) ?: AuthSecureStringReadResult.Missing

    override fun write(key: String, value: String): AuthSecureStringWriteResult {
      writeFailures[key]?.let {
        return it
      }
      values[key] = value
      return AuthSecureStringWriteResult.Success
    }

    override fun delete(key: String) {
      deleteCounts[key] = (deleteCounts[key] ?: 0) + 1
      values.remove(key)
    }
  }
}
