package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.domain.model.DefaultReservableDaysWindow
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.time.todayCostaRicaDateString

class CourtDetailRepository(
    private val reservationRepository: IReservationRepository,
    private val todayDateProvider: () -> String = { todayCostaRicaDateString() },
    private val previewDays: Int = DefaultReservableDaysWindow,
) : ICourtDetailRepository {
  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability? {
    val requestedReferenceDate = todayDateProvider()
    val discovery =
        reservationRepository.getReservableDays(
            courtId = courtId,
            fromUtcDate = requestedReferenceDate,
            days = previewDays.coerceAtLeast(1),
        )
    val firstReservableDay = discovery.reservableDays.firstOrNull() ?: return null

    return reservationRepository
        .getReservableSlots(courtId, firstReservableDay.dateUtc)
        .copy(referenceDateUtc = discovery.fromUtc)
  }
}
