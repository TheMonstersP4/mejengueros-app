package io.github.themonstersp4.mejengueros.presentation.myreservations

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.MyReservationCard
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MyReservationsViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun initLoadsReservationsAndKeepsBackendReviewLabels() =
      runTest(dispatcher) {
        val viewModel =
            MyReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(
                        myReservations =
                            MyReservations(
                                upcoming =
                                    listOf(
                                        reservationCard(
                                            id = "upcoming-id",
                                            section = "UPCOMING",
                                            reviewStatus = "NOT_APPLICABLE",
                                            canReview = false,
                                            hasReview = false,
                                        )
                                    ),
                                finalized =
                                    listOf(
                                        reservationCard(
                                            id = "finalized-id",
                                            section = "FINALIZED",
                                            reviewStatus = "PENDING_REVIEW",
                                            canReview = true,
                                            hasReview = false,
                                            primaryActionKey = "leave_review",
                                            primaryActionLabel = "CTA backend directa",
                                            indicatorKey = "already_reviewed",
                                            indicatorLabel = "Indicador backend directo",
                                        )
                                    ),
                            )
                    ),
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.upcoming.size)
        assertEquals(1, viewModel.uiState.value.finalized.size)
        assertEquals(
            "CTA backend directa",
            viewModel.uiState.value.finalized.first().primaryActionLabel,
        )
        assertEquals(
            "Indicador backend directo",
            viewModel.uiState.value.finalized.first().indicatorLabel,
        )
        assertEquals(
            "Reserva del 2026-07-10 · 12:00 – 13:00",
            viewModel.uiState.value.upcoming.first().reservationLabel,
        )
      }

  @Test
  fun initWithNoReservationsExposesEmptyState() =
      runTest(dispatcher) {
        val viewModel =
            MyReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(
                        myReservations = MyReservations(emptyList(), emptyList())
                    ),
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
      }

  @Test
  fun loadFailureShowsFriendlyErrorAndReportsRecoverableFailure() =
      runTest(dispatcher) {
        val errorReporter = FakeErrorReporter()
        val viewModel =
            MyReservationsViewModel(
                reservationRepository =
                    FakeReservationRepository(loadError = AppApiException(500, "boom")),
                errorReporter = errorReporter,
                coroutineScope = this,
            )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No pudimos cargar tus reservas. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "my_reservations_load_failed",
                    attributes = mapOf("error_source" to "app_api", "status_code" to "500"),
                )
            ),
            errorReporter.failures,
        )
      }

  @Test
  fun authFailuresShowSessionExpiredMessageFor401And403() =
      runTest(dispatcher) {
        listOf(401, 403).forEach { statusCode ->
          val errorReporter = FakeErrorReporter()
          val viewModel =
              MyReservationsViewModel(
                  reservationRepository =
                      FakeReservationRepository(
                          loadError = AppApiException(statusCode, "auth error")
                      ),
                  errorReporter = errorReporter,
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoading)
          assertEquals(
              "Necesitás volver a iniciar sesión para ver tus reservas.",
              viewModel.uiState.value.loadErrorMessage,
          )
          assertEquals(
              listOf(
                  ReportedFailure(
                      name = "my_reservations_load_failed",
                      attributes =
                          mapOf(
                              "error_source" to "app_api",
                              "status_code" to statusCode.toString(),
                          ),
                  )
              ),
              errorReporter.failures,
          )
        }
      }

  @Test
  fun olderSlowSuccessCannotOverwriteNewerRefreshError() =
      runTest(dispatcher) {
        val firstLoad = CompletableDeferred<Result<MyReservations>>()
        val secondLoad = CompletableDeferred<Result<MyReservations>>()
        val errorReporter = FakeErrorReporter()
        val reservationRepository =
            SequencedReservationRepository(listOf(firstLoad, secondLoad))
        val viewModel =
            MyReservationsViewModel(
                reservationRepository = reservationRepository,
                errorReporter = errorReporter,
                coroutineScope = this,
            )

        runCurrent()

        viewModel.refresh()
        runCurrent()

        secondLoad.complete(Result.failure(AppApiException(500, "newer boom")))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No pudimos cargar tus reservas. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )

        firstLoad.complete(Result.success(sampleReservations(finalizedId = "stale-success-id")))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No pudimos cargar tus reservas. Intentá nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )
        assertTrue(viewModel.uiState.value.finalized.isEmpty())
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "my_reservations_load_failed",
                    attributes = mapOf("error_source" to "app_api", "status_code" to "500"),
                )
            ),
            errorReporter.failures,
        )
        assertEquals(1, reservationRepository.cancellationCount)
      }

  @Test
  fun olderSlowFailureCannotOverwriteNewerRefreshSuccess() =
      runTest(dispatcher) {
        val firstLoad = CompletableDeferred<Result<MyReservations>>()
        val secondLoad = CompletableDeferred<Result<MyReservations>>()
        val errorReporter = FakeErrorReporter()
        val latestReservations = sampleReservations(finalizedId = "fresh-success-id")
        val reservationRepository =
            SequencedReservationRepository(listOf(firstLoad, secondLoad))
        val viewModel =
            MyReservationsViewModel(
                reservationRepository = reservationRepository,
                errorReporter = errorReporter,
                coroutineScope = this,
            )

        runCurrent()

        viewModel.refresh()
        runCurrent()

        secondLoad.complete(Result.success(latestReservations))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("fresh-success-id", viewModel.uiState.value.finalized.first().id)
        assertEquals(null, viewModel.uiState.value.loadErrorMessage)

        firstLoad.complete(Result.failure(AppApiException(500, "stale boom")))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("fresh-success-id", viewModel.uiState.value.finalized.first().id)
        assertEquals(null, viewModel.uiState.value.loadErrorMessage)
        assertTrue(errorReporter.failures.isEmpty())
        assertEquals(1, reservationRepository.cancellationCount)
      }
}

