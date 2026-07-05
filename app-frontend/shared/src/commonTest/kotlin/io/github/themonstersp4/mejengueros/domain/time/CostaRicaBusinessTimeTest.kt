package io.github.themonstersp4.mejengueros.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

private const val COSTA_RICA_ROLLOVER_EPOCH_SECONDS = 1_783_218_240L
private const val UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT = 1_783_231_199L
private const val UTC_COSTA_RICA_MIDNIGHT = 1_783_231_200L
private const val UTC_SECOND_BEFORE_NEW_YEAR_COSTA_RICA_MIDNIGHT = 1_798_783_199L
private const val UTC_NEW_YEAR_COSTA_RICA_MIDNIGHT = 1_798_783_200L

class CostaRicaBusinessTimeTest {
  @Test
  fun epochSecondsToCostaRicaDateStringUsesCivilDateAtUtcRollover() {
    assertEquals(
        "2026-07-04",
        epochSecondsToCostaRicaDateString(COSTA_RICA_ROLLOVER_EPOCH_SECONDS),
    )
  }

  @Test
  fun epochSecondsToCostaRicaDateStringSwitchesDateExactlyAt060000Z() {
    assertEquals(
        "2026-07-04",
        epochSecondsToCostaRicaDateString(UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT),
    )
    assertEquals(
        "2026-07-05",
        epochSecondsToCostaRicaDateString(UTC_COSTA_RICA_MIDNIGHT),
    )
  }

  @Test
  fun epochSecondsToCostaRicaDateStringHandlesMonthAndYearRollover() {
    assertEquals(
        "2026-12-31",
        epochSecondsToCostaRicaDateString(UTC_SECOND_BEFORE_NEW_YEAR_COSTA_RICA_MIDNIGHT),
    )
    assertEquals(
        "2027-01-01",
        epochSecondsToCostaRicaDateString(UTC_NEW_YEAR_COSTA_RICA_MIDNIGHT),
    )
  }

  @Test
  fun utcInstantLabelsUseCostaRicaBusinessClock() {
    assertEquals("2026-07-04", "2026-07-05T02:24:00.000Z".toCostaRicaDateLabel())
    assertEquals("20:24", "2026-07-05T02:24:00.000Z".toCostaRicaTimeLabel())
    assertEquals("18:00", "2026-07-17T00:00:00.000Z".toCostaRicaTimeLabel())
  }
}
