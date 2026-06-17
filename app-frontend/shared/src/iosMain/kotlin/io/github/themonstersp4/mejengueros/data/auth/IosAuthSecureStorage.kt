package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.NSObject
import platform.darwin.kCFBooleanTrue

@OptIn(ExperimentalForeignApi::class)
class IosAuthSecureStorage(private val json: Json) : IAuthSecureStorage {
  override suspend fun getSession(): AuthSession? =
      read(SessionKey)?.let { json.decodeFromString<AuthSession>(it) }

  override suspend fun saveSession(session: AuthSession) {
    write(SessionKey, json.encodeToString(session))
  }

  override suspend fun clearSession() {
    delete(SessionKey)
  }

  override suspend fun getOAuthState(): PendingOAuthState? =
      read(OAuthStateKey)?.let { json.decodeFromString<PendingOAuthState>(it) }

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    write(OAuthStateKey, json.encodeToString(state))
  }

  override suspend fun clearOAuthState() {
    delete(OAuthStateKey)
  }

  private fun read(key: String): String? = memScoped {
    val result = alloc<CFTypeRefVar>()
    val status = SecItemCopyMatching(query(key, includeReturnData = true), result.ptr)
    if (status != errSecSuccess) return@memScoped null
    val data = result.value as? NSData ?: return@memScoped null
    NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
  }

  private fun write(key: String, value: String) {
    delete(key)
    val attributes = query(key)
    attributes.setObject(
        value.toNSString().dataUsingEncoding(NSUTF8StringEncoding)!!,
        kSecValueData,
    )
    SecItemAdd(attributes, null)
  }

  private fun delete(key: String) {
    SecItemDelete(query(key))
  }

  private fun query(key: String, includeReturnData: Boolean = false): NSMutableDictionary {
    val query = NSMutableDictionary()
    query.setObject(kSecClassGenericPassword, kSecClass)
    query.setObject(ServiceName.toNSString(), kSecAttrService)
    query.setObject(key.toNSString(), kSecAttrAccount)
    query.setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, kSecAttrAccessible)
    if (includeReturnData) {
      query.setObject(kCFBooleanTrue as NSObject, kSecReturnData)
      query.setObject(kSecMatchLimitOne, kSecMatchLimit)
    }
    return query
  }

  private fun String.toNSString(): NSString = NSString.create(string = this)

  private companion object {
    const val ServiceName = "com.themonsters.mejengueros.auth"
    const val SessionKey = "auth_session"
    const val OAuthStateKey = "oauth_state"
  }
}
