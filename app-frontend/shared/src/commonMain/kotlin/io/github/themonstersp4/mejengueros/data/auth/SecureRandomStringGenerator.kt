package io.github.themonstersp4.mejengueros.data.auth

import dev.whyoleg.cryptography.random.CryptographyRandom

class SecureRandomStringGenerator : IRandomStringGenerator {
  override fun generate(length: Int): String {
    val bytes = CryptographyRandom.nextBytes(length)
    return Base64Url.encode(bytes).take(length)
  }
}
