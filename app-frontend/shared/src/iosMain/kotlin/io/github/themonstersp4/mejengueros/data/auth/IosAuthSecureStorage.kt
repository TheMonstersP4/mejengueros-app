package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
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

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class IosAuthSecureStorage(json: Json) :
    IAuthSecureStorage by JsonBackedAuthSecureStorage(json, KeychainAuthSecureStringStore())

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class KeychainAuthSecureStringStore : AuthSecureStringStore {
  override fun read(key: String): AuthSecureStringReadResult = memScoped {
    val result = alloc<CFTypeRefVar>()
    result.value = null

    val status =
        keychainOperation(key, includeReturnData = true) { query ->
          SecItemCopyMatching(query, result.ptr)
        }
    if (status == errSecItemNotFound) return@memScoped AuthSecureStringReadResult.Missing
    if (status != errSecSuccess) {
      logUnexpectedKeychainStatus("SecItemCopyMatching", key, status)
      return@memScoped AuthSecureStringReadResult.Failure(
          operation = "SecItemCopyMatching",
          status = status,
      )
    }

    val data =
        CFBridgingRelease(result.value) as? NSData
            ?: run {
              delete(key)
              logStorageWarning(
                  "Keychain returned non-data payload for account=$key; deleted corrupt entry."
              )
              return@memScoped AuthSecureStringReadResult.Failure(
                  operation = "KeychainPayloadTypeMismatch"
              )
            }

    val payload = NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    if (payload == null) {
      delete(key)
      logStorageWarning(
          "Keychain returned non-UTF8 payload for account=$key; deleted corrupt entry."
      )
      return@memScoped AuthSecureStringReadResult.Failure(
          operation = "KeychainPayloadEncodingMismatch"
      )
    }

    AuthSecureStringReadResult.Value(payload)
  }

  override fun write(key: String, value: String): AuthSecureStringWriteResult {
    val valueData = value.toNSString().dataUsingEncoding(NSUTF8StringEncoding)
    if (valueData == null) {
      logStorageWarning("Failed to encode keychain payload for account=$key.")
      return AuthSecureStringWriteResult.Failure(operation = "EncodeValueData")
    }

    return when (val updateStatus = updateItem(key, valueData)) {
      errSecSuccess -> AuthSecureStringWriteResult.Success
      errSecItemNotFound -> addOrRetryUpdate(key, valueData)
      else -> {
        logUnexpectedKeychainStatus("SecItemUpdate", key, updateStatus)
        AuthSecureStringWriteResult.Failure(operation = "SecItemUpdate", status = updateStatus)
      }
    }
  }

  private fun addOrRetryUpdate(key: String, valueData: NSData): AuthSecureStringWriteResult =
      when (val addStatus = addItem(key, valueData)) {
        errSecSuccess -> AuthSecureStringWriteResult.Success
        errSecDuplicateItem -> {
          val retryStatus = updateItem(key, valueData)
          if (retryStatus == errSecSuccess) {
            AuthSecureStringWriteResult.Success
          } else {
            logUnexpectedKeychainStatus("SecItemUpdate(retry)", key, retryStatus)
            AuthSecureStringWriteResult.Failure(
                operation = "SecItemUpdate(retry)",
                status = retryStatus,
            )
          }
        }
        else -> {
          logUnexpectedKeychainStatus("SecItemAdd", key, addStatus)
          AuthSecureStringWriteResult.Failure(operation = "SecItemAdd", status = addStatus)
        }
      }

  private fun addItem(key: String, valueData: NSData): Int =
      keychainOperation(key, valueData = valueData) { query -> SecItemAdd(query, null) }

  private fun updateItem(key: String, valueData: NSData): Int =
      keychainOperation(key) { query ->
        val cfValue = CFBridgingRetain(valueData)
        try {
          memScoped {
            val attributesToUpdate = cfDictionaryOf(mapOf(kSecValueData to cfValue))
            try {
              SecItemUpdate(query, attributesToUpdate)
            } finally {
              CFBridgingRelease(attributesToUpdate)
            }
          }
        } finally {
          CFBridgingRelease(cfValue)
        }
      }

  private fun deleteItem(key: String): Int =
      keychainOperation(key) { query -> SecItemDelete(query) }

  override fun delete(key: String) {
    val status = deleteItem(key)
    if (status != errSecSuccess && status != errSecItemNotFound) {
      logUnexpectedKeychainStatus("SecItemDelete", key, status)
    }
  }

  private inline fun keychainOperation(
      key: String,
      includeReturnData: Boolean = false,
      valueData: NSData? = null,
      operation: (CFDictionaryRef?) -> Int,
  ): Int = memScoped {
    val cfService = CFBridgingRetain(ServiceName)
    val cfKey = CFBridgingRetain(key)
    val cfValue = valueData?.let { CFBridgingRetain(it) }

    val keyValuePairs =
        buildMap<CFStringRef?, CFTypeRef?> {
          put(kSecClass, kSecClassGenericPassword)
          put(kSecAttrService, cfService)
          put(kSecAttrAccount, cfKey)
          put(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
          if (includeReturnData) {
            put(kSecReturnData, kCFBooleanTrue)
            put(kSecMatchLimit, kSecMatchLimitOne)
          }
          if (cfValue != null) {
            put(kSecValueData, cfValue)
          }
        }

    val query = cfDictionaryOf(keyValuePairs)

    try {
      operation(query)
    } finally {
      CFBridgingRelease(query)
      cfValue?.let { CFBridgingRelease(it) }
      CFBridgingRelease(cfKey)
      CFBridgingRelease(cfService)
    }
  }

  private fun MemScope.cfDictionaryOf(map: Map<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
    val keys = allocArrayOf(*map.keys.toTypedArray())
    val values = allocArrayOf(*map.values.toTypedArray())
    return CFDictionaryCreate(
        kCFAllocatorDefault,
        keys.reinterpret(),
        values.reinterpret(),
        map.size.convert(),
        null,
        null,
    )
  }

  private fun String.toNSString(): NSString = NSString.create(string = this)

  private fun logUnexpectedKeychainStatus(operation: String, key: String, status: Int) {
    logStorageWarning("$operation failed for account=$key with OSStatus=$status.")
  }

  private fun logStorageWarning(message: String) {
    println("IosAuthSecureStorage: $message")
  }

  private companion object {
    const val ServiceName = "com.themonsters.mejengueros.auth"
  }
}
