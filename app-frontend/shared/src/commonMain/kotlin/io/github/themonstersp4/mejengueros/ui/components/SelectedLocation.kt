package io.github.themonstersp4.mejengueros.ui.components

import kotlin.math.pow
import kotlin.math.round

data class SelectedLocation(
    val latitude: Double,
    val longitude: Double,
)

internal const val DEFAULT_COORDINATE_TEXT_DECIMALS = 5

fun SelectedLocation.toCoordinatePairText(): String =
    "${latitude.toCoordinateText()}, ${longitude.toCoordinateText()}"

internal fun Double.toCoordinateText(decimals: Int = DEFAULT_COORDINATE_TEXT_DECIMALS): String {
  val factor = 10.0.pow(decimals)
  val roundedValue = round(this * factor) / factor
  val rawText = roundedValue.toString()
  val decimalSeparatorIndex = rawText.indexOf('.')

  if (decimalSeparatorIndex == -1) {
    return "$rawText.${"0".repeat(decimals)}"
  }

  val integerPart = rawText.substring(0, decimalSeparatorIndex)
  val decimalPart =
      rawText.substring(decimalSeparatorIndex + 1).padEnd(decimals, '0').take(decimals)
  return "$integerPart.$decimalPart"
}
