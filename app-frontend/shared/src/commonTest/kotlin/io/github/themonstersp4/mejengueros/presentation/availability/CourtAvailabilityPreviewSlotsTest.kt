package io.github.themonstersp4.mejengueros.presentation.availability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourtAvailabilityPreviewSlotsTest {
  @Test
  fun generatePreviewSlotsReturnsOneHourSlotsUntilBeforeEndTime() {
    assertEquals(
        listOf("06:00", "07:00", "08:00"),
        generatePreviewSlots(startTime = "06:00", endTime = "09:00"),
    )
  }

  @Test
  fun generatePreviewSlotsReturnsNullWhenRangeDoesNotProduceExactOneHourSlots() {
    assertNull(generatePreviewSlots(startTime = "06:30", endTime = "09:00"))
  }
}
