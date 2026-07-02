package io.github.themonstersp4.mejengueros.domain.model

data class ReservableSlot(
    val startsAtUtc: String,
    val endsAtUtc: String,
) {
  /** Extracts the HH:mm portion from an ISO-8601 UTC string such as "2026-07-01T18:00:00.000Z". */
  val displayStartTime: String
    get() {
      val tIndex = startsAtUtc.indexOf('T')
      return if (tIndex >= 0 && tIndex + 6 <= startsAtUtc.length) {
        startsAtUtc.substring(tIndex + 1, tIndex + 6)
      } else {
        startsAtUtc
      }
    }
}
