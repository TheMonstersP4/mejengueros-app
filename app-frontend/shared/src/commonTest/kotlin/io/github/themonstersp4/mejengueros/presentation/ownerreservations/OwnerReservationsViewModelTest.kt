package io.github.themonstersp4.mejengueros.presentation.ownerreservations

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCard
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservations
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationsViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun initLoadsCourtsFilterAndGroupedReservations() =
      runTest(dispatcher) {
        val reservationRepository =
            FakeReservationRepository(
                ownerReservations =
                    OwnerReservations(
                        selectedCourtId = null,
                        upcoming = listOf(ownerCard(id = "upcoming-id", section = "UPCOMING")),
                        finalized = listOf(ownerCard(id = "finalized-id", section = "FINALIZED")),
                    )
            )
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository = reservationRepository,
                complexRepository = FakeComplexRepository(hub = hubWithCourts()),
                coroutineScope = this,
            )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.selectedCourtId)
        assertEquals(listOf("Cancha 1", "Cancha 2"), state.availableCourts.map { it.name })
        assertEquals(1, state.upcoming.size)
        assertEquals(1, state.finalized.size)
        assertEquals(
            "Reserva del 2026-07-10 · 12:00 – 13:00",
            state.upcoming.first().reservationLabel,
        )
      }

  @Test
  fun selectCourtReloadsFilteredByCourtId() =
      runTest(dispatcher) {
        val reservationRepository =
            FakeReservationRepository(
                ownerReservations =
                    OwnerReservations(
                        selectedCourtId = "court-2",
                        upcoming = listOf(ownerCard(id = "only-court-2", section = "UPCOMING")),
                        finalized = emptyList(),
                    )
            )
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository = reservationRepository,
                complexRepository = FakeComplexRepository(hub = hubWithCourts()),
                coroutineScope = this,
            )
        advanceUntilIdle()

        viewModel.selectCourt("court-2")
        advanceUntilIdle()

        assertEquals("court-2", reservationRepository.lastRequestedCourtId)
        assertEquals("court-2", viewModel.uiState.value.selectedCourtId)
        assertEquals("only-court-2", viewModel.uiState.value.upcoming.first().id)
      }

  @Test
  fun emptyReservationsExposeEmptyState() =
      runTest(dispatcher) {
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(
                        ownerReservations = OwnerReservations(null, emptyList(), emptyList())
                    ),
                complexRepository = FakeComplexRepository(hub = hubWithCourts()),
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
      }

  @Test
  fun loadFailureShowsFriendlyErrorAndReports() =
      runTest(dispatcher) {
        val errorReporter = FakeErrorReporter()
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(loadError = AppApiException(500, "boom")),
                complexRepository = FakeComplexRepository(hub = hubWithCourts()),
                errorReporter = errorReporter,
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No pudimos cargar las reservas de tus canchas. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )
        assertEquals(1, errorReporter.failures.size)
        assertEquals("owner_reservations_load_failed", errorReporter.failures.first().name)
      }

  @Test
  fun authFailureShowsSessionExpiredMessage() =
      runTest(dispatcher) {
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(loadError = AppApiException(401, "auth")),
                complexRepository = FakeComplexRepository(hub = hubWithCourts()),
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertEquals(
            "Necesitás volver a iniciar sesión para ver las reservas de tus canchas.",
            viewModel.uiState.value.loadErrorMessage,
        )
      }

  @Test
  fun courtsFailureStillLoadsReservationsWithoutFilterOptions() =
      runTest(dispatcher) {
        val viewModel =
            OwnerReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(
                        ownerReservations =
                            OwnerReservations(
                                selectedCourtId = null,
                                upcoming = listOf(ownerCard(id = "u", section = "UPCOMING")),
                                finalized = emptyList(),
                            )
                    ),
                complexRepository = FakeComplexRepository(hubError = AppApiException(500, "hub")),
                coroutineScope = this,
            )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.loadErrorMessage)
        assertTrue(state.availableCourts.isEmpty())
        assertEquals(1, state.upcoming.size)
      }
}

private fun ownerCard(id: String, section: String) =
    OwnerReservationCard(
        id = id,
        complexName = "Moravia FC",
        courtName = "Cancha 1",
        startsAt = "2026-07-10T18:00:00.000Z",
        endsAt = "2026-07-10T19:00:00.000Z",
        status = if (section == "UPCOMING") "CONFIRMED" else "COMPLETED",
        section = section,
    )

private fun hubWithCourts(): MyComplexHub =
    MyComplexHub(
        complexes =
            listOf(
                MyComplexHubComplex(
                    id = "complex-1",
                    name = "Moravia FC",
                    address = "San José",
                    provinceId = null,
                    cantonId = null,
                    latitude = null,
                    longitude = null,
                    status = "ACTIVE",
                    courts =
                        listOf(
                            MyComplexHubCourt(
                                id = "court-1",
                                name = "Cancha 1",
                                status = "ACTIVE",
                                availabilityStatus = CourtAvailabilitySetupStatus.CONFIGURED,
                            ),
                            MyComplexHubCourt(
                                id = "court-2",
                                name = "Cancha 2",
                                status = "ACTIVE",
                                availabilityStatus = CourtAvailabilitySetupStatus.PENDING,
                            ),
                        ),
                )
            )
    )

private class FakeReservationRepository(
    private val ownerReservations: OwnerReservations =
        OwnerReservations(null, emptyList(), emptyList()),
    private val loadError: Throwable? = null,
) : IReservationRepository {
  var lastRequestedCourtId: String? = null
    private set

  override suspend fun getReservableDays(
      courtId: String,
      fromUtcDate: String,
      days: Int,
  ): ReservationDayDiscovery = error("Unused in test")

  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): ReservationDayAvailability = error("Unused in test")

  override suspend fun createReservation(
      courtId: String,
      startsAtUtc: String,
  ): ReservationConfirmation = error("Unused in test")

  override suspend fun getMyReservations(): MyReservations = error("Unused in test")

  override suspend fun getOwnerReservations(courtId: String?): OwnerReservations {
    lastRequestedCourtId = courtId
    loadError?.let { throw it }
    // Mirror the backend, which echoes the requested court filter back to the client.
    return ownerReservations.copy(selectedCourtId = courtId)
  }
}

private class FakeComplexRepository(
    private val hub: MyComplexHub = MyComplexHub(emptyList()),
    private val hubError: Throwable? = null,
) : IComplexRepository {
  override suspend fun getProvinces(): List<Province> = error("Unused in test")

  override suspend fun getCantons(provinceId: String): List<Canton> = error("Unused in test")

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> =
      error("Unused in test")

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
      error("Unused in test")

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt =
      error("Unused in test")

  override suspend fun getMyComplexHub(): MyComplexHub {
    hubError?.let { throw it }
    return hub
  }
}

private class FakeErrorReporter : ErrorReporter {
  val failures = mutableListOf<ReportedFailure>()

  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    failures += ReportedFailure(name = name, attributes = attributes)
  }
}

private data class ReportedFailure(
    val name: String,
    val attributes: Map<String, String>,
)
