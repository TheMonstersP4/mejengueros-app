// commonMain
package example.platform

expect class SecureStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}

// androidMain
package example.platform

actual class SecureStore(private val context: android.content.Context) {
    actual suspend fun read(key: String): String? = TODO("Read from EncryptedSharedPreferences")
    actual suspend fun write(key: String, value: String) = TODO("Write to EncryptedSharedPreferences")
}

// iosMain
package example.platform

actual class SecureStore {
    actual suspend fun read(key: String): String? = TODO("Read from Keychain")
    actual suspend fun write(key: String, value: String) = TODO("Write to Keychain")
}

// jvmMain
package example.platform

actual class SecureStore {
    actual suspend fun read(key: String): String? = null // explicit no-op/fallback policy
    actual suspend fun write(key: String, value: String) = Unit
}
