package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.domain.model.ReservableDay
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class CourtDetailRepositoryTest {
  @Test
  fun getUpcomingReservableSlotsPreviewLooksAheadUntilFirstAvailableDay() = runTest {
    val requestedDiscoveryWindows = mutableListOf<Pair<String, Int>>()
    val requestedSlotsDates = mutableListOf<String>()
    val repository =
        CourtDetailRepository(
            reservationRepository =
                fakeReservationRepository(
                    getReservableDays = { _, fromUtcDate, days ->
                      requestedDiscoveryWindows += fromUtcDate to days
                      ReservationDayDiscovery(
                          fromUtc = fromUtcDate,
                          days = days,
                          reservableDays =
                              listOf(
                                  ReservableDay(
                                      dateUtc = "2026-07-03",
                                      availabilityStatus = ReservationAvailabilityStatus.Available,
                                      availableSlotsCount = 1,
                                  )
                              ),
                      )
                    },
                    getReservableSlots = { _, dateUtc ->
                      requestedSlotsDates += dateUtc
                      reservationAvailability(
                          dateUtc = dateUtc,
                          slots =
                              listOf(
                                  ReservableSlot(
                                      startsAtUtc = "2026-07-03T18:00:00.000Z",
                                      endsAtUtc = "2026-07-03T19:00:00.000Z",
                                  )
                              ),
                      )
                    },
                ),
            todayDateProvider = { "2026-07-01" },
            previewDays = 14,
        )

    val result = repository.getUpcomingReservableSlotsPreview("court-id")

    assertEquals(listOf("2026-07-01" to 14), requestedDiscoveryWindows)
    assertEquals(listOf("2026-07-03"), requestedSlotsDates)
    assertEquals("2026-07-03", result?.dateUtc)
    assertEquals("2026-07-01", result?.referenceDateUtc)
    assertEquals(listOf("18:00"), result?.slots?.map { it.displayStartTime })
  }

  @Test
  fun getUpcomingReservableSlotsPreviewUsesBackendDiscoveryReferenceDate() = runTest {
    val repository =
        CourtDetailRepository(
            reservationRepository =
                fakeReservationRepository(
                    getReservableDays = { _, _, days ->
                      ReservationDayDiscovery(
                          fromUtc = "2026-07-04",
                          days = days,
                          reservableDays =
                              listOf(
                                  ReservableDay(
                                      dateUtc = "2026-07-05",
                                      availabilityStatus = ReservationAvailabilityStatus.Available,
                                      availableSlotsCount = 2,
                                  )
                              ),
                      )
                    },
                    getReservableSlots = { _, dateUtc ->
                      reservationAvailability(
                          dateUtc = dateUtc,
                          slots =
                              listOf(
                                  ReservableSlot(
                                      startsAtUtc = "2026-07-05T18:00:00.000Z",
                                      endsAtUtc = "2026-07-05T19:00:00.000Z",
                                  )
                              ),
                      )
                    },
                ),
            todayDateProvider = { "2026-07-01" },
            previewDays = 14,
        )

    val result = repository.getUpcomingReservableSlotsPreview("court-id")

    assertEquals("2026-07-05", result?.dateUtc)
    assertEquals("2026-07-04", result?.referenceDateUtc)
  }

  @Test
  fun getUpcomingReservableSlotsPreviewReturnsNullWhenWindowHasNoSlots() = runTest {
    val requestedSlotsDates = mutableListOf<String>()
    val repository =
        CourtDetailRepository(
            reservationRepository =
                fakeReservationRepository(
                    getReservableDays = { _, fromUtcDate, days ->
                      ReservationDayDiscovery(
                          fromUtc = fromUtcDate,
                          days = days,
                          reservableDays = emptyList(),
                      )
                    },
                    getReservableSlots = { _, dateUtc ->
                      requestedSlotsDates += dateUtc
                      reservationAvailability(dateUtc = dateUtc, slots = emptyList())
                    },
                ),
            todayDateProvider = { "2026-07-01" },
            previewDays = 3,
        )

    val result = repository.getUpcomingReservableSlotsPreview("court-id")

    assertEquals(emptyList(), requestedSlotsDates)
    assertNull(result)
  }

  @Test
  fun getUpcomingReservableSlotsPreviewUsesConfiguredDiscoveryWindow() = runTest {
    val requestedDiscoveryWindows = mutableListOf<Pair<String, Int>>()
    val repository =
        CourtDetailRepository(
            reservationRepository =
                fakeReservationRepository(
                    getReservableDays = { _, fromUtcDate, days ->
                      requestedDiscoveryWindows += fromUtcDate to days
                      ReservationDayDiscovery(
                          fromUtc = fromUtcDate,
                          days = days,
                          reservableDays = emptyList(),
                      )
                    },
                ),
            todayDateProvider = { "2026-07-01" },
            previewDays = 3,
        )

    val result = repository.getUpcomingReservableSlotsPreview("court-id")

    assertEquals(listOf("2026-07-01" to 3), requestedDiscoveryWindows)
    assertNull(result)
  }
}

private fun fakeReservationRepository(
    getReservableDays: suspend (String, String, Int) -> ReservationDayDiscovery,
    getReservableSlots: suspend (String, String) -> ReservationDayAvailability = { _, dateUtc ->
      reservationAvailability(dateUtc = dateUtc, slots = emptyList())
    },
) =
    object : IReservationRepository {
      override suspend fun getReservableDays(
          courtId: String,
          fromUtcDate: String,
          days: Int,
      ): ReservationDayDiscovery = getReservableDays(courtId, fromUtcDate, days)

      override suspend fun getReservableSlots(
          courtId: String,
          dateUtc: String,
      ): ReservationDayAvailability = getReservableSlots(courtId, dateUtc)

      override suspend fun createReservation(
          courtId: String,
          startsAtUtc: String,
      ): ReservationConfirmation = error("Unused in test")
    }

private fun reservationAvailability(
    dateUtc: String,
    slots: List<ReservableSlot>,
) =
    ReservationDayAvailability(
        dateUtc = dateUtc,
        availabilityStatus = ReservationAvailabilityStatus.Available,
        slots = slots,
    )
