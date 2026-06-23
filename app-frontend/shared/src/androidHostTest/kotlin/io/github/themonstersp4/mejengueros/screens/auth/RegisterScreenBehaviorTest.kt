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
class RegisterScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun registerScreenKeepsAuthHeadingAndSupportingCopyVisible() {
    composeRule.setRegisterScreenContent(state = AuthUiState())

    composeRule.firstScreenText("Crear cuenta").assertExists()
    composeRule
        .screenText("Crea tu cuenta con correo y confirma el código que enviaremos.")
        .assertExists()
    composeRule
        .screenText(
            "Al registrarte aceptarás los términos y políticas cuando el flujo productivo esté disponible."
        )
        .assertExists()
    composeRule.screenText(PasswordPolicySupportingText).assertExists()
  }

  @Test
  fun mismatchedPasswordsShowsLocalError() {
    composeRule.setRegisterScreenContent(state = AuthUiState())

    composeRule.inputField("Correo electrónico").performTextInput("player@example.com")
    composeRule.inputField("Contraseña").performTextInput("secret123")
    composeRule.inputField("Confirmar contraseña").performTextInput("different123")
    composeRule.actionButton("Crear cuenta").performScrollTo().performClick()

    composeRule.screenText("Las contraseñas no coinciden.").assertExists()
  }

  @Test
  fun validRegistrationSubmitsEmailAndPassword() {
    var submittedEmail: String? = null
    var submittedPassword: String? = null

    composeRule.setRegisterScreenContent(
        state = AuthUiState(),
        onRegister = { email, password ->
          submittedEmail = email
          submittedPassword = password
        },
    )

    composeRule.inputField("Correo electrónico").performTextInput("player@example.com")
    composeRule.inputField("Contraseña").performTextInput("secret123")
    composeRule.inputField("Confirmar contraseña").performTextInput("secret123")
    composeRule.actionButton("Crear cuenta").performScrollTo().performClick()

    composeRule.runOnIdle {
      assertEquals("player@example.com", submittedEmail)
      assertEquals("secret123", submittedPassword)
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setRegisterScreenContent(
      state: AuthUiState,
      onRegister: (String, String) -> Unit = { _, _ -> },
  ) {
    setContent {
      MejenguerosTheme {
        RegisterScreen(
            state = state,
            onBackToLogin = {},
            onRegister = onRegister,
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
