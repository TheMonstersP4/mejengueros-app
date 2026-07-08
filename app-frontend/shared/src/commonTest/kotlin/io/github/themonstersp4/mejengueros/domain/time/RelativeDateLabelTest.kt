package io.github.themonstersp4.mejengueros.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeDateLabelTest {

  @Test
  fun returnsHoyForSameDayTimestamps() {
    val label =
        formatRelativeDateLabel(
            "2026-07-05T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 18, 0, 0),
        )
    assertEquals("Hoy", label)
  }

  @Test
  fun returnsAyerForOneDayOldTimestamps() {
    val label =
        formatRelativeDateLabel(
            "2026-07-04T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Ayer", label)
  }

  @Test
  fun returnsHaceNDiasForFewDays() {
    val label =
        formatRelativeDateLabel(
            "2026-07-01T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hace 4 días", label)
  }

  @Test
  fun returnsHaceNWeeksForExactWeekMultiples() {
    val label =
        formatRelativeDateLabel(
            "2026-06-17T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 1, 12, 0, 0),
        )
    assertEquals("Hace 2 semanas", label)
  }

  @Test
  fun returnsHace1SemanaForSingleWeek() {
    val label =
        formatRelativeDateLabel(
            "2026-06-24T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 1, 12, 0, 0),
        )
    assertEquals("Hace 1 semana", label)
  }

  @Test
  fun returnsHaceNMonthsForExactMonthMultiples() {
    // 2026-04-06 to 2026-07-05 is exactly 90 days (3 × 30 days per the formatter's month logic).
    val label =
        formatRelativeDateLabel(
            "2026-04-06T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hace 3 meses", label)
  }

  @Test
  fun returnsHace1MesForSingleMonth() {
    val label =
        formatRelativeDateLabel(
            "2026-06-05T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hace 1 mes", label)
  }

  @Test
  fun returnsHaceNAniosForExactYearMultiples() {
    val label =
        formatRelativeDateLabel(
            "2024-07-05T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hace 2 años", label)
  }

  @Test
  fun returnsHace1AnioForSingleYear() {
    val label =
        formatRelativeDateLabel(
            "2025-07-05T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hace 1 año", label)
  }

  @Test
  fun returnsOriginalTimestampWhenUnparseable() {
    val label = formatRelativeDateLabel("not-a-date", nowEpochSeconds = 0L)
    assertEquals("not-a-date", label)
  }

  @Test
  fun clampsFutureTimestampsToHoy() {
    val label =
        formatRelativeDateLabel(
            "2026-07-10T12:00:00.000Z",
            nowEpochSeconds = epochOf(2026, 7, 5, 12, 0, 0),
        )
    assertEquals("Hoy", label)
  }
}

internal fun epochOf(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
  val y = if (month <= 2) year - 1 else year
  val era = if (y >= 0) y else y - 399
  val eraOffset = era / 400
  val yearOfEra = y - eraOffset * 400
  val dayOfYear = ((153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1)
  val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
  val days = eraOffset * 146097L + dayOfEra - 719468L
  return days * 86_400L + hour * 3600L + minute * 60L + second
}
