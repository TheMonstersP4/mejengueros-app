package io.github.themonstersp4.mejengueros

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.themonstersp4.mejengueros.data.auth.AndroidAesGcmCipher
import io.github.themonstersp4.mejengueros.data.auth.AndroidAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.AndroidCipherFactory
import io.github.themonstersp4.mejengueros.data.auth.AndroidCipherPayloadCodec
import io.github.themonstersp4.mejengueros.data.auth.SecretKeyProvider
import io.github.themonstersp4.mejengueros.data.auth.ownerViewPreferenceStorageKey
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedLogicAndroidHostTest {

  @Test
  fun androidSecureStoragePersistsAndClearsAuthSession() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val storage = AndroidAuthSecureStorage(preferences, Json, testCipher())
    val session = sampleSession()

    storage.clearSession()

    storage.saveSession(session)
    assertEquals(session, storage.getSession())
    assertEncrypted(preferences, "auth_session", session.accessToken)

    storage.clearSession()
    assertNull(storage.getSession())
  }

  @Test
  fun androidSecureStoragePersistsAndClearsOAuthState() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = AndroidAuthSecureStorage(testPreferences(context), Json, testCipher())
    val state = PendingOAuthState(state = "state", codeVerifier = "verifier")

    storage.clearOAuthState()

    storage.saveOAuthState(state)
    assertEquals(state, storage.getOAuthState())

    storage.clearOAuthState()
    assertNull(storage.getOAuthState())
  }

  @Test
  fun androidSecureStorageClearsCorruptSessionPayload() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val storage = AndroidAuthSecureStorage(preferences, Json, testCipher())

    preferences.edit().putString("auth_session", "not-valid-base64").commit()

    assertNull(storage.getSession())
    assertFalse(preferences.contains("auth_session"))
  }

  @Test
  fun androidSecureStorageOwnerViewPreferenceRoundTripsAndClears() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val storage = AndroidAuthSecureStorage(preferences, Json, testCipher())

    storage.saveOwnerViewPreference(
        "owner-1",
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.OWNER,
    )
    storage.saveOwnerViewPreference(
        "player-1",
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER,
    )

    assertEquals(
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.OWNER,
        storage.getOwnerViewPreference("owner-1"),
    )
    assertEquals(
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER,
        storage.getOwnerViewPreference("player-1"),
    )

    storage.clearOwnerViewPreference("owner-1")

    assertNull(storage.getOwnerViewPreference("owner-1"))
    assertEquals(
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER,
        storage.getOwnerViewPreference("player-1"),
    )
  }

  @Test
  fun androidSecureStorageOwnerViewPreferenceClearsUnknownPayload() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val cipher = testCipher()
    val storage = AndroidAuthSecureStorage(preferences, Json, cipher)
    val key = io.github.themonstersp4.mejengueros.data.auth.ownerViewPreferenceStorageKey("owner-1")

    preferences.edit().putString(key, cipher.encrypt("COACH")).commit()

    assertNull(storage.getOwnerViewPreference("owner-1"))
    assertFalse(preferences.contains(key))
  }

  @Test
  fun androidSecureStorageOwnerViewPreferenceTrimmedAndBlankUserIdsAreDeterministic() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val storage = AndroidAuthSecureStorage(preferences, Json, testCipher())
    val trimmedKey = ownerViewPreferenceStorageKey(" owner-1 ")
    val blankKey = ownerViewPreferenceStorageKey("   ")

    storage.saveOwnerViewPreference(
        " owner-1 ",
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.OWNER,
    )
    storage.saveOwnerViewPreference(
        "   ",
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER,
    )

    assertEquals(
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.OWNER,
        storage.getOwnerViewPreference("owner-1"),
    )
    assertEquals(
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER,
        storage.getOwnerViewPreference(""),
    )
    assertEquals(trimmedKey, ownerViewPreferenceStorageKey("owner-1"))
    assertEquals(blankKey, ownerViewPreferenceStorageKey(""))
    assertFalse(trimmedKey.contains("owner-1"))
    assertNotEquals("owner_view_preference_", blankKey)
    assertEncrypted(
        preferences,
        trimmedKey,
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.OWNER.name,
    )
    assertEncrypted(
        preferences,
        blankKey,
        io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference.PLAYER.name,
    )
  }

  @Test
  fun androidCipherEncryptionDoesNotRequireCallerProvidedIv() {
    val cipherFactory = RecordingCipherFactory()
    val cipher = AndroidAesGcmCipher(FixedSecretKeyProvider(testSecretKey()), cipherFactory)

    val payload = cipher.encrypt("session-token")
    val decodedPayload = AndroidCipherPayloadCodec.decode(payload)

    assertEquals(1, cipherFactory.encryptCalls)
    assertEquals(0, cipherFactory.decryptCalls)
    assertFalse(decodedPayload == null || decodedPayload.iv.isEmpty())
  }

  @Test
  fun androidCipherUsesRandomIvForRepeatedSessionEncryption() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val cipher = testCipher()
    val storage = AndroidAuthSecureStorage(preferences, Json, cipher)
    val session = sampleSession()

    storage.saveSession(session)
    val firstPayload =
        preferences.getString(AndroidAuthSecureStorage.SessionKey, null)
            ?: error("Expected first encrypted payload")

    storage.saveSession(session)
    val secondPayload =
        preferences.getString(AndroidAuthSecureStorage.SessionKey, null)
            ?: error("Expected second encrypted payload")

    assertNotEquals(firstPayload, secondPayload)
    assertEquals(session, Json.decodeFromString<AuthSession>(cipher.decrypt(firstPayload)!!))
    assertEquals(session, Json.decodeFromString<AuthSession>(cipher.decrypt(secondPayload)!!))
    assertEquals(session, storage.getSession())
  }

  @Test
  fun androidSecureStorageForcedLegacyResetClearsUnknownEntriesAndMarksSchemaVersion() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context, clearFirst = true)

    preferences.edit().putString("legacy_encrypted_key_blob", "legacy-value").commit()

    AndroidAuthSecureStorage(preferences, Json, testCipher())

    assertFalse(preferences.contains("legacy_encrypted_key_blob"))
    assertEquals(
        AndroidAuthSecureStorage.CurrentStorageSchemaVersion,
        preferences.getInt(
            AndroidAuthSecureStorage.StorageSchemaVersionKey,
            AndroidAuthSecureStorage.MissingStorageSchemaVersion,
        ),
    )
  }

  @Test
  fun androidSecureStorageSchemaMarkerPreventsRepeatedResetAcrossStorageInstances() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = testPreferences(context)
    val session = sampleSession()
    val secretKey = testSecretKey()

    AndroidAuthSecureStorage(preferences, Json, testCipher(secretKey)).saveSession(session)

    val secondStorage = AndroidAuthSecureStorage(preferences, Json, testCipher(secretKey))

    assertEquals(session, secondStorage.getSession())
    assertEquals(
        AndroidAuthSecureStorage.CurrentStorageSchemaVersion,
        preferences.getInt(
            AndroidAuthSecureStorage.StorageSchemaVersionKey,
            AndroidAuthSecureStorage.MissingStorageSchemaVersion,
        ),
    )
  }

  @Test
  fun androidCipherPayloadCodecRoundTripsIvAndCiphertext() {
    val iv = byteArrayOf(1, 2, 3, 4)
    val ciphertext = byteArrayOf(9, 8, 7, 6, 5)

    val decoded = AndroidCipherPayloadCodec.decode(AndroidCipherPayloadCodec.encode(iv, ciphertext))

    assertEquals(iv.toList(), decoded?.iv?.toList())
    assertContentEquals(ciphertext, decoded?.ciphertext)
  }

  @Test
  fun androidCipherPayloadCodecRejectsMalformedPayload() {
    assertNull(AndroidCipherPayloadCodec.decode("not-valid-base64"))
  }

  private fun testPreferences(context: Context, clearFirst: Boolean = true) =
      context.getSharedPreferences("auth-secure-storage-test", Context.MODE_PRIVATE).also {
        if (clearFirst) {
          it.edit().clear().commit()
        }
      }

  private fun testCipher(secretKey: SecretKey = testSecretKey()) =
      AndroidAesGcmCipher(FixedSecretKeyProvider(secretKey), RecordingCipherFactory())

  private fun testSecretKey(): SecretKey =
      KeyGenerator.getInstance("AES").run {
        init(256)
        generateKey()
      }

  private fun assertEncrypted(
      preferences: android.content.SharedPreferences,
      key: String,
      secret: String,
  ) {
    val storedValue =
        preferences.getString(key, null) ?: error("Expected encrypted value for key $key")
    assertFalse(storedValue.contains(secret))
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

  private class FixedSecretKeyProvider(private val secretKey: SecretKey) : SecretKeyProvider {
    override fun getOrCreate(): SecretKey = secretKey
  }

  private class RecordingCipherFactory : AndroidCipherFactory {
    var encryptCalls: Int = 0
      private set

    var decryptCalls: Int = 0
      private set

    override fun createEncryptCipher(secretKey: SecretKey): Cipher {
      encryptCalls += 1
      return Cipher.getInstance(AndroidAuthSecureStorage.AesTransformation).apply {
        init(Cipher.ENCRYPT_MODE, secretKey)
      }
    }

    override fun createDecryptCipher(secretKey: SecretKey, iv: ByteArray): Cipher {
      decryptCalls += 1
      return Cipher.getInstance(AndroidAuthSecureStorage.AesTransformation).apply {
        init(
            Cipher.DECRYPT_MODE,
            secretKey,
            javax.crypto.spec.GCMParameterSpec(AndroidAuthSecureStorage.GcmTagLengthBits, iv),
        )
      }
    }
  }
}
