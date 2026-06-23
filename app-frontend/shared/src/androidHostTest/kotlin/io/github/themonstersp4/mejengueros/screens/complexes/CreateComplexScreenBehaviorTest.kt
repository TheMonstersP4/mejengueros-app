package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateComplexScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun submitButtonStaysDisabledUntilAllFieldsHaveContent() {
    composeRule.setCreateComplexScreenContent(state = CreateComplexUiState())

    composeRule.submitButton().assertIsNotEnabled()
    composeRule.complexNameField().performTextInput("North Sports Center")
    composeRule.addressField().performTextInput("123 Main Street")
    composeRule.submitButton().assertIsNotEnabled()
    composeRule.firstCourtField().performTextInput("Court A")

    composeRule.submitButton().assertIsEnabled()
  }

  @Test
  fun submitInvokesCallbackAndKeepsOwnerProvisioningMessageVisible() {
    var submits = 0
    composeRule.setCreateComplexScreenContent(
        state =
            CreateComplexUiState(
                complexName = "North Sports Center",
                complexAddress = "123 Main Street",
                firstCourtName = "Court A",
            ),
        onSubmit = { submits += 1 },
    )

    composeRule.submitButton().performScrollTo().assertIsEnabled().performTouchInput { click() }

    composeRule.runOnIdle { assertEquals(1, submits) }
    composeRule
        .supportingText(
            "Si tu usuario todavía no tiene el rol OWNER local, el sistema mostrará el bloqueo para que puedas pedir la provisión demo."
        )
        .assertExists()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setCreateComplexScreenContent(
      state: CreateComplexUiState,
      onComplexNameChange: (String) -> Unit = {},
      onComplexAddressChange: (String) -> Unit = {},
      onFirstCourtNameChange: (String) -> Unit = {},
      onSubmit: () -> Unit = {},
  ) {
    setContent {
      var localState by remember { mutableStateOf(state) }

      MejenguerosTheme {
        CreateComplexScreen(
            state = localState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            onComplexNameChange = {
              localState = localState.copy(complexName = it)
              onComplexNameChange(it)
            },
            onComplexAddressChange = {
              localState = localState.copy(complexAddress = it)
              onComplexAddressChange(it)
            },
            onFirstCourtNameChange = {
              localState = localState.copy(firstCourtName = it)
              onFirstCourtNameChange(it)
            },
            onSubmit = onSubmit,
        )
      }
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.complexNameField() =
      onNode(hasSetTextAction() and hasContentDescription("Nombre del complejo"))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.addressField() =
      onNode(hasSetTextAction() and hasContentDescription("Dirección"))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.firstCourtField() =
      onNode(hasSetTextAction() and hasContentDescription("Nombre de la primera cancha"))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.submitButton() =
      onNodeWithTag("create_complex_submit_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.supportingText(text: String) =
      onNodeWithText(text)
}
