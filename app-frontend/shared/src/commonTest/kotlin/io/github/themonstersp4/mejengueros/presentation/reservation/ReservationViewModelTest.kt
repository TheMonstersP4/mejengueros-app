package io.github.themonstersp4.mejengueros.presentation.reservation

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dates = buildReservationDateOptions("2026-07-16", days = 3)

  @Test
  fun initLoadsFirstDateAvailabilityAndBuildsOccupiedGaps() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                dateUtc = "2026-07-16",
                                slots =
                                    listOf(
                                        reservableSlot(
                                            "2026-07-16T18:00:00.000Z",
                                            "2026-07-16T19:00:00.000Z",
                                        ),
                                        reservableSlot(
                                            "2026-07-16T20:00:00.000Z",
                                            "2026-07-16T21:00:00.000Z",
                                        ),
                                    ),
                            )
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingSlots)
        assertEquals(listOf("2026-07-16"), repository.requestedDates)
        assertEquals(
            listOf("18:00", "19:00", "20:00"),
            viewModel.uiState.value.slots.map { it.label },
        )
        assertEquals("Hoy", viewModel.uiState.value.dates.first().dayLabel)
        assertNull(viewModel.uiState.value.loadErrorMessage)
      }

  @Test
  fun selectDateReloadsAvailabilityForTheChosenDay() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-16T18:00:00.000Z",
                                        "2026-07-16T19:00:00.000Z",
                                    )
                                ),
                            ),
                        "2026-07-17" to
                            reservationAvailability(
                                "2026-07-17",
                                listOf(
                                    reservableSlot(
                                        "2026-07-17T21:00:00.000Z",
                                        "2026-07-17T22:00:00.000Z",
                                    )
                                ),
                            ),
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectDate(1)
        advanceUntilIdle()

        assertEquals(listOf("2026-07-16", "2026-07-17"), repository.requestedDates)
        assertEquals(1, viewModel.uiState.value.selectedDateIndex)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })
      }

  @Test
  fun confirmReservationTransitionsIntoSuccessState() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-16T19:00:00.000Z",
                                        "2026-07-16T20:00:00.000Z",
                                    )
                                ),
                            ),
                    ),
                confirmation =
                    ReservationConfirmation(
                        id = "reservation-id",
                        courtId = "court-id",
                        startsAtUtc = "2026-07-16T19:00:00.000Z",
                        endsAtUtc = "2026-07-16T20:00:00.000Z",
                        status = "CONFIRMED",
                    ),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        val selectedSlotId = viewModel.uiState.value.slots.first().id
        viewModel.selectSlot(selectedSlotId)
        viewModel.confirmReservation()
        advanceUntilIdle()

        val success = assertIs<ReservationUiMode.Success>(viewModel.uiState.value.mode)
        assertEquals(listOf("2026-07-16T19:00:00.000Z"), repository.createdStartsAt)
        assertEquals("Mejengas CR · Cancha 1", success.ticket.courtLabel)
        assertEquals("San José · Escazú", success.ticket.locationLabel)
        assertEquals("19:00 – 20:00", success.ticket.timeLabel)
      }

  @Test
  fun conflictTransitionsIntoConflictStateAndViewOtherHoursReloadsSelection() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-16T19:00:00.000Z",
                                        "2026-07-16T20:00:00.000Z",
                                    )
                                ),
                            ),
                    ),
                createError = AppApiException(409, "Conflict"),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectSlot(viewModel.uiState.value.slots.first().id)
        viewModel.confirmReservation()
        advanceUntilIdle()

        val conflict = assertIs<ReservationUiMode.Conflict>(viewModel.uiState.value.mode)
        assertEquals("19:00 – 20:00", conflict.attemptedTimeLabel)

        viewModel.viewOtherHours()
        advanceUntilIdle()

        assertEquals(ReservationUiMode.Selection, viewModel.uiState.value.mode)
        assertEquals(2, repository.requestedDates.size)
      }

  @Test
  fun buildReservationDateOptionsIsDeterministicAcrossMonthBoundaries() {
    val result = buildReservationDateOptions("2026-07-31", days = 3)

    assertEquals(listOf("2026-07-31", "2026-08-01", "2026-08-02"), result.map { it.utcDate })
    assertEquals(listOf("Hoy", "Sáb", "Dom"), result.map { it.dayLabel })
    assertEquals("Sáb, 1 de agosto", result[1].ticketLabel)
  }

  @Test
  fun selectDateIgnoresOlderAvailabilityResponsesFromPreviousSelections() =
      runTest(dispatcher) {
        val firstDateResponse = CompletableDeferred<ReservationDayAvailability>()
        val secondDateResponse = CompletableDeferred<ReservationDayAvailability>()
        val repository =
            FakeReservationRepository(
                availabilityLoads =
                    mapOf(
                        "2026-07-16" to { firstDateResponse.await() },
                        "2026-07-17" to { secondDateResponse.await() },
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        viewModel.selectDate(1)
        secondDateResponse.complete(
            reservationAvailability(
                "2026-07-17",
                listOf(reservableSlot("2026-07-17T21:00:00.000Z", "2026-07-17T22:00:00.000Z")),
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedDateIndex)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })

        firstDateResponse.complete(
            reservationAvailability(
                "2026-07-16",
                listOf(reservableSlot("2026-07-16T18:00:00.000Z", "2026-07-16T19:00:00.000Z")),
            )
        )
        advanceUntilIdle()

        assertEquals(listOf("2026-07-16", "2026-07-17"), repository.requestedDates)
        assertEquals(1, viewModel.uiState.value.selectedDateIndex)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })
      }

  @Test
  fun confirmReservationPreventsDuplicateSubmitWhileFirstRequestIsStillRunning() =
      runTest(dispatcher) {
        val createReservationResult = CompletableDeferred<ReservationConfirmation>()
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-16T19:00:00.000Z",
                                        "2026-07-16T20:00:00.000Z",
                                    )
                                ),
                            )
                    ),
                createReservationBlock = { _, _ -> createReservationResult.await() },
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                dateOptionsProvider = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectSlot(viewModel.uiState.value.slots.first().id)

        viewModel.confirmReservation()
        viewModel.confirmReservation()
        advanceUntilIdle()

        assertEquals(listOf("2026-07-16T19:00:00.000Z"), repository.createdStartsAt)
        assertEquals(true, viewModel.uiState.value.isSubmitting)

        createReservationResult.complete(
            ReservationConfirmation(
                id = "reservation-id",
                courtId = "court-id",
                startsAtUtc = "2026-07-16T19:00:00.000Z",
                endsAtUtc = "2026-07-16T20:00:00.000Z",
                status = "CONFIRMED",
            )
        )
        advanceUntilIdle()

        assertIs<ReservationUiMode.Success>(viewModel.uiState.value.mode)
        assertEquals(listOf("2026-07-16T19:00:00.000Z"), repository.createdStartsAt)
      }
}

