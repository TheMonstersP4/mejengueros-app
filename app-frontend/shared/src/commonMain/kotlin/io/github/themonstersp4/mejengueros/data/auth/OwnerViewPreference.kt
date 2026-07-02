package io.github.themonstersp4.mejengueros.data.auth

enum class OwnerViewPreference {
  OWNER,
  PLAYER,
}

internal fun ownerViewPreferenceStorageKey(userId: String): String =
    "owner_view_preference_${Sha256.digest(userId.trim().encodeToByteArray()).toHexString()}"

private fun ByteArray.toHexString(): String =
    buildString(size * 2) {
      for (byte in this@toHexString) {
        append(HexDigits[(byte.toInt() ushr 4) and 0x0F])
        append(HexDigits[byte.toInt() and 0x0F])
      }
    }

private const val HexDigits = "0123456789abcdef"
