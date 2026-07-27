package io.github.themonstersp4.mejengueros.presentation.catalog

/**
 * Presentation-layer normalization for surface/service names.
 *
 * The backend catalog stores service names without Spanish accents (for example "Sintetico",
 * "Hibrido", "Iluminacion"). This maps a raw name to its canonical, correctly accented Spanish
 * label for display. Matching is tolerant of missing accents and casing. Names that are not part of
 * the known catalog are returned unchanged as a safe fallback, so the UI never breaks.
 */
private val canonicalServiceNames =
    listOf(
        "Sintético",
        "Híbrido",
        "Natural",
        "Iluminación",
        "Parqueo",
    )

private val serviceLabelByKey: Map<String, String> =
    canonicalServiceNames.associateBy { normalizeServiceKey(it) }

/** Returns the canonical accented label for a service name, or the raw name if it is unknown. */
fun serviceDisplayName(rawName: String): String {
  val key = normalizeServiceKey(rawName)
  return serviceLabelByKey[key] ?: rawName
}

private fun normalizeServiceKey(value: String): String = value.trim().lowercase().stripAccents()

private fun String.stripAccents(): String = buildString {
  for (char in this@stripAccents) {
    append(
        when (char) {
          'á',
          'à',
          'ä',
          'â' -> 'a'
          'é',
          'è',
          'ë',
          'ê' -> 'e'
          'í',
          'ì',
          'ï',
          'î' -> 'i'
          'ó',
          'ò',
          'ö',
          'ô' -> 'o'
          'ú',
          'ù',
          'ü',
          'û' -> 'u'
          else -> char
        }
    )
  }
}
