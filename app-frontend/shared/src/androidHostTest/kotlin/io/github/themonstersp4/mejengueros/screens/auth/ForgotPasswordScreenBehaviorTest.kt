package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForgotPasswordScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun forgotPasswordRequiresEmailBeforeSendingCode() {
    composeRule.setForgotPasswordScreenContent()

    composeRule.screenText("Recuperar acceso").assertExists()
    composeRule.actionButton("Enviar código").assertIsNotEnabled()
  }

  @Test
  fun forgotPasswordSubmitsEnteredEmail() {
    var submittedEmail: String? = null

    composeRule.setForgotPasswordScreenContent(onSendCode = { submittedEmail = it })

    composeRule.inputField("Correo electrónico").performTextInput("david@example.com")
    composeRule.actionButton("Enviar código").assertIsEnabled().performClick()

    composeRule.runOnIdle { assertEquals("david@example.com", submittedEmail) }
  }

  @Test
  fun forgotPasswordShowsRetryWhenRequestFails() {
    composeRule.setForgotPasswordScreenContent(
        state =
            AuthUiState(
                emailInput = "david@example.com",
                errorMessage =
                    "No pudimos conectar con el servicio. Revisá tu conexión e intentá de nuevo.",
            )
    )

    composeRule
        .screenText("No pudimos conectar con el servicio. Revisá tu conexión e intentá de nuevo.")
        .assertExists()
    composeRule.actionButton("Reintentar").assertIsEnabled()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setForgotPasswordScreenContent(
      state: AuthUiState = AuthUiState(),
      onSendCode: (String) -> Unit = {},
  ) {
    setContent {
      MejenguerosTheme {
        ForgotPasswordScreen(
            state = state,
            onBackToLogin = {},
            onSendCode = onSendCode,
        )
      }
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.actionButton(text: String) =
      onNode(hasText(text) and buttonRoleMatcher())

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.screenText(text: String) =
      onNode(hasText(text))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.inputField(label: String) =
      onNode(hasSetTextAction() and hasContentDescription(label))

  private fun buttonRoleMatcher() =
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
}