private class FakeReservationRepository(
    private val availabilityByDate: Map<String, ReservationDayAvailability> = emptyMap(),
    private val availabilityLoads: Map<String, suspend () -> ReservationDayAvailability> =
        emptyMap(),
    private val confirmation: ReservationConfirmation? = null,
    private val createError: Throwable? = null,
    private val createReservationBlock: (suspend (String, String) -> ReservationConfirmation)? =
        null,
) : IReservationRepository {
  val requestedDates = mutableListOf<String>()
  val createdStartsAt = mutableListOf<String>()

  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): ReservationDayAvailability {
    requestedDates += dateUtc
    return availabilityLoads[dateUtc]?.invoke() ?: availabilityByDate.getValue(dateUtc)
  }

  override suspend fun createReservation(
      courtId: String,
      startsAtUtc: String,
  ): ReservationConfirmation {
    createdStartsAt += startsAtUtc
    createReservationBlock?.let {
      return it(courtId, startsAtUtc)
    }
    createError?.let { throw it }
    return requireNotNull(confirmation)
  }
}

private fun reservationContext() =
    ReservationContext(
        courtId = "court-id",
        complexId = "complex-id",
        complexName = "Mejengas CR",
        courtName = "Cancha 1",
        provinceName = "San José",
        cantonName = "Escazú",
    )

private fun reservationAvailability(
    dateUtc: String,
    slots: List<ReservableSlot>,
): ReservationDayAvailability =
    ReservationDayAvailability(
        dateUtc = dateUtc,
        availabilityStatus = ReservationAvailabilityStatus.Available,
        slots = slots,
    )

private fun reservableSlot(startsAtUtc: String, endsAtUtc: String): ReservableSlot =
    ReservableSlot(startsAtUtc = startsAtUtc, endsAtUtc = endsAtUtc)
