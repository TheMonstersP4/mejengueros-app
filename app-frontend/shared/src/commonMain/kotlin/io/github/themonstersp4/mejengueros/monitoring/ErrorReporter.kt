package io.github.themonstersp4.mejengueros.monitoring

interface ErrorReporter {
  fun reportRecoverableFailure(
      name: String,
      attributes: Map<String, String> = emptyMap(),
  )
}

class NoOpErrorReporter : ErrorReporter {
  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) = Unit
}

internal fun formatRecoverableFailureMessage(
    name: String,
    attributes: Map<String, String>,
): String {
  val safeAttributes = attributes.toSafeLogAttributes()

  return buildString {
    append("Recoverable failure: ")
    append(name.toSafeLogValue())

    if (safeAttributes.isNotEmpty()) {
      append(" | attributes=")
      append(
          safeAttributes.entries.joinToString(prefix = "[", postfix = "]") { (key, value) ->
            "$key=$value"
          }
      )
    }
  }
}

private fun Map<String, String>.toSafeLogAttributes(): Map<String, String> =
    entries
        .mapNotNull { (key, value) ->
          key.toSafeLogKey()?.let { safeKey -> safeKey to value.toSafeLogValue() }
        }
        .toMap()

private fun String.toSafeLogKey(): String? {
  val normalized = trim().lowercase()
  if (normalized.isEmpty()) return null

  val blockedFragments =
      listOf(
          "token",
          "secret",
          "password",
          "cookie",
          "authorization",
          "email",
          "phone",
          "name",
          "address",
          "env",
      )

  if (blockedFragments.any(normalized::contains)) return null

  val safeKey = normalized.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
  return safeKey.ifBlank { null }
}

private fun String.toSafeLogValue(): String =
    trim().replace(Regex("[\\r\\n\\t]+"), " ").take(MAX_LOG_VALUE_LENGTH)

private const val MAX_LOG_VALUE_LENGTH = 120
