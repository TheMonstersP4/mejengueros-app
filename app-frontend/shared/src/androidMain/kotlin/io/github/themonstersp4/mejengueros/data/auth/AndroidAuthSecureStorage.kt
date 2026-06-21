package io.github.themonstersp4.mejengueros.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import java.nio.charset.StandardCharsets.UTF_8
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class AndroidAuthSecureStorage
internal constructor(
    private val preferences: SharedPreferences,
    private val json: Json,
    private val cipher: AndroidTextCipher = AndroidAesGcmCipher(AndroidKeystoreSecretKeyProvider()),
) : IAuthSecureStorage {
  constructor(
      context: Context,
      json: Json,
  ) : this(createPreferences(context), json)

  init {
    ensureStorageSchema()
  }

  override suspend fun getSession(): AuthSession? =
      readValue(SessionKey) { json.decodeFromString<AuthSession>(it) }

  override suspend fun saveSession(session: AuthSession) {
    writeValue(SessionKey, json.encodeToString(session))
  }

  override suspend fun clearSession() {
    preferences.edit().remove(SessionKey).apply()
  }

  override suspend fun getOAuthState(): PendingOAuthState? =
      readValue(OAuthStateKey) { json.decodeFromString<PendingOAuthState>(it) }

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    writeValue(OAuthStateKey, json.encodeToString(state))
  }

  override suspend fun clearOAuthState() {
    preferences.edit().remove(OAuthStateKey).apply()
  }

  private fun writeValue(key: String, value: String) {
    preferences.edit().putString(key, cipher.encrypt(value)).apply()
  }

  private fun <T> readValue(key: String, decode: (String) -> T): T? {
    val payload = preferences.getString(key, null) ?: return null
    val plaintext =
        try {
          cipher.decrypt(payload)
        } catch (_: IllegalArgumentException) {
          null
        } catch (_: GeneralSecurityException) {
          null
        }

    if (plaintext == null) {
      preferences.edit().remove(key).apply()
      return null
    }

    return try {
      decode(plaintext)
    } catch (_: IllegalArgumentException) {
      preferences.edit().remove(key).apply()
      null
    } catch (_: SerializationException) {
      preferences.edit().remove(key).apply()
      null
    }
  }

  private fun ensureStorageSchema() {
    if (
        preferences.getInt(StorageSchemaVersionKey, MissingStorageSchemaVersion) ==
            CurrentStorageSchemaVersion
    ) {
      return
    }

    // The previous Android implementation used deprecated encrypted-key preferences in the same
    // file. Since those entries cannot be safely read without reintroducing deprecated APIs, the
    // first launch on this schema performs an intentional one-time auth reset instead of silently
    // failing to read legacy values.
    preferences.edit().clear().putInt(StorageSchemaVersionKey, CurrentStorageSchemaVersion).apply()
  }

  internal companion object {
    const val PreferencesName = "mejengueros_auth_secure_storage"
    const val SessionKey = "auth_session"
    const val OAuthStateKey = "oauth_state"
    const val StorageSchemaVersionKey = "storage_schema_version"
    const val KeyAlias = "mejengueros_auth_secure_storage_key"
    const val IvLengthBytes = 12
    const val GcmTagLengthBits = 128
    const val AndroidKeyStoreProvider = "AndroidKeyStore"
    const val AesTransformation = "AES/GCM/NoPadding"
    const val MissingStorageSchemaVersion = -1
    const val CurrentStorageSchemaVersion = 1

    fun createPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
  }
}

internal interface AndroidTextCipher {
  fun encrypt(plaintext: String): String

  @Throws(GeneralSecurityException::class, IllegalArgumentException::class)
  fun decrypt(payload: String): String?
}

internal interface SecretKeyProvider {
  fun getOrCreate(): SecretKey
}

internal class AndroidAesGcmCipher(
    private val secretKeyProvider: SecretKeyProvider,
    private val cipherFactory: AndroidCipherFactory = AndroidCipherFactoryImpl(),
) : AndroidTextCipher {
  override fun encrypt(plaintext: String): String {
    val cipher = cipherFactory.createEncryptCipher(secretKeyProvider.getOrCreate())

    val ciphertext = cipher.doFinal(plaintext.toByteArray(UTF_8))
    val iv = cipher.iv
    return AndroidCipherPayloadCodec.encode(iv, ciphertext)
  }

  override fun decrypt(payload: String): String? {
    val decodedPayload = AndroidCipherPayloadCodec.decode(payload) ?: return null
    val cipher =
        cipherFactory.createDecryptCipher(secretKeyProvider.getOrCreate(), decodedPayload.iv)
    return String(cipher.doFinal(decodedPayload.ciphertext), UTF_8)
  }
}

internal interface AndroidCipherFactory {
  fun createEncryptCipher(secretKey: SecretKey): Cipher

  fun createDecryptCipher(secretKey: SecretKey, iv: ByteArray): Cipher
}

internal class AndroidCipherFactoryImpl : AndroidCipherFactory {
  override fun createEncryptCipher(secretKey: SecretKey): Cipher =
      Cipher.getInstance(AndroidAuthSecureStorage.AesTransformation).apply {
        init(Cipher.ENCRYPT_MODE, secretKey)
      }

  override fun createDecryptCipher(secretKey: SecretKey, iv: ByteArray): Cipher =
      Cipher.getInstance(AndroidAuthSecureStorage.AesTransformation).apply {
        init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(AndroidAuthSecureStorage.GcmTagLengthBits, iv),
        )
      }
}

internal class AndroidKeystoreSecretKeyProvider : SecretKeyProvider {
  override fun getOrCreate(): SecretKey {
    val keyStore =
        KeyStore.getInstance(AndroidAuthSecureStorage.AndroidKeyStoreProvider).apply { load(null) }
    val existingKey = keyStore.getKey(AndroidAuthSecureStorage.KeyAlias, null) as? SecretKey
    if (existingKey != null) {
      return existingKey
    }

    val keyGenerator =
        KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            AndroidAuthSecureStorage.AndroidKeyStoreProvider,
        )
    keyGenerator.init(
        KeyGenParameterSpec.Builder(
                AndroidAuthSecureStorage.KeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
    )
    return keyGenerator.generateKey()
  }
}

internal object AndroidCipherPayloadCodec {
  private const val PayloadVersion: Byte = 1

  fun encode(iv: ByteArray, ciphertext: ByteArray): String {
    require(iv.isNotEmpty()) { "IV must not be empty." }
    require(ciphertext.isNotEmpty()) { "Ciphertext must not be empty." }

    val payload = ByteArray(2 + iv.size + ciphertext.size)
    payload[0] = PayloadVersion
    payload[1] = iv.size.toByte()
    iv.copyInto(payload, destinationOffset = 2)
    ciphertext.copyInto(payload, destinationOffset = 2 + iv.size)
    return Base64.encodeToString(payload, Base64.NO_WRAP)
  }

  fun decode(payload: String): DecodedPayload? {
    val bytes =
        try {
          Base64.decode(payload, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
          return null
        }

    if (bytes.size < 3 || bytes[0] != PayloadVersion) {
      return null
    }

    val ivSize = bytes[1].toInt() and 0xFF
    if (ivSize <= 0 || bytes.size <= 2 + ivSize) {
      return null
    }

    val iv = bytes.copyOfRange(2, 2 + ivSize)
    val ciphertext = bytes.copyOfRange(2 + ivSize, bytes.size)
    if (ciphertext.isEmpty()) {
      return null
    }

    return DecodedPayload(iv = iv, ciphertext = ciphertext)
  }
}

internal data class DecodedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray,
)
