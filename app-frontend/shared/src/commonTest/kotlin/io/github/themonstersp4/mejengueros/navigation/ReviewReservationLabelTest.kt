package io.github.themonstersp4.mejengueros.navigation

import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val COSTA_RICA_UTC_ROLLOVER_REVIEW_START = "2026-07-05T02:24:00.000Z"
private const val COSTA_RICA_UTC_ROLLOVER_REVIEW_END = "2026-07-05T03:24:00.000Z"

class ReviewReservationLabelTest {
  @Test
  fun reviewReservationLabelUsesCostaRicaCivilDateInsteadOfRawUtcDateSlice() {
    val context =
        ReviewableReservation(
                reservationId = "reservation-id",
                complexName = "Moravia FC",
                courtName = "Cancha A",
                startsAt = COSTA_RICA_UTC_ROLLOVER_REVIEW_START,
                endsAt = COSTA_RICA_UTC_ROLLOVER_REVIEW_END,
                imageUrl = null,
            )
            .toLeaveReviewReservationContext()

    assertEquals(
        "Reserva del 2026-07-04 · 20:24 – 21:24",
        context.reservationLabel,
    )
    assertFalse(context.reservationLabel.contains("2026-07-05"))
  }
}
