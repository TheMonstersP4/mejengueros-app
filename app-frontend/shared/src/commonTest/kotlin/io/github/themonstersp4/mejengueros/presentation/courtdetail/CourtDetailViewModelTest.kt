package io.github.themonstersp4.mejengueros.presentation.courtdetail

import io.github.themonstersp4.mejengueros.domain.model.CourtReview
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtFavoriteRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtReviewsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FailingCourtDetailRepository(),
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
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
  fun retryLoadReflectsUpdatedAvailabilityAfterASlotIsBooked() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          // Simulates the just-booked slot (16:00) disappearing from availability on refetch,
          // which is what the retained detail preview must reflect after a reservation.
          val repository =
              SequentialCourtDetailRepository(
                  availabilityPreviews =
                      listOf(
                          ReservationDayAvailability(
                              dateUtc = "2026-07-01",
                              availabilityStatus = ReservationAvailabilityStatus.Available,
                              slots = defaultSlots,
                          ),
                          ReservationDayAvailability(
                              dateUtc = "2026-07-01",
                              availabilityStatus = ReservationAvailabilityStatus.Available,
                              slots = defaultSlots.dropLast(1),
                          ),
                      ),
              )
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()
          assertEquals(
              listOf("08:00", "09:00", "10:00"),
              viewModel.uiState.value.slots.map { it.displayTime },
          )

          viewModel.retryLoad()
          advanceUntilIdle()

          assertEquals(
              listOf("08:00", "09:00"),
              viewModel.uiState.value.slots.map { it.displayTime },
          )
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
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
                  userId = "user-1",
                  courtId = "court-1",
                  repository = repository,
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertEquals("Hoy · slots de 1 hora", viewModel.uiState.value.availabilityHeadline)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initLoadsPublishedReviewsAndExposesThem() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(reviews = defaultReviews),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          assertTrue(viewModel.uiState.value.isLoadingReviews)

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoadingReviews)
          assertNull(viewModel.uiState.value.reviewsErrorMessage)
          assertEquals(
              listOf("Diego R.", "María S."),
              viewModel.uiState.value.reviews.map { it.authorName },
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initWithNoReviewsExposesEmptyReviewsWithoutError() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(reviews = emptyList()),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoadingReviews)
          assertNull(viewModel.uiState.value.reviewsErrorMessage)
          assertTrue(viewModel.uiState.value.reviews.isEmpty())
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun reviewsFailureExposesErrorWithoutBreakingSlots() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FailingCourtReviewsRepository(),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()

          assertNotNull(viewModel.uiState.value.reviewsErrorMessage)
          assertTrue(viewModel.uiState.value.reviews.isEmpty())
          // A reviews failure must not blank out the availability slots.
          assertEquals(3, viewModel.uiState.value.slots.size)
          assertNull(viewModel.uiState.value.slotsErrorMessage)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun retryLoadReviewsClearsErrorAndRestoresReviewsAfterTransientFailure() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FlakyCourtReviewsRepository(reviews = defaultReviews),
                  favoriteRepository = FakeCourtFavoriteRepository(),
                  coroutineScope = this,
              )

          advanceUntilIdle()
          assertNotNull(viewModel.uiState.value.reviewsErrorMessage)

          viewModel.retryLoadReviews()
          advanceUntilIdle()

          assertNull(viewModel.uiState.value.reviewsErrorMessage)
          assertEquals(2, viewModel.uiState.value.reviews.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initLoadsConfirmedFavoriteForTheAuthenticatedUserAndCourt() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val favoriteRepository = RecordingCourtFavoriteRepository(initialFavorite = true)
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(reviews = defaultReviews),
                  favoriteRepository = favoriteRepository,
                  coroutineScope = this,
              )

          assertEquals(CourtFavoriteStatus.Unknown, viewModel.uiState.value.favoriteStatus)
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Confirmed, viewModel.uiState.value.favoriteStatus)
          assertEquals(true, viewModel.uiState.value.isFavorite)
          assertEquals(listOf("user-1" to "court-1"), favoriteRepository.reads)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun toggleFavoriteIsPessimisticAndBlocksConcurrentTaps() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val writeGate = CompletableDeferred<Unit>()
          val favoriteRepository =
              RecordingCourtFavoriteRepository(
                  initialFavorite = false,
                  writeGate = writeGate,
              )
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = favoriteRepository,
                  coroutineScope = this,
              )
          advanceUntilIdle()

          viewModel.toggleFavorite()
          viewModel.toggleFavorite()
          runCurrent()

          assertEquals(CourtFavoriteStatus.Updating, viewModel.uiState.value.favoriteStatus)
          assertEquals(false, viewModel.uiState.value.isFavorite)
          assertEquals(listOf(Triple("user-1", "court-1", true)), favoriteRepository.writes)

          writeGate.complete(Unit)
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Confirmed, viewModel.uiState.value.favoriteStatus)
          assertEquals(true, viewModel.uiState.value.isFavorite)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun favoriteMutationFailureKeepsConfirmedStateWithoutDisruptingOtherContent() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(reviews = defaultReviews),
                  favoriteRepository = FailingWriteCourtFavoriteRepository(),
                  coroutineScope = this,
              )
          advanceUntilIdle()

          viewModel.toggleFavorite()
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Error, viewModel.uiState.value.favoriteStatus)
          assertEquals(false, viewModel.uiState.value.isFavorite)
          assertNotNull(viewModel.uiState.value.favoriteErrorMessage)
          assertEquals(3, viewModel.uiState.value.slots.size)
          assertEquals(2, viewModel.uiState.value.reviews.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun favoriteMutationCanBeRetriedAfterARecoverableFailure() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val favoriteRepository = FlakyWriteCourtFavoriteRepository()
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = favoriteRepository,
                  coroutineScope = this,
              )
          advanceUntilIdle()

          viewModel.toggleFavorite()
          advanceUntilIdle()
          assertEquals(CourtFavoriteStatus.Error, viewModel.uiState.value.favoriteStatus)
          assertEquals(false, viewModel.uiState.value.isFavorite)

          viewModel.toggleFavorite()
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Confirmed, viewModel.uiState.value.favoriteStatus)
          assertEquals(true, viewModel.uiState.value.isFavorite)
          assertEquals(2, favoriteRepository.writeAttempts)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun retryFavoriteLoadRecoversFromInitialReadFailure() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel =
              CourtDetailViewModel(
                  userId = "user-1",
                  courtId = "court-1",
                  repository = FakeCourtDetailRepository(availabilityPreview = defaultAvailability),
                  reviewsRepository = FakeCourtReviewsRepository(),
                  favoriteRepository = FlakyReadCourtFavoriteRepository(),
                  coroutineScope = this,
              )
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Error, viewModel.uiState.value.favoriteStatus)
          assertNull(viewModel.uiState.value.isFavorite)

          viewModel.retryLoadFavorite()
          advanceUntilIdle()

          assertEquals(CourtFavoriteStatus.Confirmed, viewModel.uiState.value.favoriteStatus)
          assertEquals(true, viewModel.uiState.value.isFavorite)
        } finally {
          Dispatchers.resetMain()
        }
      }
}

