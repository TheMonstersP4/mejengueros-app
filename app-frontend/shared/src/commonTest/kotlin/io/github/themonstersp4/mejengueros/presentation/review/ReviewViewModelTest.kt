package io.github.themonstersp4.mejengueros.presentation.review

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
  @Test
  fun submitBlocksOneStarUntilCommentAndImageArePresent() = runTest {
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = FakeReviewRepository(),
            reviewEvidenceUploadRepository = FakeReviewEvidenceUploadRepository(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("La iluminación falló toda la hora.")

    assertFalse(viewModel.uiState.value.canSubmit)

    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    assertTrue(viewModel.uiState.value.canSubmit)
  }

  @Test
  fun submitUploadsEvidenceAndCreatesReview() = runTest {
    val reviewRepository = FakeReviewRepository()
    val evidenceRepository = FakeReviewEvidenceUploadRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = evidenceRepository,
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("La iluminación falló toda la hora.")
    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    viewModel.submit()
    advanceUntilIdle()

    assertEquals("reservation-id", reviewRepository.lastCreateRequest?.reservationId)
    assertEquals("evidence-image-id", reviewRepository.lastCreateRequest?.evidenceImageUploadId)
    assertEquals("review-id", viewModel.uiState.value.submittedReview?.id)
  }

  @Test
  fun submitRetryReusesConfirmedEvidenceUploadInsteadOfUploadingAgain() = runTest {
    val reviewRepository = FailOnceReviewRepository()
    val evidenceRepository = CountingReviewEvidenceUploadRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = evidenceRepository,
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("La iluminación falló toda la hora.")
    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "No pudimos enviar la reseña. Intentá de nuevo.",
        viewModel.uiState.value.submitErrorMessage,
    )
    assertEquals(1, evidenceRepository.uploadCalls)

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(1, evidenceRepository.uploadCalls)
    assertEquals("evidence-image-id", reviewRepository.lastCreateRequest?.evidenceImageUploadId)
    assertEquals("review-id", viewModel.uiState.value.submittedReview?.id)
  }

  @Test
  fun resetFlowIgnoresLateCreateSuccessFromCancelledSubmission() = runTest {
    val createResult = CompletableDeferred<Result<CreatedReview>>()
    val reviewRepository =
        RefreshingSuspendedCreateReviewRepository(
            createResult,
            latestReservations = listOf(sampleReservation(), null),
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = FakeReviewEvidenceUploadRepository(),
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(5)
    viewModel.updateComment("Borrador que no debe volver")

    viewModel.submit()
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isSubmitting)

    viewModel.resetFlow()
    advanceUntilIdle()

    createResult.complete(Result.success(sampleCreatedReview()))
    advanceUntilIdle()

    assertEquals(2, reviewRepository.latestReservationCalls)
    assertEquals(0, viewModel.uiState.value.selectedRating)
    assertEquals("", viewModel.uiState.value.comment)
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertNull(viewModel.uiState.value.reviewableReservation)
    assertNull(viewModel.uiState.value.submittedReview)
    assertNull(viewModel.uiState.value.submitErrorMessage)
  }

  @Test
  fun resetFlowIgnoresLateEvidenceUploadFailureFromCancelledSubmission() = runTest {
    val uploadResult = CompletableDeferred<Result<ConfirmedReviewEvidenceImageUpload>>()
    val reviewRepository =
        RefreshingReviewRepository(latestReservations = listOf(sampleReservation(), null))
    val evidenceRepository = SuspendedReviewEvidenceUploadRepository(uploadResult)
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = evidenceRepository,
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("Borrador de una estrella")
    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    viewModel.submit()
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isSubmitting)

    viewModel.resetFlow()
    advanceUntilIdle()

    uploadResult.complete(Result.failure(AppApiException(statusCode = 415, message = "Bad image")))
    advanceUntilIdle()

    assertEquals(2, reviewRepository.latestReservationCalls)
    assertEquals(0, viewModel.uiState.value.selectedRating)
    assertEquals("", viewModel.uiState.value.comment)
    assertNull(viewModel.uiState.value.selectedEvidenceImage)
    assertFalse(viewModel.uiState.value.isSubmitting)
    assertNull(viewModel.uiState.value.reviewableReservation)
    assertNull(viewModel.uiState.value.submitErrorMessage)
  }

  @Test
  fun latestEligibleReservationCanBeAbsent() = runTest {
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = NullLatestReservationReviewRepository(),
            reviewEvidenceUploadRepository = FakeReviewEvidenceUploadRepository(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.loadErrorMessage)
    assertNull(viewModel.uiState.value.reviewableReservation)
  }

  @Test
  fun loadFailureCanRetryAndRecoverReservation() = runTest {
    val reviewRepository = FailLatestReservationOnceReviewRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = FakeReviewEvidenceUploadRepository(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertEquals(
        "No pudimos cargar la reserva pendiente de reseña.",
        viewModel.uiState.value.loadErrorMessage,
    )
    assertNull(viewModel.uiState.value.reviewableReservation)

    viewModel.refreshLatestReviewableReservation()
    assertTrue(viewModel.uiState.value.isLoading)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.loadErrorMessage)
    assertEquals("reservation-id", viewModel.uiState.value.reviewableReservation?.reservationId)
    assertEquals(2, reviewRepository.latestReservationCalls)
  }

  @Test
  fun submitMapsUploadFailureToEvidenceMessage() = runTest {
    val evidenceRepository =
        FailingReviewEvidenceUploadRepository(
            AppApiException(statusCode = 415, message = "Bad image")
        )
    val reviewRepository = RecordingReviewRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = evidenceRepository,
            coroutineScope = scope,
        )

    advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("La iluminación falló toda la hora.")
    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "Revisá la imagen de evidencia e intentá de nuevo.",
        viewModel.uiState.value.submitErrorMessage,
    )
    assertEquals(0, reviewRepository.createCalls)
  }

  @Test
  fun submitMapsUpload401And403StatusesToEvidenceSessionMessage() = runTest {
    assertUploadFailureMessage(
        failure = AppApiException(statusCode = 401, message = "Unauthorized"),
        expectedMessage = "Necesitás volver a iniciar sesión para subir la imagen de evidencia.",
    )
    assertUploadFailureMessage(
        failure = AppApiException(statusCode = 403, message = "Forbidden"),
        expectedMessage = "Necesitás volver a iniciar sesión para subir la imagen de evidencia.",
    )
  }

  @Test
  fun submitMapsUnexpectedUploadFailuresToGenericEvidenceMessage() = runTest {
    assertUploadFailureMessage(
        failure = AppApiException(statusCode = 500, message = "Upload failed"),
        expectedMessage =
            "No pudimos subir la imagen de evidencia. Revisá el archivo e intentá de nuevo.",
    )
    assertUploadFailureMessage(
        failure = IllegalStateException("unexpected upload failure"),
        expectedMessage =
            "No pudimos subir la imagen de evidencia. Revisá el archivo e intentá de nuevo.",
    )
  }

  @Test
  fun submitMaps400StatusToValidationMessage() = runTest {
    assertSubmitErrorMessage(
        statusCode = 400,
        expectedMessage =
            "Revisá la calificación, el comentario y la evidencia antes de enviar la reseña.",
    )
  }

  @Test
  fun submitMaps401And403StatusesToSessionMessage() = runTest {
    assertSubmitErrorMessage(
        statusCode = 401,
        expectedMessage = "Necesitás volver a iniciar sesión para enviar la reseña.",
    )
    assertSubmitErrorMessage(
        statusCode = 403,
        expectedMessage = "Necesitás volver a iniciar sesión para enviar la reseña.",
    )
  }

  @Test
  fun submitMaps404StatusToMissingReservationMessage() = runTest {
    assertSubmitErrorMessage(
        statusCode = 404,
        expectedMessage = "No encontramos una reserva válida para registrar esta reseña.",
    )
  }

  @Test
  fun submitMaps409StatusToConflictMessage() = runTest {
    assertSubmitErrorMessage(
        statusCode = 409,
        expectedMessage = "Esta reserva ya tenía una reseña registrada.",
    )
  }

  private class FakeReviewRepository : IReviewRepository {
    var lastCreateRequest: CreateReviewRequest? = null

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        ReviewableReservation(
            reservationId = "reservation-id",
            complexName = "Moravia FC",
            courtName = "Cancha A",
            startsAt = "2026-07-02T20:00:00.000Z",
            endsAt = "2026-07-02T21:00:00.000Z",
            imageUrl = "https://read.example.test/court-a.png",
        )

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
      lastCreateRequest = request
      return CreatedReview(
          id = "review-id",
          reservationId = request.reservationId,
          rating = request.rating,
          comment = request.comment,
          evidenceImageUploadId = request.evidenceImageUploadId,
          createdAt = "2026-07-03T02:00:00.000Z",
      )
    }

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class FakeReviewEvidenceUploadRepository : IReviewEvidenceUploadRepository {
    override suspend fun uploadReviewEvidence(
        image: LocalReviewEvidenceImage
    ): ConfirmedReviewEvidenceImageUpload =
        ConfirmedReviewEvidenceImageUpload(
            id = "evidence-image-id",
            objectKey = "uploads/review-evidence-image/player-sub/2026/07/evidence.png",
            readUrl = "https://read.example.test/evidence.png",
        )
  }

  private class CountingReviewEvidenceUploadRepository : IReviewEvidenceUploadRepository {
    var uploadCalls: Int = 0

    override suspend fun uploadReviewEvidence(
        image: LocalReviewEvidenceImage
    ): ConfirmedReviewEvidenceImageUpload {
      uploadCalls += 1
      return ConfirmedReviewEvidenceImageUpload(
          id = "evidence-image-id",
          objectKey = "uploads/review-evidence-image/player-sub/2026/07/evidence.png",
          readUrl = "https://read.example.test/evidence.png",
      )
    }
  }

  private class FailOnceReviewRepository : IReviewRepository {
    var createCalls: Int = 0
    var lastCreateRequest: CreateReviewRequest? = null

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        ReviewableReservation(
            reservationId = "reservation-id",
            complexName = "Moravia FC",
            courtName = "Cancha A",
            startsAt = "2026-07-02T20:00:00.000Z",
            endsAt = "2026-07-02T21:00:00.000Z",
            imageUrl = "https://read.example.test/court-a.png",
        )

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
      createCalls += 1
      lastCreateRequest = request

      if (createCalls == 1) {
        throw AppApiException(statusCode = 500, message = "Temporary failure")
      }

      return CreatedReview(
          id = "review-id",
          reservationId = request.reservationId,
          rating = request.rating,
          comment = request.comment,
          evidenceImageUploadId = request.evidenceImageUploadId,
          createdAt = "2026-07-03T02:00:00.000Z",
      )
    }

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class SuspendedCreateReviewRepository(
      private val createResult: CompletableDeferred<Result<CreatedReview>>
  ) : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        sampleReservation()

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        createResult.await().getOrThrow()

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class RefreshingSuspendedCreateReviewRepository(
      private val createResult: CompletableDeferred<Result<CreatedReview>>,
      latestReservations: List<ReviewableReservation?>,
  ) : IReviewRepository {
    private val latestReservations = latestReservations
    private val latestReservationIterator = latestReservations.iterator()
    var latestReservationCalls: Int = 0

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
      latestReservationCalls += 1
      return if (latestReservationIterator.hasNext()) {
        latestReservationIterator.next()
      } else {
        latestReservations.lastOrNull()
      }
    }

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        createResult.await().getOrThrow()

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class SuspendedReviewEvidenceUploadRepository(
      private val uploadResult: CompletableDeferred<Result<ConfirmedReviewEvidenceImageUpload>>
  ) : IReviewEvidenceUploadRepository {
    override suspend fun uploadReviewEvidence(
        image: LocalReviewEvidenceImage
    ): ConfirmedReviewEvidenceImageUpload = uploadResult.await().getOrThrow()
  }

  private class NullLatestReservationReviewRepository : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? = null

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create a review when no reservation is available.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class RefreshingReviewRepository(
      latestReservations: List<ReviewableReservation?>,
  ) : IReviewRepository {
    private val latestReservations = latestReservations
    private val latestReservationIterator = latestReservations.iterator()
    var latestReservationCalls: Int = 0

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
      latestReservationCalls += 1
      return if (latestReservationIterator.hasNext()) {
        latestReservationIterator.next()
      } else {
        latestReservations.lastOrNull()
      }
    }

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create a review in upload cancellation tests.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class FailLatestReservationOnceReviewRepository : IReviewRepository {
    var latestReservationCalls: Int = 0

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
      latestReservationCalls += 1
      if (latestReservationCalls == 1) {
        throw AppApiException(statusCode = 500, message = "Temporary load failure")
      }
      return sampleReservation()
    }

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create a review in load retry tests.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class RecordingReviewRepository : IReviewRepository {
    var createCalls: Int = 0

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        sampleReservation()

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
      createCalls += 1
      return sampleCreatedReview()
    }

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private class FailingReviewEvidenceUploadRepository(private val failure: Throwable) :
      IReviewEvidenceUploadRepository {
    override suspend fun uploadReviewEvidence(
        image: LocalReviewEvidenceImage
    ): ConfirmedReviewEvidenceImageUpload = throw failure
  }

  private suspend fun assertSubmitErrorMessage(statusCode: Int, expectedMessage: String) {
    val scope = TestScope(StandardTestDispatcher())
    val viewModel =
        ReviewViewModel(
            reviewRepository = FailingCreateReviewRepository(statusCode),
            reviewEvidenceUploadRepository = FakeReviewEvidenceUploadRepository(),
            coroutineScope = scope,
        )

    scope.advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(5)

    viewModel.submit()
    scope.advanceUntilIdle()

    assertEquals(expectedMessage, viewModel.uiState.value.submitErrorMessage)
  }

  private suspend fun assertUploadFailureMessage(failure: Throwable, expectedMessage: String) {
    val reviewRepository = RecordingReviewRepository()
    val scope = TestScope(StandardTestDispatcher())
    val viewModel =
        ReviewViewModel(
            reviewRepository = reviewRepository,
            reviewEvidenceUploadRepository = FailingReviewEvidenceUploadRepository(failure),
            coroutineScope = scope,
        )

    scope.advanceUntilIdle()
    viewModel.startReview()
    viewModel.updateRating(1)
    viewModel.updateComment("La iluminación falló toda la hora.")
    viewModel.updateSelectedEvidenceImage(sampleEvidenceImage())

    viewModel.submit()
    scope.advanceUntilIdle()

    assertEquals(expectedMessage, viewModel.uiState.value.submitErrorMessage)
    assertEquals(0, reviewRepository.createCalls)
  }

  private class FailingCreateReviewRepository(private val statusCode: Int) : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        sampleReservation()

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview {
      throw AppApiException(statusCode = statusCode, message = "HTTP $statusCode")
    }

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in leave-review tests.")
  }

  private fun sampleEvidenceImage() =
      LocalReviewEvidenceImage(
          fileName = "evidence.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://evidence.png",
      )
}

private fun sampleReservation() =
    ReviewableReservation(
        reservationId = "reservation-id",
        complexName = "Moravia FC",
        courtName = "Cancha A",
        startsAt = "2026-07-02T20:00:00.000Z",
        endsAt = "2026-07-02T21:00:00.000Z",
        imageUrl = "https://read.example.test/court-a.png",
    )

private fun sampleCreatedReview() =
    CreatedReview(
        id = "review-id",
        reservationId = "reservation-id",
        rating = 5,
        comment = "Comentario enviado",
        evidenceImageUploadId = null,
        createdAt = "2026-07-03T02:00:00.000Z",
    )
