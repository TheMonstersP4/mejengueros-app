package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VerifyAccountScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun verifyAccountScreenKeepsAuthHeadingAndSupportingCopyVisible() {
    composeRule.setVerifyAccountScreenContent()

    composeRule.firstScreenText("Verificar cuenta").assertExists()
    composeRule.screenText("Ingresa el código de seis dígitos enviado a tu correo.").assertExists()
  }

  @Test
  fun verifyActionSubmitsSixDigitCode() {
    var submittedCode: String? = null
    composeRule.setVerifyAccountScreenContent(onConfirmRegistration = { submittedCode = it })

    composeRule.inputField("Código de verificación").performTextInput("123456")
    composeRule.actionButton("Verificar cuenta").performScrollTo().performClick()

    composeRule.runOnIdle { assertEquals("123456", submittedCode) }
  }

  @Test
  fun resendActionInvokesCallback() {
    var resendClicks = 0
    composeRule.setVerifyAccountScreenContent(onResendRegistrationCode = { resendClicks++ })

    composeRule.actionButton("Reenviar código").performScrollTo().performClick()

    composeRule.runOnIdle { assertEquals(1, resendClicks) }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setVerifyAccountScreenContent(
      state: AuthUiState = AuthUiState(emailInput = "player@example.com"),
      onConfirmRegistration: (String) -> Unit = {},
      onResendRegistrationCode: () -> Unit = {},
  ) {
    setContent {
      MejenguerosTheme {
        VerifyAccountScreen(
            state = state,
            onBackToRegister = {},
            onBackToLogin = {},
            onConfirmRegistration = onConfirmRegistration,
            onResendRegistrationCode = onResendRegistrationCode,
        )
      }
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.actionButton(text: String) =
      onNode(hasText(text) and buttonRoleMatcher())

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.screenText(text: String) =
      onNode(hasText(text))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.firstScreenText(text: String) =
      onAllNodes(hasText(text)).onFirst()

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.inputField(label: String) =
      onNode(hasSetTextAction() and hasContentDescription(label))

  private fun buttonRoleMatcher() =
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
}
