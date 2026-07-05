package io.github.themonstersp4.mejengueros.domain.time

import kotlin.time.Clock

private const val CostaRicaUtcOffsetSeconds = -6 * 60 * 60L
private const val CostaRicaUtcOffsetMinutes = -6 * 60

internal fun todayUtcDateString(): String =
    epochSecondsToUtcDateString(Clock.System.now().epochSeconds)

internal fun todayCostaRicaDateString(): String =
    epochSecondsToCostaRicaDateString(Clock.System.now().epochSeconds)

/**
 * Shared UTC civil-date math based on the proleptic Gregorian calendar.
 *
 * These conversions intentionally stay centralized here so reservation date labels and epoch
 * arithmetic never drift across duplicate implementations.
 */
private object UtcCivilDateMath {
  const val DaysPerWeek = 7
  const val ThursdayOffsetFromMonday = 3
  const val MonthsPerYear = 12
  const val January = 1
  const val February = 2
  const val March = 3
  const val DaysPerCommonYear = 365
  const val YearsPerEra = 400
  const val DaysPerEra = 146_097
  const val DaysPer4YearCycle = 1_460
  const val DaysPer100YearCycle = 36_524
  const val LastDayIndexInEra = 146_096
  const val MarchBasedMonthScale = 153
  const val MarchBasedMonthOffset = 2
  const val DaysFromCivil1970Epoch = 719_468
  const val SecondsPerDay = 86_400L
  const val JanuaryBasedMonthShift = 9
}

internal data class UtcCalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
  fun toIsoDate(): String =
      "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

  fun plusDays(days: Int): UtcCalendarDate = fromEpochDay(toEpochDay() + days)

  fun shortWeekdayLabel(): String =
      when (
          mod(
              toEpochDay() + UtcCivilDateMath.ThursdayOffsetFromMonday,
              UtcCivilDateMath.DaysPerWeek,
          )
      ) {
        0 -> "Lun"
        1 -> "Mar"
        2 -> "Mié"
        3 -> "Jue"
        4 -> "Vie"
        5 -> "Sáb"
        else -> "Dom"
      }

  fun monthName(): String =
      when (month) {
        1 -> "enero"
        2 -> "febrero"
        3 -> "marzo"
        4 -> "abril"
        5 -> "mayo"
        6 -> "junio"
        7 -> "julio"
        8 -> "agosto"
        9 -> "septiembre"
        10 -> "octubre"
        11 -> "noviembre"
        else -> "diciembre"
      }

  fun toEpochDay(): Int {
    val adjustedYear = year - if (month <= UtcCivilDateMath.February) 1 else 0
    val era = floorDiv(adjustedYear, UtcCivilDateMath.YearsPerEra)
    val yearOfEra = adjustedYear - era * UtcCivilDateMath.YearsPerEra
    val marchBasedMonth =
        month +
            if (month > UtcCivilDateMath.February) -UtcCivilDateMath.March
            else UtcCivilDateMath.JanuaryBasedMonthShift
    val dayOfYear =
        (UtcCivilDateMath.MarchBasedMonthScale * marchBasedMonth +
            UtcCivilDateMath.MarchBasedMonthOffset) / 5 + day - 1
    val dayOfEra =
        yearOfEra * UtcCivilDateMath.DaysPerCommonYear + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * UtcCivilDateMath.DaysPerEra + dayOfEra - UtcCivilDateMath.DaysFromCivil1970Epoch
  }

  companion object {
    fun fromEpochDay(epochDay: Int): UtcCalendarDate {
      val daysFromCivilEpoch = epochDay + UtcCivilDateMath.DaysFromCivil1970Epoch
      val era = floorDiv(daysFromCivilEpoch, UtcCivilDateMath.DaysPerEra)
      val dayOfEra = daysFromCivilEpoch - era * UtcCivilDateMath.DaysPerEra
      val yearOfEra =
          (dayOfEra - dayOfEra / UtcCivilDateMath.DaysPer4YearCycle +
              dayOfEra / UtcCivilDateMath.DaysPer100YearCycle -
              dayOfEra / UtcCivilDateMath.LastDayIndexInEra) / UtcCivilDateMath.DaysPerCommonYear
      val yearWithinEra = yearOfEra + era * UtcCivilDateMath.YearsPerEra
      val dayOfYear =
          dayOfEra -
              (UtcCivilDateMath.DaysPerCommonYear * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
      val marchBasedMonth =
          (5 * dayOfYear + UtcCivilDateMath.MarchBasedMonthOffset) /
              UtcCivilDateMath.MarchBasedMonthScale
      val dayOfMonth =
          dayOfYear -
              (UtcCivilDateMath.MarchBasedMonthScale * marchBasedMonth +
                  UtcCivilDateMath.MarchBasedMonthOffset) / 5 + 1
      val month =
          marchBasedMonth +
              if (marchBasedMonth < 10) UtcCivilDateMath.March
              else -UtcCivilDateMath.JanuaryBasedMonthShift
      val year = yearWithinEra + if (month <= UtcCivilDateMath.February) 1 else 0
      return UtcCalendarDate(year = year, month = month, day = dayOfMonth)
    }
  }
}

