package io.github.themonstersp4.mejengueros.screens.review

import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeaveReviewUiStateTest {
  @Test
  fun oneStarBlankCommentCannotSubmit() {
    assertFalse(
        sampleState(
                selectedRating = MinReviewRating,
                selectedEvidenceImage = sampleEvidenceImage(),
            )
            .canSubmit
    )
  }

  @Test
  fun oneStarWhitespaceOnlyCommentCannotSubmit() {
    assertFalse(
        sampleState(
                selectedRating = MinReviewRating,
                comment = "   \n\t  ",
                selectedEvidenceImage = sampleEvidenceImage(),
            )
            .canSubmit
    )
  }

  @Test
  fun oneStarCommentCanSubmitWhenNotBlank() {
    assertTrue(
        sampleState(
                selectedRating = MinReviewRating,
                comment = "La iluminación falló y la cancha estaba descuidada",
                selectedEvidenceImage = sampleEvidenceImage(),
            )
            .canSubmit
    )
  }

  @Test
  fun oneStarWithoutEvidenceImageCannotSubmit() {
    assertFalse(
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

  @Test
  fun higherRatingsRequireQuestionnaireAnswers() {
    assertFalse(
        sampleState(selectedRating = 4, selectedQuestionnaireAnswers = emptyMap()).canSubmit
    )
  }

  private fun sampleState(
      selectedRating: Int = 0,
      comment: String = "",
      selectedQuestionnaireAnswers: Map<String, String> = sampleQuestionnaireAnswers(),
      selectedEvidenceImage: LocalReviewEvidenceImage? = null,
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
          selectedQuestionnaireAnswers = selectedQuestionnaireAnswers,
          selectedEvidenceImage = selectedEvidenceImage,
          mode = mode,
      )

  private fun sampleQuestionnaireAnswers() =
      mapOf(
          "FIELD_CONDITION" to "GOOD",
          "LIGHTING" to "REGULAR",
          "WOULD_RETURN" to "YES",
      )

  private fun sampleEvidenceImage() =
      LocalReviewEvidenceImage(
          fileName = "evidence.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://evidence.png",
      )
}
