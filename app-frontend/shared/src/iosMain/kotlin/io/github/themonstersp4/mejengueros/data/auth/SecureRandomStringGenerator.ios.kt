package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

class SecureRandomStringGenerator : IRandomStringGenerator {
  @OptIn(ExperimentalForeignApi::class)
  override fun generate(length: Int): String {
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
      val status = SecRandomCopyBytes(kSecRandomDefault, bytes.size.toULong(), pinned.addressOf(0))
      check(status == errSecSuccess) { "Unable to generate secure random bytes." }
    }
    return buildString(length) {
      bytes.forEach { byte -> append(Alphabet[(byte.toInt() and 0xff) % Alphabet.length]) }
    }
  }

  private companion object {
    const val Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
  }
}