internal fun parseUtcCalendarDate(value: String): UtcCalendarDate {
  val parts = value.split('-')
  require(parts.size == 3) { "Invalid UTC date: $value" }
  return UtcCalendarDate(
      year = parts[0].toInt(),
      month = parts[1].toInt(),
      day = parts[2].toInt(),
  )
}

internal fun epochSecondsToUtcDateString(epochSeconds: Long): String {
  val epochDay = floorDiv(epochSeconds, UtcCivilDateMath.SecondsPerDay).toInt()
  return UtcCalendarDate.fromEpochDay(epochDay).toIsoDate()
}

internal fun epochSecondsToCostaRicaDateString(epochSeconds: Long): String =
    epochSecondsToUtcDateString(epochSeconds + CostaRicaUtcOffsetSeconds)

internal fun String.toCostaRicaDateLabel(): String =
    parseUtcInstantToCostaRicaDateTimeOrNull()?.date?.toIsoDate() ?: this

internal fun String.toCostaRicaTimeLabel(): String =
    parseUtcInstantToCostaRicaDateTimeOrNull()?.toTimeLabel() ?: this

private data class CostaRicaDateTime(
    val date: UtcCalendarDate,
    val hour: Int,
    val minute: Int,
) {
  fun toTimeLabel(): String =
      "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun String.parseUtcInstantToCostaRicaDateTimeOrNull(): CostaRicaDateTime? {
  if (length < 17 || getOrNull(4) != '-' || getOrNull(7) != '-' || getOrNull(10) != 'T') {
    return null
  }

  val year = substring(0, 4).toIntOrNull() ?: return null
  val month = substring(5, 7).toIntOrNull() ?: return null
  val day = substring(8, 10).toIntOrNull() ?: return null
  val hour = substring(11, 13).toIntOrNull() ?: return null
  val minute = substring(14, 16).toIntOrNull() ?: return null

  val baseDate = UtcCalendarDate(year = year, month = month, day = day)
  val totalMinutesInCostaRica = hour * 60 + minute + CostaRicaUtcOffsetMinutes
  val dayOffset = floorDiv(totalMinutesInCostaRica, 24 * 60)
  val normalizedMinutes = mod(totalMinutesInCostaRica, 24 * 60)

  return CostaRicaDateTime(
      date = baseDate.plusDays(dayOffset),
      hour = normalizedMinutes / 60,
      minute = normalizedMinutes % 60,
  )
}

private fun floorDiv(value: Int, divisor: Int): Int {
  var quotient = value / divisor
  if ((value xor divisor) < 0 && quotient * divisor != value) {
    quotient -= 1
  }
  return quotient
}

private fun floorDiv(value: Long, divisor: Long): Long {
  var quotient = value / divisor
  if ((value xor divisor) < 0L && quotient * divisor != value) {
    quotient -= 1
  }
  return quotient
}

private fun mod(value: Int, divisor: Int): Int {
  val result = value % divisor
  return if (result < 0) result + divisor else result
}
