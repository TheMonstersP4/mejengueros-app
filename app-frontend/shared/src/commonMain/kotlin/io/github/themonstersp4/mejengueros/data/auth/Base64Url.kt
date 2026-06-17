package io.github.themonstersp4.mejengueros.data.auth

object Base64Url {
  private const val Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
  private val indexes =
      IntArray(128) { -1 }
          .also { table -> Alphabet.forEachIndexed { index, char -> table[char.code] = index } }

  fun encode(bytes: ByteArray): String {
    val output = StringBuilder(((bytes.size + 2) / 3) * 4)
    var index = 0
    while (index < bytes.size) {
      val first = bytes[index++].toInt() and 0xff
      val second = if (index < bytes.size) bytes[index++].toInt() and 0xff else -1
      val third = if (index < bytes.size) bytes[index++].toInt() and 0xff else -1

      output.append(Alphabet[first ushr 2])
      output.append(Alphabet[((first and 0x03) shl 4) or ((second.takeIf { it >= 0 } ?: 0) ushr 4)])
      if (second >= 0)
          output.append(
              Alphabet[((second and 0x0f) shl 2) or ((third.takeIf { it >= 0 } ?: 0) ushr 6)]
          )
      if (third >= 0) output.append(Alphabet[third and 0x3f])
    }
    return output.toString()
  }

  fun decode(value: String): ByteArray {
    val clean = value.trim().replace("=", "")
    val output = mutableListOf<Byte>()
    var index = 0
    while (index < clean.length) {
      val first = decodeChar(clean[index++])
      val second = decodeChar(clean[index++])
      val third = if (index < clean.length) decodeChar(clean[index++]) else -1
      val fourth = if (index < clean.length) decodeChar(clean[index++]) else -1

      output += ((first shl 2) or (second ushr 4)).toByte()
      if (third >= 0) output += (((second and 0x0f) shl 4) or (third ushr 2)).toByte()
      if (fourth >= 0) output += (((third and 0x03) shl 6) or fourth).toByte()
    }
    return output.toByteArray()
  }

  private fun decodeChar(char: Char): Int {
    require(char.code < indexes.size && indexes[char.code] >= 0) { "Invalid base64url character." }
    return indexes[char.code]
  }
}
