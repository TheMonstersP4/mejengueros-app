package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IReviewRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewCourt
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewer
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

class ReviewOwnerReceivedRepositoryTest {

  @Test
  fun getOwnerReceivedReviewsDelegatesToRemoteDataSource(): TestResult = runTest {
    val remote = RecordingRemoteDataSource()
    val repository = ReviewRepository(remote)

    val actual = repository.getOwnerReceivedReviews("court-1", page = 1, pageSize = 10)

    val expected = samplePage(items = 1, courtId = "court-1")
    assertEquals(expected, actual)
    val expectedCalls: List<Triple<String?, Int, Int>> = listOf(Triple("court-1", 1, 10))
    assertEquals(expectedCalls, remote.receivedReviewsCalls)
  }

  @Test
  fun getOwnerReceivedReviewsPropagatesRemoteFailures(): TestResult = runTest {
    val failure = IllegalStateException("upstream failure")
    val remote = FailingRemoteDataSource(failure)
    val repository = ReviewRepository(remote)

    val error =
        assertFailsWith<IllegalStateException> {
          repository.getOwnerReceivedReviews(null, page = 1, pageSize = 10)
        }
    assertEquals(failure, error)
  }

  private class RecordingRemoteDataSource : IReviewRemoteDataSource {
    val receivedReviewsCalls = mutableListOf<Triple<String?, Int, Int>>()

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? = null

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Not used in owner reviews repository tests.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage {
      receivedReviewsCalls += Triple(courtId, page, pageSize)
      return samplePage(items = receivedReviewsCalls.size, courtId = courtId)
    }
  }

  private class FailingRemoteDataSource(private val failure: Throwable) : IReviewRemoteDataSource {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? = null

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Not used in owner reviews repository tests.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = throw failure
  }
}

private fun samplePage(items: Int, courtId: String?): ReceivedReviewPage =
    ReceivedReviewPage(
        items =
            (1..items).map { index ->
              ReceivedReview(
                  reviewId = "review-$index",
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
                totalReviews = items,
                averageRating = 4.2,
            ),
        page = 1,
        pageSize = 10,
        totalItems = items,
        totalPages = 1,
        hasNextPage = false,
    )
