package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
class PasswordResetScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun passwordResetShowsCurrentPasswordPolicy() {
    composeRule.setContent {
      MejenguerosTheme {
        PasswordResetScreen(
            state = AuthUiState(emailInput = "david@example.com"),
            onBackToLogin = {},
            onConfirmPasswordReset = { _, _ -> },
        )
      }
    }

    composeRule.onNode(hasText(PasswordPolicySupportingText)).assertExists()
  }

  @Test
  fun passwordResetSubmitsCodeEnteredInOtpBoxes() {
    var submittedCode: String? = null
    var submittedPassword: String? = null

    composeRule.setPasswordResetScreenContent(
        onConfirmPasswordReset = { code, password ->
          submittedCode = code
          submittedPassword = password
        }
    )

    composeRule.inputField("Código de recuperación").performTextInput("123456")
    composeRule.inputField("Nueva contraseña").performTextInput("Password123!")
    composeRule.actionButton("Actualizar contraseña").performScrollTo().performClick()

    composeRule.runOnIdle {
      assertEquals("123456", submittedCode)
      assertEquals("Password123!", submittedPassword)
    }
  }

  @Test
  fun passwordResetSuccessRequiresExplicitReturnToLogin() {
    var backToLoginClicks = 0

    composeRule.setPasswordResetScreenContent(
        state =
            AuthUiState(
                emailInput = "david@example.com",
                successMessage = "Ya puedes iniciar sesión con tu nueva contraseña.",
            ),
        onBackToLogin = { backToLoginClicks++ },
    )

    composeRule.screenText("Contraseña actualizada").assertExists()
    composeRule.screenText("Ya puedes iniciar sesión con tu nueva contraseña.").assertExists()
    composeRule.actionButton("Ir a iniciar sesión").performClick()

    composeRule.runOnIdle { assertEquals(1, backToLoginClicks) }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setPasswordResetScreenContent(
      state: AuthUiState = AuthUiState(emailInput = "david@example.com"),
      onBackToLogin: () -> Unit = {},
      onConfirmPasswordReset: (String, String) -> Unit = { _, _ -> },
  ) {
    setContent {
      MejenguerosTheme {
        PasswordResetScreen(
            state = state,
            onBackToLogin = onBackToLogin,
            onConfirmPasswordReset = onConfirmPasswordReset,
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
