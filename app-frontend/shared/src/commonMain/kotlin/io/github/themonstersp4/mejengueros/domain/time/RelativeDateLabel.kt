package io.github.themonstersp4.mejengueros.domain.time

import kotlin.time.Clock

/**
 * Formats an ISO-8601 timestamp (e.g. "2026-07-01T18:00:00.000Z") as a short Spanish relative date
 * label like "Hoy", "Ayer", "Hace 3 días", "Hace 2 semanas", "Hace 3 meses", or "Hace 2 años".
 *
 * Pure-Kotlin so the formatter stays testable from common tests without touching kotlinx-datetime
 * or platform-specific ISO parsers.
 */
fun formatRelativeDateLabel(
    isoTimestamp: String,
    nowEpochSeconds: Long = currentEpochSecondsCompat(),
): String {
  val seconds = parseIsoToEpochSeconds(isoTimestamp) ?: return isoTimestamp
  val diffSeconds = (nowEpochSeconds - seconds).coerceAtLeast(0L)
  val days = diffSeconds / SecondsPerDay

  return when {
    days <= 0L -> "Hoy"
    days == 1L -> "Ayer"
    days < 7L -> "Hace $days días"
    days < 30L -> {
      val weeks = days / 7L
      if (days % 7L == 0L) {
        if (weeks == 1L) "Hace 1 semana" else "Hace $weeks semanas"
      } else {
        "Hace $days días"
      }
    }
    days < 365L -> {
      val months = days / 30L
      if (days % 30L == 0L) {
        if (months == 1L) "Hace 1 mes" else "Hace $months meses"
      } else {
        "Hace $days días"
      }
    }
    else -> {
      val years = days / 365L
      if (days % 365L == 0L) {
        if (years == 1L) "Hace 1 año" else "Hace $years años"
      } else {
        "Hace $days días"
      }
    }
  }
}

private fun parseIsoToEpochSeconds(iso: String): Long? {
  val trimmed = iso.trim()
  if (trimmed.isEmpty()) return null
  val datePart = trimmed.substringBefore('T').takeIf { it.length >= 10 } ?: return null
  val timePart = trimmed.substringAfter('T', missingDelimiterValue = "")

  val year = datePart.substring(0, 4).toIntOrNull() ?: return null
  val month = datePart.substring(5, 7).toIntOrNull() ?: return null
  val day = datePart.substring(8, 10).toIntOrNull() ?: return null

  var hour = 0
  var minute = 0
  var second = 0
  if (timePart.isNotEmpty()) {
    val timeCore = timePart.substringBefore('+').substringBefore('-').substringBefore('Z')
    val timeSegments = timeCore.split(':')
    if (timeSegments.isNotEmpty()) {
      hour = timeSegments[0].toIntOrNull() ?: return null
      if (timeSegments.size > 1) {
        minute = timeSegments[1].toIntOrNull() ?: return null
      }
      if (timeSegments.size > 2) {
        val secondSegment = timeSegments[2].substringBefore('.').substringBefore(',')
        second = secondSegment.toIntOrNull() ?: return null
      }
    }
  }

  return utcDateTimeToEpochSeconds(year, month, day, hour, minute, second)
}

private fun utcDateTimeToEpochSeconds(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
): Long {
  val days = daysFromCivil1970(year, month, day)
  return days * SecondsPerDay + hour * 3600L + minute * 60L + second
}

private fun daysFromCivil1970(year: Int, month: Int, day: Int): Long {
  // Adapted from Howard Hinnant's date algorithms to keep this self-contained.
  val y = if (month <= 2) year - 1 else year
  val era = if (y >= 0) y else y - 399
  val eraOffset = era / 400
  val yearOfEra = y - eraOffset * 400
  val dayOfYear = ((153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1)
  val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
  return eraOffset * 146097L + dayOfEra - 719468L
}

private fun currentEpochSecondsCompat(): Long = Clock.System.now().epochSeconds

private const val SecondsPerDay = 86_400L
