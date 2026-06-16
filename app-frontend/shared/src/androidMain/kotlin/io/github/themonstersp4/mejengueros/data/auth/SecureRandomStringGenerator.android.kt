package io.github.themonstersp4.mejengueros.data.auth

import java.security.SecureRandom

class SecureRandomStringGenerator : IRandomStringGenerator {
  private val secureRandom = SecureRandom()

  override fun generate(length: Int): String =
      buildString(length) {
        repeat(length) { append(Alphabet[secureRandom.nextInt(Alphabet.length)]) }
      }

  private companion object {
    const val Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
  }
}
