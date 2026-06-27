package io.github.themonstersp4.mejengueros.presentation.availability

private val WholeHourPattern = Regex("^(?:[01]\\d|2[0-3]):00$")

fun generatePreviewSlots(startTime: String, endTime: String): List<String>? {
  if (!WholeHourPattern.matches(startTime) || !WholeHourPattern.matches(endTime)) {
    return null
  }

  val startMinutes = startTime.toMinutes()
  val endMinutes = endTime.toMinutes()
  val durationMinutes = endMinutes - startMinutes
  if (durationMinutes < 60 || durationMinutes % 60 != 0) {
    return null
  }

  return buildList {
    var currentMinutes = startMinutes
    while (currentMinutes < endMinutes) {
      add(currentMinutes.toTimeLabel())
      currentMinutes += 60
    }
  }
}

private fun String.toMinutes(): Int {
  val (hours, minutes) = split(":").map(String::toInt)
  return (hours * 60) + minutes
}

private fun Int.toTimeLabel(): String {
  val hours = this / 60
  val minutes = this % 60
  return hours.toString().padStart(2, '0') + ":" + minutes.toString().padStart(2, '0')
}
