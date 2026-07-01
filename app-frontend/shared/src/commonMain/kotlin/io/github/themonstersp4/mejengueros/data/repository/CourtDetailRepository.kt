package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtDetailRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import kotlin.time.Clock

class CourtDetailRepository(
    private val remoteDataSource: ICourtDetailRemoteDataSource,
    private val todayDateProvider: () -> String = { todayUtcDateString() },
) : ICourtDetailRepository {
  override suspend fun getReservableSlotsForToday(courtId: String): List<ReservableSlot> =
      remoteDataSource.getReservableSlots(courtId, todayDateProvider())
}

internal fun todayUtcDateString(): String =
    epochSecondsToUtcDateString(Clock.System.now().epochSeconds)

/** Converts a Unix epoch seconds timestamp to a YYYY-MM-DD UTC date string using pure Kotlin arithmetic. */
internal fun epochSecondsToUtcDateString(epochSeconds: Long): String {
  // Civil-from-days algorithm (Gregorian). Reference: http://howardhinnant.github.io/date_algorithms.html
  val z = epochSeconds / 86400L + 719468L
  val era = (if (z >= 0L) z else z - 146096L) / 146097L
  val doe = z - era * 146097L
  val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
  val y = yoe + era * 400L
  val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
  val mp = (5L * doy + 2L) / 153L
  val d = doy - (153L * mp + 2L) / 5L + 1L
  val m = if (mp < 10L) mp + 3L else mp - 9L
  val year = if (m <= 2L) y + 1L else y
  return "${year.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}
