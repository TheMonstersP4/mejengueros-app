package io.github.themonstersp4.mejengueros.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectedLocationTest {

  @Test
  fun formatsCoordinatePairWithFiveDecimals() {
    val location = SelectedLocation(latitude = 9.9351029, longitude = -84.0911034)

    assertEquals("9.93510, -84.09110", location.toCoordinatePairText())
    assertEquals("9.93510", location.latitude.toCoordinateText())
  }

  @Test
  fun padsTrailingZerosWhenNeeded() {
    val location = SelectedLocation(latitude = 10.0, longitude = -84.1)

    assertEquals("10.00000, -84.10000", location.toCoordinatePairText())
  }

  @Test
  fun roundsAcrossIntegerBoundaryWhenFormattingCoordinates() {
    val location = SelectedLocation(latitude = 9.999996, longitude = -84.999996)

    assertEquals("10.00000, -85.00000", location.toCoordinatePairText())
  }

  @Test
  fun formatsCoordinateBoundariesWithRequestedPrecision() {
    val location = SelectedLocation(latitude = -90.0, longitude = 180.0)

    assertEquals("-90.00000, 180.00000", location.toCoordinatePairText())
  }
}
