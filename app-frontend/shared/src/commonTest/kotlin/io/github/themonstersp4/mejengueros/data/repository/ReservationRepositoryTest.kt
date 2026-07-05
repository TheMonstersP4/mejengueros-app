package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IReservationRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.MyReservationCard
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import kotlin.test.Test
import kotlin.test.assertEquals

class ReservationRepositoryTest {
  @Test
  fun getMyReservationsForwardsRemoteResponse() =
      kotlinx.coroutines.test.runTest {
        val expected =
            MyReservations(
                upcoming =
                    listOf(
                        MyReservationCard(
                            id = "upcoming-id",
                            complexName = "Moravia FC",
                            courtName = "Cancha 1",
                            startsAt = "2026-07-10T18:00:00.000Z",
                            endsAt = "2026-07-10T19:00:00.000Z",
                            status = "CONFIRMED",
                            section = "UPCOMING",
                            reviewStatus = "NOT_APPLICABLE",
                            canReview = false,
                            hasReview = false,
                        )
                    ),
                finalized =
                    listOf(
                        MyReservationCard(
                            id = "finalized-id",
                            complexName = "Moravia FC",
                            courtName = "Cancha 2",
                            startsAt = "2026-07-08T18:00:00.000Z",
                            endsAt = "2026-07-08T19:00:00.000Z",
                            status = "COMPLETED",
                            section = "FINALIZED",
                            reviewStatus = "PENDING_REVIEW",
                            canReview = true,
                            hasReview = false,
                            primaryActionKey = "leave_review",
                            primaryActionLabel = "Dejar reseña",
                        )
                    ),
            )
        val repository =
            ReservationRepository(FakeReservationRemoteDataSource(myReservations = expected))

        val result = repository.getMyReservations()

        assertEquals(expected, result)
      }
}

private class FakeReservationRemoteDataSource(
    private val myReservations: MyReservations = MyReservations(emptyList(), emptyList()),
) : IReservationRemoteDataSource {
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

  override suspend fun getMyReservations(): MyReservations = myReservations
}
