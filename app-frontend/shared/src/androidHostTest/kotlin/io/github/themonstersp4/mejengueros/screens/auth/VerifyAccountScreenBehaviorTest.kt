package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VerifyAccountScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun verifyAccountScreenKeepsAuthHeadingAndSupportingCopyVisible() {
    composeRule.setVerifyAccountScreenContent()

    composeRule.screenText("Verificar cuenta").assertExists()
    composeRule
        .screenText(
            "Ingresa el código de seis dígitos enviado a tu correo cuando el flujo esté disponible."
        )
        .assertExists()
  }

  @Test
  fun verifyActionsShowTheirPendingMessages() {
    composeRule.setVerifyAccountScreenContent()

    composeRule.actionButton("Verificación pendiente").performScrollTo().performClick()
    composeRule.screenText("La verificación de cuenta aún no está conectada.").assertExists()

    composeRule.actionButton("Reenviar código no disponible aún").performScrollTo().performClick()
    composeRule.screenText("El reenvío de código todavía no está disponible.").assertExists()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule
      .setVerifyAccountScreenContent() {
    setContent {
      MejenguerosTheme {
        VerifyAccountScreen(
            onBackToRegister = {},
            onBackToLogin = {},
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
