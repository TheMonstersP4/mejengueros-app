package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RegisterScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun registerScreenKeepsAuthHeadingAndSupportingCopyVisible() {
    composeRule.setRegisterScreenContent(state = AuthUiState())

    composeRule.screenText("Crear cuenta").assertExists()
    composeRule
        .screenText("Este formulario prepara el flujo visual de registro de semana 10.")
        .assertExists()
    composeRule
        .screenText(
            "Al registrarte aceptarás los términos y políticas cuando el flujo productivo esté disponible."
        )
        .assertExists()
  }

  @Test
  fun manualRegistrationActionShowsPendingMessage() {
    composeRule.setRegisterScreenContent(state = AuthUiState())

    composeRule.actionButton("Registro manual pendiente").performScrollTo().performClick()

    composeRule.screenText("El registro manual aún no está conectado al backend.").assertExists()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setRegisterScreenContent(
      state: AuthUiState,
  ) {
    setContent {
      MejenguerosTheme {
        RegisterScreen(
            state = state,
            onBackToLogin = {},
            onOpenVerification = {},
        )
      }
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.actionButton(text: String) =
      onNode(hasText(text) and buttonRoleMatcher())

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.screenText(text: String) =
      onNode(hasText(text))

  private fun buttonRoleMatcher() =
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
}
