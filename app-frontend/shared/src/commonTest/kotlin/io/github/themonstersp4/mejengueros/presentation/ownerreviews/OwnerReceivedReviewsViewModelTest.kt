package io.github.themonstersp4.mejengueros.presentation.ownerreviews

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.OwnerReceivedCourtFilter
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewCourt
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewer
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReceivedReviewsViewModelTest {

  @Test
  fun initLoadsCourtsAndFirstPageThenClearsLoadingState() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository =
        FakeComplexRepository(
            hub =
                MyComplexHub(
                    complexes =
                        listOf(
                            complex(
                                id = "complex-b",
                                name = "South Sports",
                                courts =
                                    listOf(
                                        court("court-z", "Z Court"),
                                        court("court-a", "A Court"),
                                    ),
                            ),
                            complex(
                                id = "complex-a",
                                name = "North Sports",
                                courts = listOf(court("court-m", "M Court")),
                            ),
                        ),
                )
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.loadErrorMessage)
    assertEquals(
        listOf(
            OwnerReceivedCourtFilter(courtId = "court-a", name = "A Court"),
            OwnerReceivedCourtFilter(courtId = "court-m", name = "M Court"),
            OwnerReceivedCourtFilter(courtId = "court-z", name = "Z Court"),
        ),
        state.availableCourts,
    )
    assertNull(state.selectedCourtId)
    assertEquals(1, reviewRepository.firstPageCalls.size)
    assertEquals(Triple(null, 1, 10), reviewRepository.firstPageCalls.single())
    assertEquals(3, state.items.size)
  }

  @Test
  fun selectCourtReloadsFirstPageWithChosenFilter() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository =
        FakeComplexRepository(
            hub =
                MyComplexHub(
                    complexes =
                        listOf(
                            complex(
                                id = "complex-a",
                                name = "North",
                                courts =
                                    listOf(
                                        court("court-1", "Court 1"),
                                        court("court-2", "Court 2"),
                                    ),
                            )
                        )
                )
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertEquals(1, reviewRepository.firstPageCalls.size)
    assertNull(viewModel.uiState.value.selectedCourtId)
    assertEquals(Triple(null, 1, 10), reviewRepository.firstPageCalls.single())

    viewModel.selectCourt("court-2")
    advanceUntilIdle()

    assertEquals(2, reviewRepository.firstPageCalls.size)
    assertEquals("court-2", reviewRepository.firstPageCalls.last().first)
    assertEquals(1, reviewRepository.firstPageCalls.last().second)
    assertEquals("court-2", viewModel.uiState.value.selectedCourtId)
  }

  @Test
  fun selectCourtWithNullSwitchesBackToAllAndReloads() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository =
        FakeComplexRepository(
            hub =
                MyComplexHub(
                    complexes =
                        listOf(
                            complex(
                                id = "complex-a",
                                name = "North",
                                courts =
                                    listOf(
                                        court("court-1", "Court 1"),
                                        court("court-2", "Court 2"),
                                    ),
                            )
                        )
                )
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.selectCourt("court-2")
    advanceUntilIdle()
    assertEquals("court-2", viewModel.uiState.value.selectedCourtId)

    viewModel.selectCourt(null)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedCourtId)
    assertEquals(3, reviewRepository.firstPageCalls.size)
    assertNull(reviewRepository.firstPageCalls.last().first)
  }

  @Test
  fun loadNextPageAppendsItemsAndStopsAtLastPage() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 10, page = 1, totalPages = 2, courtId = null),
            nextPageResponse = samplePage(items = 5, page = 2, totalPages = 2, courtId = null),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertEquals(10, viewModel.uiState.value.items.size)
    assertTrue(viewModel.uiState.value.hasNextPage)

    viewModel.loadNextPage()
    advanceUntilIdle()

    assertEquals(15, viewModel.uiState.value.items.size)
    assertEquals(2, viewModel.uiState.value.page)
    assertFalse(viewModel.uiState.value.hasNextPage)
    assertEquals(1, reviewRepository.nextPageCalls.size)
    assertEquals(Triple(null, 2, 10), reviewRepository.nextPageCalls.single())
  }

  @Test
  fun loadNextPageIsNoOpWhenCanLoadMoreIsFalse() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.hasNextPage)

    viewModel.loadNextPage()
    advanceUntilIdle()

    assertEquals(0, reviewRepository.nextPageCalls.size)
  }

  @Test
  fun loadMoreFailureExposesErrorWithoutDroppingItems() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 10, page = 1, totalPages = 2, courtId = null),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    reviewRepository.nextPageFailure = AppApiException(503, "boom")

    viewModel.loadNextPage()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(10, state.items.size)
    assertEquals(
        "No pudimos cargar más reseñas. Intentá de nuevo.",
        state.loadMoreErrorMessage,
    )
    assertFalse(state.isLoadingMore)
    assertFalse(
        state.canLoadMore,
        "canLoadMore must stay false while loadMoreErrorMessage is visible so the screen does not auto-retry.",
    )
  }

  @Test
  fun loadMoreAutoRetriesResumeAfterErrorIsAcknowledged() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 10, page = 1, totalPages = 2, courtId = null),
            nextPageResponse = samplePage(items = 5, page = 2, totalPages = 2, courtId = null),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    reviewRepository.nextPageFailure = AppApiException(503, "boom")

    viewModel.loadNextPage()
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.canLoadMore)

    viewModel.acknowledgeError()
    assertTrue(viewModel.uiState.value.canLoadMore)
  }

  @Test
  fun retryLoadMoreRecoversAfterLoadMoreFailureAndAppendsItems() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 10, page = 1, totalPages = 2, courtId = null),
            nextPageResponse = samplePage(items = 5, page = 2, totalPages = 2, courtId = null),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    reviewRepository.nextPageFailure = AppApiException(503, "boom")

    viewModel.loadNextPage()
    advanceUntilIdle()

    val erroredState = viewModel.uiState.value
    assertEquals(10, erroredState.items.size)
    assertEquals(
        "No pudimos cargar más reseñas. Intentá de nuevo.",
        erroredState.loadMoreErrorMessage,
    )
    assertFalse(erroredState.canLoadMore)
    assertEquals(1, reviewRepository.nextPageCalls.size)

    // Recovery: the repository stops failing, and the user presses the
    // visible "Reintentar" button (i.e. retryLoadMore, NOT loadNextPage).
    reviewRepository.nextPageFailure = null

    viewModel.retryLoadMore()
    advanceUntilIdle()

    val recoveredState = viewModel.uiState.value
    assertEquals(15, recoveredState.items.size)
    assertNull(recoveredState.loadMoreErrorMessage)
    assertEquals(2, recoveredState.page)
    assertFalse(recoveredState.hasNextPage)
    assertFalse(recoveredState.isLoadingMore)
    assertEquals(2, reviewRepository.nextPageCalls.size)
    assertEquals(Triple(null, 2, 10), reviewRepository.nextPageCalls.last())
  }

  @Test
  fun retryLoadMoreIsNoOpWhenHasNextPageIsFalse() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.hasNextPage)

    viewModel.retryLoadMore()
    advanceUntilIdle()

    assertEquals(0, reviewRepository.nextPageCalls.size)
  }

  @Test
  fun acknowledgeErrorClearsLoadAndLoadMoreErrors() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageFailure = AppApiException(500, "boom"),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.loadErrorMessage)

    viewModel.acknowledgeError()
    assertNull(viewModel.uiState.value.loadErrorMessage)
  }

  @Test
  fun refreshRetriesFirstPageAfterFailure() = runTest {
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 2, page = 1, totalPages = 1, courtId = null),
            firstPageFailures = ArrayDeque(listOf(AppApiException(500, "boom"))),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.loadErrorMessage)
    assertEquals(1, reviewRepository.firstPageCalls.size)

    viewModel.refresh()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.loadErrorMessage)
    assertEquals(2, viewModel.uiState.value.items.size)
    assertEquals(2, reviewRepository.firstPageCalls.size)
  }

  @Test
  fun refreshReloadsFirstPageAfterAPreviouslyEmptyLoad() = runTest {
    // Reproduces the bug: the NavDisplay retains this ViewModel, so an owner whose first
    // load returned empty keeps seeing an empty list on re-entry. The mount effect calls
    // refresh(), which must reload the first page and surface reviews that now exist.
    val reviewRepository =
        RecordingReviewRepository(
            firstPageResponse = samplePage(items = 0, page = 1, totalPages = 0, courtId = null),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.items.isEmpty())
    assertEquals(1, reviewRepository.firstPageCalls.size)

    reviewRepository.firstPageResponse =
        samplePage(items = 3, page = 1, totalPages = 1, courtId = null)

    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(3, viewModel.uiState.value.items.size)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.loadErrorMessage)
    assertEquals(2, reviewRepository.firstPageCalls.size)
  }

  @Test
  fun complexRepositoryFailureFallsBackToFirstPageWithoutChips() = runTest {
    val reviewRepository = RecordingReviewRepository()
    val complexRepository = FakeComplexRepository(failure = IllegalStateException("hub down"))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = FakeErrorReporter(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.availableCourts.isEmpty())
    assertNull(state.selectedCourtId)
    assertEquals(1, reviewRepository.firstPageCalls.size)
    assertEquals(3, state.items.size)
  }

  @Test
  fun firstPageApiFailureMapsToControlledErrorMessageAndReports() = runTest {
    val errorReporter = FakeErrorReporter()
    val reviewRepository =
        RecordingReviewRepository(
            firstPageFailure = AppApiException(401, "Unauthorized"),
        )
    val complexRepository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        OwnerReceivedReviewsViewModel(
            reviewRepository = reviewRepository,
            complexRepository = complexRepository,
            errorReporter = errorReporter,
            coroutineScope = scope,
        )

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(
        "No tenés permisos para ver las reseñas recibidas.",
        state.loadErrorMessage,
    )
    assertEquals(1, errorReporter.events.size)
    assertEquals("owner_received_reviews_load_failed", errorReporter.events.single().name)
  }

  private fun complex(
      id: String,
      name: String,
      courts: List<MyComplexHubCourt>,
  ): MyComplexHubComplex =
      MyComplexHubComplex(
          id = id,
          name = name,
          address = "$name address",
          provinceId = null,
          cantonId = null,
          latitude = null,
          longitude = null,
          status = "ACTIVE",
          courts = courts,
      )

  private fun court(id: String, name: String): MyComplexHubCourt =
      MyComplexHubCourt(
          id = id,
          name = name,
          status = "ACTIVE",
          availabilityStatus = CourtAvailabilitySetupStatus.CONFIGURED,
      )

  private fun samplePage(
      items: Int,
      page: Int,
      totalPages: Int,
      courtId: String?,
  ): ReceivedReviewPage =
      ReceivedReviewPage(
          items =
              (1..items).map { index ->
                ReceivedReview(
                    reviewId = "review-${page}-${index}",
                    rating = (index % 5) + 1,
                    comment = "Comentario $index",
                    createdAt = "2026-07-01T20:00:00.000Z",
                    court = ReceivedReviewCourt(id = courtId ?: "court-1", name = "Cancha A"),
                    reviewer =
                        ReceivedReviewer(
                            displayName = "Mejenguero $index",
                            initials = "M$index",
                        ),
                )
              },
          summary =
              ReceivedReviewsSummary(
                  selectedCourtId = courtId,
                  totalReviews = items * totalPages,
                  averageRating = 4.2,
              ),
          page = page,
          pageSize = 10,
          totalItems = items * totalPages,
          totalPages = totalPages,
          hasNextPage = page < totalPages,
      )
}

