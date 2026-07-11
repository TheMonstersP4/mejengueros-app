package io.github.themonstersp4.mejengueros.presentation.reservation

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservableDay
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaDateLabel
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
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
  private companion object {
    const val COSTA_RICA_UTC_ROLLOVER_INSTANT = "2026-07-05T02:24:00.000Z"
    const val COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE = "2026-07-04"
  }

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
                                            "2026-07-17T00:00:00.000Z",
                                            "2026-07-17T01:00:00.000Z",
                                        ),
                                        reservableSlot(
                                            "2026-07-17T02:00:00.000Z",
                                            "2026-07-17T03:00:00.000Z",
                                        ),
                                    ),
                            )
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                reservableDatesLoader = { dates },
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
                                        "2026-07-17T00:00:00.000Z",
                                        "2026-07-17T01:00:00.000Z",
                                    )
                                ),
                            ),
                        "2026-07-17" to
                            reservationAvailability(
                                "2026-07-17",
                                listOf(
                                    reservableSlot(
                                        "2026-07-18T03:00:00.000Z",
                                        "2026-07-18T04:00:00.000Z",
                                    )
                                ),
                            ),
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                reservableDatesLoader = { dates },
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
  fun initRequestsCostaRicaBusinessDateForDiscoveryAtUtcRollover() =
      runTest(dispatcher) {
        val repository = FakeReservationRepository(availabilityByDate = emptyMap())
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                todayDateProvider = { COSTA_RICA_UTC_ROLLOVER_INSTANT.toCostaRicaDateLabel() },
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertEquals(
            listOf(COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE to 14),
            repository.discoveryRequests,
        )
        assertEquals(emptyList(), viewModel.uiState.value.dates)
      }

  @Test
  fun initUsesCostaRicaCivilDateLabelsForDiscoveryAtUtcRollover() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE to
                            reservationAvailability(
                                COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE,
                                listOf(
                                    reservableSlot(
                                        "2026-07-05T03:00:00.000Z",
                                        "2026-07-05T04:00:00.000Z",
                                    )
                                ),
                            )
                    ),
                discoveryResult =
                    ReservationDayDiscovery(
                        fromUtc = COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE,
                        days = 14,
                        reservableDays =
                            listOf(
                                ReservableDay(
                                    dateUtc = COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE,
                                    availabilityStatus = ReservationAvailabilityStatus.Available,
                                    availableSlotsCount = 1,
                                )
                            ),
                    ),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                todayDateProvider = { COSTA_RICA_UTC_ROLLOVER_INSTANT.toCostaRicaDateLabel() },
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertEquals(
            listOf(COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE to 14),
            repository.discoveryRequests,
        )
        assertEquals(listOf(COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE), repository.requestedDates)
        assertEquals("Hoy", viewModel.uiState.value.dates.first().dayLabel)
        assertEquals("04", viewModel.uiState.value.dates.first().dateLabel)
        assertEquals("Hoy 04", viewModel.uiState.value.dates.first().summaryLabel)
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
                                        "2026-07-17T01:00:00.000Z",
                                        "2026-07-17T02:00:00.000Z",
                                    )
                                ),
                            ),
                    ),
                confirmation =
                    ReservationConfirmation(
                        id = "reservation-id",
                        courtId = "court-id",
                        startsAtUtc = "2026-07-17T01:00:00.000Z",
                        endsAtUtc = "2026-07-17T02:00:00.000Z",
                        status = "CONFIRMED",
                    ),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                reservableDatesLoader = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        val selectedSlotId = viewModel.uiState.value.slots.first().id
        viewModel.selectSlot(selectedSlotId)
        viewModel.confirmReservation()
        advanceUntilIdle()

        val success = assertIs<ReservationUiMode.Success>(viewModel.uiState.value.mode)
        assertEquals(listOf("2026-07-17T01:00:00.000Z"), repository.createdStartsAt)
        assertEquals("Mejengas CR · Cancha 1", success.ticket.courtLabel)
        assertEquals("San José · Escazú", success.ticket.locationLabel)
        assertEquals("19:00 – 20:00", success.ticket.timeLabel)
      }

  @Test
  fun conflictTransitionsIntoConflictStateAndViewOtherHoursReloadsSelection() =
      runTest(dispatcher) {
        val errorReporter = FakeErrorReporter()
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-17T01:00:00.000Z",
                                        "2026-07-17T02:00:00.000Z",
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
                errorReporter = errorReporter,
                reservableDatesLoader = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectSlot(viewModel.uiState.value.slots.first().id)
        viewModel.confirmReservation()
        advanceUntilIdle()

        val conflict = assertIs<ReservationUiMode.Conflict>(viewModel.uiState.value.mode)
        assertEquals("19:00 – 20:00", conflict.attemptedTimeLabel)
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "reservation_submit_failed",
                    attributes =
                        mapOf(
                            "operation" to "create_reservation",
                            "selected_date" to "2026-07-16",
                            "error_source" to "app_api",
                            "status_code" to "409",
                        ),
                )
            ),
            errorReporter.events,
        )

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
                reservableDatesLoader = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectDate(1)
        secondDateResponse.complete(
            reservationAvailability(
                "2026-07-17",
                listOf(reservableSlot("2026-07-18T03:00:00.000Z", "2026-07-18T04:00:00.000Z")),
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedDateIndex)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })

        firstDateResponse.complete(
            reservationAvailability(
                "2026-07-16",
                listOf(reservableSlot("2026-07-17T00:00:00.000Z", "2026-07-17T01:00:00.000Z")),
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
                                        "2026-07-17T01:00:00.000Z",
                                        "2026-07-17T02:00:00.000Z",
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
                reservableDatesLoader = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectSlot(viewModel.uiState.value.slots.first().id)

        viewModel.confirmReservation()
        viewModel.confirmReservation()
        advanceUntilIdle()

        assertEquals(listOf("2026-07-17T01:00:00.000Z"), repository.createdStartsAt)
        assertEquals(true, viewModel.uiState.value.isSubmitting)

        createReservationResult.complete(
            ReservationConfirmation(
                id = "reservation-id",
                courtId = "court-id",
                startsAtUtc = "2026-07-17T01:00:00.000Z",
                endsAtUtc = "2026-07-17T02:00:00.000Z",
                status = "CONFIRMED",
            )
        )
        advanceUntilIdle()

        assertIs<ReservationUiMode.Success>(viewModel.uiState.value.mode)
        assertEquals(listOf("2026-07-17T01:00:00.000Z"), repository.createdStartsAt)
      }

  @Test
  fun initUsesBackendReservableDatesOnly() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-18" to
                            reservationAvailability(
                                "2026-07-18",
                                listOf(
                                    reservableSlot(
                                        "2026-07-19T03:00:00.000Z",
                                        "2026-07-19T04:00:00.000Z",
                                    )
                                ),
                            )
                    )
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                reservableDatesLoader = {
                  listOf(
                      ReservationDateUiModel(
                          utcDate = "2026-07-18",
                          dayLabel = "Sáb",
                          dateLabel = "18",
                          summaryLabel = "Sáb 18",
                          ticketLabel = "Sáb, 18 de julio",
                      )
                  )
                },
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertEquals(listOf("2026-07-18"), viewModel.uiState.value.dates.map { it.utcDate })
        assertEquals(listOf("2026-07-18"), repository.requestedDates)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })
      }

  @Test
  fun initUsesDiscoveryReferenceDateForBackendDrivenLabels() =
      runTest(dispatcher) {
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-18" to
                            reservationAvailability(
                                "2026-07-18",
                                listOf(
                                    reservableSlot(
                                        "2026-07-19T03:00:00.000Z",
                                        "2026-07-19T04:00:00.000Z",
                                    )
                                ),
                            )
                    ),
                discoveryResult =
                    ReservationDayDiscovery(
                        fromUtc = "2026-07-18",
                        days = 14,
                        reservableDays =
                            listOf(
                                ReservableDay(
                                    dateUtc = "2026-07-18",
                                    availabilityStatus = ReservationAvailabilityStatus.Available,
                                    availableSlotsCount = 1,
                                ),
                                ReservableDay(
                                    dateUtc = "2026-07-20",
                                    availabilityStatus = ReservationAvailabilityStatus.Available,
                                    availableSlotsCount = 2,
                                ),
                            ),
                    ),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertEquals(
            listOf("2026-07-18", "2026-07-20"),
            viewModel.uiState.value.dates.map { it.utcDate },
        )
        assertEquals("Hoy", viewModel.uiState.value.dates[0].dayLabel)
        assertEquals("Hoy, 18 de julio", viewModel.uiState.value.dates[0].ticketLabel)
        assertEquals("Lun", viewModel.uiState.value.dates[1].dayLabel)
        assertEquals(listOf("2026-07-18"), repository.requestedDates)
      }

  @Test
  fun initWithNoReservableDatesExposesEmptySelectionState() =
      runTest(dispatcher) {
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = FakeReservationRepository(),
                reservableDatesLoader = { emptyList() },
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingSlots)
        assertEquals(emptyList(), viewModel.uiState.value.dates)
        assertEquals(emptyList(), viewModel.uiState.value.slots)
        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertFalse(viewModel.uiState.value.canConfirm)
      }

  @Test
  fun initDiscoveryFailureExposesLoadErrorWithoutDatesOrSlotsAndReportsRecoverableFailure() =
      runTest(dispatcher) {
        val errorReporter = FakeErrorReporter()
        val repository =
            FakeReservationRepository(
                discoveryBlock = { _, _ -> throw AppApiException(500, "Discovery failed") }
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                errorReporter = errorReporter,
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingSlots)
        assertEquals(emptyList(), viewModel.uiState.value.dates)
        assertEquals(emptyList(), viewModel.uiState.value.slots)
        assertEquals(
            "No pudimos cargar los horarios disponibles. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )
        assertEquals(1, repository.discoveryRequests.size)
        assertEquals(emptyList(), repository.requestedDates)
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "reservation_discovery_load_failed",
                    attributes =
                        mapOf(
                            "operation" to "load_reservable_days",
                            "error_source" to "app_api",
                            "status_code" to "500",
                        ),
                )
            ),
            errorReporter.events,
        )
      }

  @Test
  fun selectDateSlotLoadFailureReportsRecoverableFailureForTheSelectedBusinessDate() =
      runTest(dispatcher) {
        val errorReporter = FakeErrorReporter()
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-16" to
                            reservationAvailability(
                                "2026-07-16",
                                listOf(
                                    reservableSlot(
                                        "2026-07-17T01:00:00.000Z",
                                        "2026-07-17T02:00:00.000Z",
                                    )
                                ),
                            )
                    ),
                availabilityLoads =
                    mapOf("2026-07-17" to { throw AppApiException(503, "Slots failed") }),
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                errorReporter = errorReporter,
                reservableDatesLoader = { dates },
                coroutineScope = this,
            )

        advanceUntilIdle()
        viewModel.selectDate(1)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedDateIndex)
        assertEquals(emptyList(), viewModel.uiState.value.slots)
        assertEquals(
            "No pudimos cargar los horarios disponibles. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "reservation_slots_load_failed",
                    attributes =
                        mapOf(
                            "operation" to "load_reservable_slots",
                            "selected_date" to "2026-07-17",
                            "error_source" to "app_api",
                            "status_code" to "503",
                        ),
                )
            ),
            errorReporter.events,
        )
      }

  @Test
  fun retryAfterInitialDiscoveryFailureReloadsDiscoveryAndFirstDateSlots() =
      runTest(dispatcher) {
        var shouldFailDiscovery = true
        val repository =
            FakeReservationRepository(
                availabilityByDate =
                    mapOf(
                        "2026-07-18" to
                            reservationAvailability(
                                "2026-07-18",
                                listOf(
                                    reservableSlot(
                                        "2026-07-19T03:00:00.000Z",
                                        "2026-07-19T04:00:00.000Z",
                                    )
                                ),
                            )
                    ),
                discoveryBlock = { _, days ->
                  if (shouldFailDiscovery) {
                    throw AppApiException(500, "Discovery failed")
                  }
                  ReservationDayDiscovery(
                      fromUtc = "2026-07-18",
                      days = days,
                      reservableDays =
                          listOf(
                              ReservableDay(
                                  dateUtc = "2026-07-18",
                                  availabilityStatus = ReservationAvailabilityStatus.Available,
                                  availableSlotsCount = 1,
                              )
                          ),
                  )
                },
            )
        val viewModel =
            ReservationViewModel(
                context = reservationContext(),
                repository = repository,
                coroutineScope = this,
            )

        advanceUntilIdle()
        assertEquals(
            "No pudimos cargar los horarios disponibles. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )

        shouldFailDiscovery = false
        viewModel.retryLoad()
        advanceUntilIdle()

        assertEquals(2, repository.discoveryRequests.size)
        assertEquals(listOf("2026-07-18"), repository.requestedDates)
        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertEquals(listOf("2026-07-18"), viewModel.uiState.value.dates.map { it.utcDate })
        assertEquals(0, viewModel.uiState.value.selectedDateIndex)
        assertEquals(listOf("21:00"), viewModel.uiState.value.slots.map { it.label })
      }
}

