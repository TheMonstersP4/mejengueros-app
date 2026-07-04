package io.github.themonstersp4.mejengueros.screens.review

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeaveReviewScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun initialStateRequiresRatingBeforeSubmit() {
    setReviewScreenContent()

    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertExists()
    composeRule.onNodeWithText("Seleccioná de 1 a 5 estrellas").assertExists()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .assertExists()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun selectingAndChangingRatingUpdatesHelperAndEnablesSubmit() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule.onNodeWithText("4 de 5 · Muy buena").assertExists()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsEnabled()

    composeRule.onNodeWithContentDescription("Seleccionar 2 de 5 estrellas").performClick()
    composeRule.onNodeWithText("2 de 5 · Regular").assertExists()
    composeRule.onNodeWithText("4 de 5 · Muy buena").assertDoesNotExist()
  }

  @Test
  fun oneStarRequiresCommentBeforeSubmit() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()

    composeRule.onNodeWithText("1 de 5 · Muy mala").assertExists()
    composeRule
        .onNodeWithText("Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia.")
        .assertExists()
    composeRule
        .onNodeWithText(
            "Si dejás 1 estrella, agregá al menos una imagen para respaldar tu reporte."
        )
        .assertExists()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun oneStarWithCommentAndImageCanSubmit() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule
        .commentField()
        .performTextInput("La iluminación falló y la cancha estaba muy descuidada")
    composeRule.onNodeWithTag("leave_review_pick_evidence_button").performScrollTo().performClick()

    composeRule.onNodeWithTag("leave_review_submit_button").assertIsEnabled()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()

    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()
  }

  @Test
  fun switchingFromOneStarToHigherRatingRemovesCommentRequirementWithoutClearingDraft() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule.commentField().performTextInput("Comentario que debe quedarse")
    composeRule.onNodeWithTag("leave_review_pick_evidence_button").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()

    composeRule
        .onNodeWithText("Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia.")
        .assertDoesNotExist()
    composeRule.onNodeWithText("Comentario que debe quedarse").assertExists()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsEnabled()
  }

  @Test
  fun oneStarWithCommentButWithoutImageStaysBlocked() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule
        .commentField()
        .performTextInput("La iluminación falló y la cancha estaba muy descuidada")

    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun unavailablePickerForOneStarShowsHonestExplanationAndDisabledAction() {
    composeRule.setContent {
      MejenguerosTheme {
        LeaveReviewScreen(
            state =
                sampleState(
                        selectedRating = 1,
                    )
                    .copy(
                        comment = "La iluminación falló y la cancha estaba muy descuidada",
                        isEvidenceImagePickerAvailable = false,
                    ),
            contentPadding = PaddingValues(),
            actions = noOpActions(),
        )
      }
    }

    composeRule
        .onNodeWithText(
            "En este dispositivo todavía no podés adjuntar imágenes de respaldo. Si necesitás dejar 1 estrella, intentá desde un dispositivo compatible."
        )
        .assertExists()
    composeRule.onNodeWithTag("leave_review_pick_evidence_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun selectedEvidenceImageShowsPreviewAndCanBeRemoved() {
    setReviewScreenContent()

    composeRule.onNodeWithTag("leave_review_pick_evidence_button").performScrollTo().performClick()

    composeRule.onNodeWithTag("leave_review_evidence_preview").assertExists()
    composeRule.onNodeWithText("evidence.png").assertExists()
    composeRule.onNodeWithTag("leave_review_clear_evidence_button").performScrollTo().performClick()
    composeRule.onNodeWithTag("leave_review_evidence_preview").assertDoesNotExist()
  }

  @Test
  fun whitespaceOnlyCommentKeepsOneStarSubmissionBlocked() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule.commentField().performTextInput("   \n\t  ")

    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun changingBackToOneStarReactivatesCommentRequirement() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsEnabled()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()

    composeRule
        .onNodeWithText("Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia.")
        .assertExists()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
  }

  @Test
  fun commentContainerTapFocusesFieldAndKeepsTypingBehaviorForOneStarFlow() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule.commentField().assertIsNotFocused()
    composeRule.onNodeWithTag("COMENTARIO text field container").performTouchInput {
      click(Offset(8f, height - 8f))
    }

    composeRule.commentField().assertIsFocused().performTextInput("La experiencia fue mala")
    composeRule.onNodeWithTag("leave_review_pick_evidence_button").performScrollTo().performClick()

    composeRule.onNodeWithTag("leave_review_submit_button").assertIsEnabled()
  }

  @Test
  fun submitTransitionsIntoSuccessContractAfterValidRating() {
    setReviewScreenContent()

    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()

    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()
    composeRule.onNodeWithText("VOLVER A MIS RESERVAS").assertExists()
    composeRule.onNodeWithText("EXPLORAR CANCHAS").assertExists()
  }

  @Test
  fun successScreenKeepsIndicatorCopyAndActionsVisible() {
    composeRule.setContent {
      MejenguerosTheme {
        LeaveReviewScreen(
            state = sampleState(mode = LeaveReviewUiMode.Success, selectedRating = 4),
            contentPadding = PaddingValues(),
            actions = noOpActions(),
        )
      }
    }

    composeRule.onNodeWithTag("leave_review_success_indicator").assertIsDisplayed()
    composeRule.onNodeWithTag("leave_review_success_icon").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Gracias por compartir tu experiencia. Vamos a usar tu reseña para mejorar la información de la cancha."
        )
        .assertExists()
    composeRule.onNodeWithText("VOLVER A MIS RESERVAS").assertIsDisplayed()
    composeRule.onNodeWithText("EXPLORAR CANCHAS").assertIsDisplayed()
  }

  @Test
  fun submittingStateShowsVisibleLoadingFeedback() {
    composeRule.setContent {
      MejenguerosTheme {
        LeaveReviewScreen(
            state = sampleState(selectedRating = 5).copy(isSubmitting = true),
            contentPadding = PaddingValues(),
            actions = noOpActions(),
        )
      }
    }

    composeRule.onNodeWithTag("mejengueros_loading_dialog").assertIsDisplayed()
    composeRule.onNodeWithText("Enviando reseña").assertExists()
  }

  private fun setReviewScreenContent() {
    composeRule.setContent { MejenguerosTheme { ReviewScreenTestHost() } }
  }

  @Composable
  private fun ReviewScreenTestHost() {
    var state by remember { mutableStateOf(sampleState()) }

    LeaveReviewScreen(
        state = state,
        contentPadding = PaddingValues(),
        actions =
            LeaveReviewScreenActions(
                onRatingSelected = { rating -> state = state.copy(selectedRating = rating) },
                onCommentChanged = { comment -> state = state.copy(comment = comment) },
                onPickEvidenceImage = {
                  state = state.copy(selectedEvidenceImage = sampleEvidenceImage())
                },
                onClearEvidenceImage = { state = state.copy(selectedEvidenceImage = null) },
                onSubmit = {
                  if (state.canSubmit) {
                    state = state.copy(mode = LeaveReviewUiMode.Success)
                  }
                },
                onReturnToReservations = {},
                onExploreCourts = {},
            ),
    )
  }

  private fun sampleState(
      mode: LeaveReviewUiMode = LeaveReviewUiMode.Form,
      selectedRating: Int = 0,
  ) =
      LeaveReviewUiState(
          reservationContext =
              LeaveReviewReservationContext(
                  title = "Moravia FC · Cancha A",
                  reservationLabel = "Reserva de ayer · 20:00 – 21:00",
                  imageContentDescription = "Cancha A",
              ),
          selectedRating = selectedRating,
          isEvidenceImagePickerAvailable = true,
          mode = mode,
      )

  private fun sampleEvidenceImage() =
      LocalReviewEvidenceImage(
          fileName = "evidence.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://evidence.png",
      )

  private fun noOpActions() =
      LeaveReviewScreenActions(
          onRatingSelected = {},
          onCommentChanged = {},
          onPickEvidenceImage = {},
          onClearEvidenceImage = {},
          onSubmit = {},
          onReturnToReservations = {},
          onExploreCourts = {},
      )

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.commentField() =
      onNode(hasSetTextAction() and hasContentDescription("COMENTARIO"))
}
