package io.github.themonstersp4.mejengueros.domain.model

import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaTimeLabel

data class ReservableSlot(
    val startsAtUtc: String,
    val endsAtUtc: String,
) {
  /** Formats the UTC instant as Costa Rica business local time (HH:mm). */
  val displayStartTime: String
    get() = startsAtUtc.toCostaRicaTimeLabel()

  val displayEndTime: String
    get() = endsAtUtc.toCostaRicaTimeLabel()

  val displayRange: String
    get() = "$displayStartTime – $displayEndTime"
}
