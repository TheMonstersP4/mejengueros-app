package io.github.themonstersp4.mejengueros.data.auth

import org.kotlincrypto.hash.sha2.SHA256

object Sha256 {
  fun digest(input: ByteArray): ByteArray = SHA256().digest(input)
}
