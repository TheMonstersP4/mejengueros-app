package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddCourtScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun screenExposesOptionalImageActions() {
    var pickRequests = 0
    var clearRequests = 0

    composeRule.setContent {
      MejenguerosTheme {
        AddCourtScreen(
            state =
                AddCourtUiState(
                    complexName = "North Sports Center",
                    isLoadingServices = false,
                    isCourtImagePickerAvailable = true,
                    selectedCourtImage =
                        LocalCourtImage(
                            fileName = "court.png",
                            contentType = "image/png",
                            bytes = byteArrayOf(1, 2, 3),
                            previewUrl = "content://court.png",
                        ),
                ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            actions =
                AddCourtScreenActions(
                    onRetryServices = {},
                    onCourtNameChange = {},
                    onToggleService = {},
                    onPickCourtImage = { pickRequests += 1 },
                    onClearCourtImage = { clearRequests += 1 },
                    onSubmit = {},
                ),
        )
      }
    }

    composeRule.onNodeWithTag("add_court_pick_image_button").performScrollTo().performTouchInput {
      click()
    }
    composeRule.onNodeWithTag("add_court_clear_image_button").performScrollTo().performTouchInput {
      click()
    }

    composeRule.runOnIdle {
      assertEquals(1, pickRequests)
      assertEquals(1, clearRequests)
    }
  }

  @Test
  fun screenShowsInitialSelectImageCtaWhenPickerAvailableWithoutSelectedImage() {
    var pickRequests = 0

    composeRule.setContent {
      MejenguerosTheme {
        AddCourtScreen(
            state =
                AddCourtUiState(
                    complexName = "North Sports Center",
                    isLoadingServices = false,
                    isCourtImagePickerAvailable = true,
                ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            actions =
                AddCourtScreenActions(
                    onRetryServices = {},
                    onCourtNameChange = {},
                    onToggleService = {},
                    onPickCourtImage = { pickRequests += 1 },
                    onClearCourtImage = {},
                    onSubmit = {},
                ),
        )
      }
    }

    composeRule.onAllNodesWithTag("add_court_image_preview").assertCountEquals(0)
    composeRule.onAllNodesWithTag("add_court_clear_image_button").assertCountEquals(0)
    composeRule
        .onNodeWithTag("add_court_pick_image_button")
        .assertTextEquals("Seleccionar imagen")
        .assertIsEnabled()
        .performScrollTo()
        .performTouchInput { click() }

    composeRule.runOnIdle { assertEquals(1, pickRequests) }
  }
}
