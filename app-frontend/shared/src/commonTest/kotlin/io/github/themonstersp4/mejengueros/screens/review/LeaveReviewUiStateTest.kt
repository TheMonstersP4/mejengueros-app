package io.github.themonstersp4.mejengueros.screens.review

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeaveReviewUiStateTest {
  @Test
  fun oneStarBlankCommentCannotSubmit() {
    assertFalse(sampleState(selectedRating = MinReviewRating).canSubmit)
  }

  @Test
  fun oneStarWhitespaceOnlyCommentCannotSubmit() {
    assertFalse(sampleState(selectedRating = MinReviewRating, comment = "   \n\t  ").canSubmit)
  }

  @Test
  fun oneStarCommentCanSubmitWhenNotBlank() {
    assertTrue(
        sampleState(
                selectedRating = MinReviewRating,
                comment = "La iluminación falló y la cancha estaba descuidada",
            )
            .canSubmit
    )
  }

  @Test
  fun higherRatingsDoNotRequireComment() {
    assertTrue(sampleState(selectedRating = 4).canSubmit)
  }

  private fun sampleState(
      selectedRating: Int = 0,
      comment: String = "",
      mode: LeaveReviewUiMode = LeaveReviewUiMode.Form,
  ) =
      LeaveReviewUiState(
          reservationContext =
              LeaveReviewReservationContext(
                  title = "Moravia FC · Cancha A",
                  reservationLabel = "Reserva de ayer · 20:00 – 21:00",
              ),
          selectedRating = selectedRating,
          comment = comment,
          mode = mode,
      )
}
