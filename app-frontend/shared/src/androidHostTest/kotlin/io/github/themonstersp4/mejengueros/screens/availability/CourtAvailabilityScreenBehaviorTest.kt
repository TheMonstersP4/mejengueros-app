package io.github.themonstersp4.mejengueros.screens.availability

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourtAvailabilityScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun successShowsConfirmationDialogAndConfirmUsesAcknowledgedCallback() {
    var acknowledgements = 0

    composeRule.setContent {
      MejenguerosTheme {
        CourtAvailabilityScreen(
            state =
                CourtAvailabilityUiState(
                    courtName = "Cancha 1",
                    complexName = "Mejengas CR",
                    previewSlots = listOf("07:00"),
                    isLoading = false,
                    successMessage =
                        "Tu cancha ya tiene una disponibilidad base para recibir reservas.",
                ),
            contentPadding = PaddingValues(),
            actions =
                CourtAvailabilityScreenActions(
                    onToggleDay = {},
                    onStartTimeSelected = {},
                    onEndTimeSelected = {},
                    onRetry = {},
                    onSave = {},
                    onSuccessAcknowledged = { acknowledgements += 1 },
                ),
        )
      }
    }

    composeRule.onNodeWithText("Disponibilidad configurada").assertExists()
    composeRule
        .onNodeWithText("Tu cancha ya tiene una disponibilidad base para recibir reservas.")
        .assertExists()
    composeRule.onNodeWithText("Ir a Mi complejo").assertExists()
    composeRule.onNodeWithTag("mejengueros_confirmation_dialog").assertExists()
    composeRule.onNodeWithTag("mejengueros_confirmation_dialog_confirm").performClick()

    composeRule.runOnIdle { assertEquals(1, acknowledgements) }
  }

  @Test
  fun screenDoesNotShowConfirmationDialogWithoutSuccessMessage() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtAvailabilityScreen(
            state =
                CourtAvailabilityUiState(
                    courtName = "Cancha 1",
                    complexName = "Mejengas CR",
                    previewSlots = listOf("07:00"),
                    isLoading = false,
                ),
            contentPadding = PaddingValues(),
            actions =
                CourtAvailabilityScreenActions(
                    onToggleDay = {},
                    onStartTimeSelected = {},
                    onEndTimeSelected = {},
                    onRetry = {},
                    onSave = {},
                    onSuccessAcknowledged = {},
                ),
        )
      }
    }

    composeRule.onNodeWithTag("mejengueros_confirmation_dialog").assertDoesNotExist()
    composeRule.onNodeWithText("Disponibilidad configurada").assertDoesNotExist()
  }
}