private class FakeErrorReporter : ErrorReporter {
  val events = mutableListOf<ReportedFailure>()

  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    events += ReportedFailure(name = name, attributes = attributes)
  }
}

private data class ReportedFailure(
    val name: String,
    val attributes: Map<String, String>,
)

private class FakeReservationRepository(
    private val availabilityByDate: Map<String, ReservationDayAvailability> = emptyMap(),
    private val availabilityLoads: Map<String, suspend () -> ReservationDayAvailability> =
        emptyMap(),
    private val discoveryResult: ReservationDayDiscovery? = null,
    private val discoveryBlock: (suspend (String, Int) -> ReservationDayDiscovery)? = null,
    private val confirmation: ReservationConfirmation? = null,
    private val createError: Throwable? = null,
    private val createReservationBlock: (suspend (String, String) -> ReservationConfirmation)? =
        null,
) : IReservationRepository {
  val discoveryRequests = mutableListOf<Pair<String, Int>>()
  val requestedDates = mutableListOf<String>()
  val createdStartsAt = mutableListOf<String>()

  override suspend fun getReservableDays(
      courtId: String,
      fromUtcDate: String,
      days: Int,
  ): ReservationDayDiscovery {
    discoveryRequests += fromUtcDate to days
    return discoveryBlock?.invoke(fromUtcDate, days)
        ?: discoveryResult
        ?: ReservationDayDiscovery(
            fromUtc = fromUtcDate,
            days = days,
            reservableDays =
                availabilityByDate.keys.sorted().map { dateUtc ->
                  ReservableDay(
                      dateUtc = dateUtc,
                      availabilityStatus = ReservationAvailabilityStatus.Available,
                      availableSlotsCount = availabilityByDate.getValue(dateUtc).slots.size,
                  )
                },
        )
  }

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

  override suspend fun getMyReservations(): MyReservations =
      MyReservations(emptyList(), emptyList())

  override suspend fun getOwnerReservations(courtId: String?): OwnerReservations =
      error("Unused in test")
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