private class FakeReservationRepository(
    private val myReservations: MyReservations = MyReservations(emptyList(), emptyList()),
    private val loadError: Throwable? = null,
) : IReservationRepository {
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

  override suspend fun getMyReservations(): MyReservations {
    loadError?.let { throw it }
    return myReservations
  }
}

private class SequencedReservationRepository(
    private val results: List<CompletableDeferred<Result<MyReservations>>>,
) : IReservationRepository {
  private var callIndex: Int = 0
  var cancellationCount: Int = 0
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

  override suspend fun getMyReservations(): MyReservations {
    val deferred = results.getOrElse(callIndex) { results.last() }
    callIndex += 1

    return try {
      deferred.await().getOrThrow()
    } catch (error: CancellationException) {
      cancellationCount += 1
      throw error
    }
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

private fun sampleReservations(finalizedId: String = "finalized-id"): MyReservations =
    MyReservations(
        upcoming =
            listOf(
                reservationCard(
                    id = "upcoming-id",
                    section = "UPCOMING",
                    reviewStatus = "NOT_APPLICABLE",
                    canReview = false,
                    hasReview = false,
                )
            ),
        finalized =
            listOf(
                reservationCard(
                    id = finalizedId,
                    section = "FINALIZED",
                    reviewStatus = "PENDING_REVIEW",
                    canReview = true,
                    hasReview = false,
                    primaryActionKey = "leave_review",
                    primaryActionLabel = "CTA backend directa",
                    indicatorKey = "pending_review",
                    indicatorLabel = "Pendiente de reseña",
                )
            ),
    )

private fun reservationCard(
    id: String,
    section: String,
    reviewStatus: String,
    canReview: Boolean,
    hasReview: Boolean,
    primaryActionKey: String? = null,
    primaryActionLabel: String? = null,
    indicatorKey: String? = null,
    indicatorLabel: String? = null,
) =
    MyReservationCard(
        id = id,
        complexName = "Moravia FC",
        courtName = "Cancha 1",
        startsAt = "2026-07-10T18:00:00.000Z",
        endsAt = "2026-07-10T19:00:00.000Z",
        status = if (section == "UPCOMING") "CONFIRMED" else "COMPLETED",
        section = section,
        reviewStatus = reviewStatus,
        canReview = canReview,
        hasReview = hasReview,
        primaryActionKey = primaryActionKey,
        primaryActionLabel = primaryActionLabel,
        indicatorKey = indicatorKey,
        indicatorLabel = indicatorLabel,
    )