private class RecordingReviewRepository(
    var firstPageResponse: ReceivedReviewPage? =
        OwnerReceivedReviewsViewModelTestHelper.defaultPage(),
    private val nextPageResponse: ReceivedReviewPage? =
        OwnerReceivedReviewsViewModelTestHelper.defaultPage(),
    private val firstPageFailures: ArrayDeque<Throwable> = ArrayDeque(),
    var nextPageFailure: Throwable? = null,
    firstPageFailure: Throwable? = null,
) : IReviewRepository {
  val firstPageCalls = mutableListOf<Triple<String?, Int, Int>>()
  val nextPageCalls = mutableListOf<Triple<String?, Int, Int>>()

  init {
    if (firstPageFailure != null) {
      firstPageFailures.addLast(firstPageFailure)
    }
  }

  override suspend fun getLatestReviewableReservation() = null

  override suspend fun createReview(
      request: io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
  ) = error("Not used in owner reviews tests.")

  override suspend fun getOwnerReceivedReviews(
      courtId: String?,
      page: Int,
      pageSize: Int,
  ): ReceivedReviewPage {
    if (page == 1) {
      firstPageCalls += Triple(courtId, page, pageSize)
      firstPageFailures.removeFirstOrNull()?.let { throw it }
      return firstPageResponse ?: error("No first-page response configured.")
    }
    nextPageCalls += Triple(courtId, page, pageSize)
    nextPageFailure?.let { throw it }
    return nextPageResponse ?: error("No next-page response configured.")
  }
}

