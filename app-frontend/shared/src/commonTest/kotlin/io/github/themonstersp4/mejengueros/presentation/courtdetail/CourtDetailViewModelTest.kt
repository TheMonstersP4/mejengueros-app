package io.github.themonstersp4.mejengueros.presentation.courtdetail

import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CourtDetailViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun initLoadsReservableSlotsAndExposesThemAsDisplayTimes() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability)
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = repository,
                  coroutineScope = this,
              )

          assertTrue(viewModel.uiState.value.isLoadingSlots)

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoadingSlots)
          assertEquals("Hoy · slots de 1 hora", viewModel.uiState.value.availabilityHeadline)
          assertNull(viewModel.uiState.value.slotsErrorMessage)
          assertEquals(
              listOf("08:00", "09:00", "10:00"),
              viewModel.uiState.value.slots.map { it.displayTime },
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initWithEmptyResponseExposesEmptySlotsListWithoutError() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = FakeCourtDetailRepository(availabilityPreview = null)
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = repository,
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoadingSlots)
          assertNull(viewModel.uiState.value.availabilityHeadline)
          assertNull(viewModel.uiState.value.slotsErrorMessage)
          assertTrue(viewModel.uiState.value.slots.isEmpty())
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initFailureExposesErrorMessageAndEmptySlots() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = FailingCourtDetailRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoadingSlots)
          assertNotNull(viewModel.uiState.value.slotsErrorMessage)
          assertTrue(viewModel.uiState.value.slots.isEmpty())
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun retryLoadClearsErrorAndRestoresSlotsAfterTransientFailure() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = FlakyCourtDetailRepository(availabilityPreview = defaultAvailability)
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = repository,
                  coroutineScope = this,
              )

          advanceUntilIdle()
          assertNotNull(viewModel.uiState.value.slotsErrorMessage)

          viewModel.retryLoad()
          advanceUntilIdle()

          assertNull(viewModel.uiState.value.slotsErrorMessage)
          assertEquals(3, viewModel.uiState.value.slots.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initWithNextAvailableDayExposesFutureAvailabilityHeadline() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository =
              FakeCourtDetailRepository(
                  availabilityPreview =
                      ReservationDayAvailability(
                          dateUtc = "2026-07-02",
                          referenceDateUtc = "2026-07-01",
                          availabilityStatus = ReservationAvailabilityStatus.Available,
                          slots = defaultSlots,
                      )
              )
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = repository,
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertEquals(
              "Próximo día disponible · Jue, 2 de julio",
              viewModel.uiState.value.availabilityHeadline,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initUsesRepositoryReferenceDateForAvailabilityHeadlineClassification() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository =
              FakeCourtDetailRepository(
                  availabilityPreview =
                      ReservationDayAvailability(
                          dateUtc = "2026-07-02",
                          referenceDateUtc = "2026-07-02",
                          availabilityStatus = ReservationAvailabilityStatus.Available,
                          slots = defaultSlots,
                      )
              )
          val viewModel =
              CourtDetailViewModel(
                  courtId = "court-1",
                  repository = repository,
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertEquals("Hoy · slots de 1 hora", viewModel.uiState.value.availabilityHeadline)
        } finally {
          Dispatchers.resetMain()
        }
      }
}

private val defaultSlots =
    listOf(
        ReservableSlot(
            startsAtUtc = "2026-07-01T08:00:00.000Z",
            endsAtUtc = "2026-07-01T09:00:00.000Z",
        ),
        ReservableSlot(
            startsAtUtc = "2026-07-01T09:00:00.000Z",
            endsAtUtc = "2026-07-01T10:00:00.000Z",
        ),
        ReservableSlot(
            startsAtUtc = "2026-07-01T10:00:00.000Z",
            endsAtUtc = "2026-07-01T11:00:00.000Z",
        ),
    )

private val defaultAvailability =
    ReservationDayAvailability(
        dateUtc = "2026-07-01",
        availabilityStatus = ReservationAvailabilityStatus.Available,
        slots = defaultSlots,
    )

private class FakeCourtDetailRepository(
    private val availabilityPreview: ReservationDayAvailability?,
) : ICourtDetailRepository {
  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability? = availabilityPreview
}

private class FailingCourtDetailRepository : ICourtDetailRepository {
  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability? {
    throw IllegalStateException("network error")
  }
}

private class FlakyCourtDetailRepository(
    private val availabilityPreview: ReservationDayAvailability,
) : ICourtDetailRepository {
  private var attempts = 0

  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability? {
    attempts += 1
    if (attempts == 1) throw IllegalStateException("transient failure")
    return availabilityPreview
  }
}