private val defaultSlots =
    listOf(
        ReservableSlot(
            startsAtUtc = "2026-07-01T14:00:00.000Z",
            endsAtUtc = "2026-07-01T15:00:00.000Z",
        ),
        ReservableSlot(
            startsAtUtc = "2026-07-01T15:00:00.000Z",
            endsAtUtc = "2026-07-01T16:00:00.000Z",
        ),
        ReservableSlot(
            startsAtUtc = "2026-07-01T16:00:00.000Z",
            endsAtUtc = "2026-07-01T17:00:00.000Z",
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

private class SequentialCourtDetailRepository(
    private val availabilityPreviews: List<ReservationDayAvailability?>,
) : ICourtDetailRepository {
  private var invocations = 0

  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability? {
    val index = invocations.coerceAtMost(availabilityPreviews.lastIndex)
    invocations += 1
    return availabilityPreviews[index]
  }
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

private val defaultReviews =
    listOf(
        CourtReview(
            id = "review-a",
            rating = 5,
            comment = "Cancha impecable, volvería.",
            authorName = "Diego R.",
            authorInitials = "DR",
            dateLabel = "2 de julio de 2026",
        ),
        CourtReview(
            id = "review-b",
            rating = 4,
            comment = null,
            authorName = "María S.",
            authorInitials = "MS",
            dateLabel = "1 de julio de 2026",
        ),
    )

private class FakeCourtReviewsRepository(
    private val reviews: List<CourtReview> = emptyList(),
) : ICourtReviewsRepository {
  override suspend fun getCourtReviews(courtId: String): List<CourtReview> = reviews
}

private class FakeCourtFavoriteRepository(
    private var favorite: Boolean = false,
) : ICourtFavoriteRepository {
  override suspend fun isFavorite(userId: String, courtId: String): Boolean = favorite

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    favorite = isFavorite
  }
}

private class RecordingCourtFavoriteRepository(
    private var initialFavorite: Boolean,
    private val writeGate: CompletableDeferred<Unit>? = null,
) : ICourtFavoriteRepository {
  val reads = mutableListOf<Pair<String, String>>()
  val writes = mutableListOf<Triple<String, String, Boolean>>()

  override suspend fun isFavorite(userId: String, courtId: String): Boolean {
    reads += userId to courtId
    return initialFavorite
  }

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    writes += Triple(userId, courtId, isFavorite)
    writeGate?.await()
    initialFavorite = isFavorite
  }
}

private class FailingWriteCourtFavoriteRepository : ICourtFavoriteRepository {
  override suspend fun isFavorite(userId: String, courtId: String): Boolean = false

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    error("local write failed")
  }
}

private class FlakyWriteCourtFavoriteRepository : ICourtFavoriteRepository {
  var writeAttempts = 0
    private set

  override suspend fun isFavorite(userId: String, courtId: String): Boolean = false

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    writeAttempts += 1
    if (writeAttempts == 1) error("transient local write failure")
  }
}

private class FlakyReadCourtFavoriteRepository : ICourtFavoriteRepository {
  private var attempts = 0

  override suspend fun isFavorite(userId: String, courtId: String): Boolean {
    attempts += 1
    if (attempts == 1) error("local read failed")
    return true
  }

  override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) = Unit
}

private class FailingCourtReviewsRepository : ICourtReviewsRepository {
  override suspend fun getCourtReviews(courtId: String): List<CourtReview> {
    throw IllegalStateException("network error")
  }
}

private class FlakyCourtReviewsRepository(
    private val reviews: List<CourtReview>,
) : ICourtReviewsRepository {
  private var attempts = 0

  override suspend fun getCourtReviews(courtId: String): List<CourtReview> {
    attempts += 1
    if (attempts == 1) throw IllegalStateException("transient failure")
    return reviews
  }
}