private class FakeComplexRepository(
    private val hub: MyComplexHub = MyComplexHub(complexes = emptyList()),
    private val failure: Throwable? = null,
) : IComplexRepository {
  override suspend fun getMyComplexHub(): MyComplexHub {
    failure?.let { throw it }
    return hub
  }

  override suspend fun getProvinces() = error("Unused in this test")

  override suspend fun getCantons(provinceId: String) = error("Unused in this test")

  override suspend fun getServices(
      scope: io.github.themonstersp4.mejengueros.domain.model.ServiceScope
  ) = error("Unused in this test")

  override suspend fun createComplex(
      request: io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
  ) = error("Unused in this test")

  override suspend fun addCourt(
      complexId: String,
      request: io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest,
  ) = error("Unused in this test")

  override suspend fun updateCourtImage(
      complexId: String,
      courtId: String,
      imageUploadId: String,
  ) = error("Unused in this test")
}

private class FakeErrorReporter : ErrorReporter {
  val events = mutableListOf<ReportedFailure>()

  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    events += ReportedFailure(name = name, attributes = attributes)
  }
}

private data class ReportedFailure(val name: String, val attributes: Map<String, String>)

private object OwnerReceivedReviewsViewModelTestHelper {
  fun defaultPage(): ReceivedReviewPage =
      ReceivedReviewPage(
          items =
              listOf(
                  ReceivedReview(
                      reviewId = "review-1",
                      rating = 5,
                      comment = "Excelente",
                      createdAt = "2026-07-01T20:00:00.000Z",
                      court =
                          ReceivedReviewCourt(
                              id = "court-a",
                              name = "A Court",
                          ),
                      reviewer =
                          ReceivedReviewer(
                              displayName = "Mejenguero 1",
                              initials = "M1",
                          ),
                  ),
                  ReceivedReview(
                      reviewId = "review-2",
                      rating = 4,
                      comment = "Muy buena",
                      createdAt = "2026-07-01T20:00:00.000Z",
                      court =
                          ReceivedReviewCourt(
                              id = "court-a",
                              name = "A Court",
                          ),
                      reviewer =
                          ReceivedReviewer(
                              displayName = "Mejenguero 2",
                              initials = "M2",
                          ),
                  ),
                  ReceivedReview(
                      reviewId = "review-3",
                      rating = 3,
                      comment = "Aceptable",
                      createdAt = "2026-07-01T20:00:00.000Z",
                      court =
                          ReceivedReviewCourt(
                              id = "court-a",
                              name = "A Court",
                          ),
                      reviewer =
                          ReceivedReviewer(
                              displayName = "Mejenguero 3",
                              initials = "M3",
                          ),
                  ),
              ),
          summary =
              ReceivedReviewsSummary(
                  selectedCourtId = "court-a",
                  totalReviews = 3,
                  averageRating = 4.0,
              ),
          page = 1,
          pageSize = 10,
          totalItems = 3,
          totalPages = 1,
          hasNextPage = false,
      )
}
