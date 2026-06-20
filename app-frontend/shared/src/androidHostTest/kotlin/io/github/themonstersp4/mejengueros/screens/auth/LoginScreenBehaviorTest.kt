package io.github.themonstersp4.mejengueros.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun blankCredentialsKeepEmailCtaDisabled() {
    composeRule.setLoginScreenContent(state = AuthUiState())

    composeRule.emailCtaButton().assertIsNotEnabled()
    composeRule.supportingText("Completá tu correo y contraseña para continuar.").assertExists()
  }

  @Test
  fun loginHeaderKeepsProductIdentityCopyVisible() {
    composeRule.setLoginScreenContent(state = AuthUiState())

    composeRule.supportingText("Mejengueros").assertExists()
    composeRule.supportingText("Encontrá cancha y armá la mejenga.").assertExists()
  }

  @Test
  fun nonBlankCredentialsEnableEmailCtaAndSubmitEnteredValues() {
    var submittedEmail: String? = null
    var submittedPassword: String? = null

    composeRule.setLoginScreenContent(
        state = AuthUiState(),
        onEmailSignIn = { email, password ->
          submittedEmail = email
          submittedPassword = password
        },
    )

    composeRule.emailField().performTextInput("player@example.com")
    composeRule.passwordField().performTextInput("secret123")
    composeRule.passwordField().assertIsFocused()
    composeRule.waitForIdle()

    composeRule.emailCtaButton().performScrollTo().assertIsEnabled().performRealClick()
    composeRule.runOnIdle {
      assertEquals("player@example.com", submittedEmail)
      assertEquals("secret123", submittedPassword)
    }
  }

  @Test
  fun focusedFieldTouchClickOnGoogleInvokesCallback() {
    var googleClicks = 0

    composeRule.setLoginScreenContent(
        state = AuthUiState(),
        onGoogleSignIn = { googleClicks += 1 },
    )

    composeRule.emailField().performTextInput("player@example.com")
    composeRule.emailField().assertIsFocused()

    composeRule.googleButton().performScrollTo().assertIsEnabled().performRealClick()

    composeRule.runOnIdle { assertEquals(1, googleClicks) }
  }

  @Test
  fun focusedFieldTouchClickOnMicrosoftInvokesCallback() {
    var microsoftClicks = 0

    composeRule.setLoginScreenContent(
        state = AuthUiState(),
        onMicrosoftSignIn = { microsoftClicks += 1 },
    )

    composeRule.passwordField().performTextInput("secret123")
    composeRule.passwordField().assertIsFocused()

    composeRule.microsoftButton().performScrollTo().assertIsEnabled().performRealClick()

    composeRule.runOnIdle { assertEquals(1, microsoftClicks) }
  }

  @Test
  fun focusedFieldTouchClickOnForgotPasswordInvokesCallback() {
    var forgotClicks = 0

    composeRule.setLoginScreenContent(
        state = AuthUiState(),
        onForgotPassword = { forgotClicks += 1 },
    )

    composeRule.emailField().performTextInput("player@example.com")
    composeRule.emailField().assertIsFocused()

    composeRule.forgotPasswordButton().performScrollTo().assertIsEnabled().performRealClick()

    composeRule.runOnIdle { assertEquals(1, forgotClicks) }
  }

  @Test
  fun focusedFieldTouchClickOnRegisterInvokesCallback() {
    var registerClicks = 0

    composeRule.setLoginScreenContent(
        state = AuthUiState(),
        onRegister = { registerClicks += 1 },
    )

    composeRule.passwordField().performTextInput("secret123")
    composeRule.passwordField().assertIsFocused()

    composeRule.registerButton().assertIsEnabled().performRealClick()

    composeRule.runOnIdle { assertEquals(1, registerClicks) }
  }

  @Test
  fun tappingFieldsFocusesThemAndBackgroundTapClearsFocus() {
    composeRule.setLoginScreenContent(state = AuthUiState())

    composeRule.emailField().assertIsNotFocused()
    composeRule.emailFieldContainer().performFieldContainerTap()
    composeRule.waitForIdle()
    composeRule.emailField().assertIsFocused()

    composeRule.passwordFieldContainer().performScrollTo().performFieldContainerTap()
    composeRule.waitForIdle()
    composeRule.passwordField().assertIsFocused()

    composeRule.loginRoot().performBackgroundTap()
    composeRule.waitForIdle()

    composeRule.passwordField().assertIsNotFocused()
  }

  @Test
  fun loadingStateDisablesEmailAndProviderActionsWhileKeepingProvidersVisible() {
    composeRule.setLoginScreenContent(state = AuthUiState(isLoading = true))

    composeRule.emailCtaButton().assertIsNotEnabled()
    composeRule.providerButton("Continuar con Google").assertExists().assertIsNotEnabled()
    composeRule.providerButton("Continuar con Microsoft").assertExists().assertIsNotEnabled()
    composeRule.supportingText("O seguí con un proveedor disponible hoy").assertExists()
    composeRule.actionButton("¿Olvidaste tu contraseña?").assertIsNotEnabled()
    composeRule.registerButton().assertIsNotEnabled()
  }

  @Test
  fun errorMessageRemainsVisibleWhenAuthStateProvidesOne() {
    composeRule.setLoginScreenContent(
        state = AuthUiState(errorMessage = "Authentication failed"),
    )

    composeRule.supportingText("Authentication failed").assertExists()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setLoginScreenContent(
      state: AuthUiState,
      onEmailSignIn: (String, String) -> Unit = { _, _ -> },
      onGoogleSignIn: () -> Unit = {},
      onMicrosoftSignIn: () -> Unit = {},
      onForgotPassword: () -> Unit = {},
      onRegister: () -> Unit = {},
  ) {
    setContent {
      MejenguerosTheme {
        LoginScreen(
            state = state,
            onEmailSignIn = onEmailSignIn,
            onGoogleSignIn = onGoogleSignIn,
            onMicrosoftSignIn = onMicrosoftSignIn,
            onForgotPassword = onForgotPassword,
            onRegister = onRegister,
        )
      }
    }
  }

  private fun androidx.compose.ui.test.SemanticsNodeInteraction.performRealClick() {
    performTouchInput { click() }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.emailField() =
      onNode(hasSetTextAction() and hasContentDescription("Correo electrónico"))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.emailFieldContainer() =
      onNodeWithTag("Correo electrónico text field container", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.passwordField() =
      onNode(hasSetTextAction() and hasContentDescription("Contraseña"))

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.passwordFieldContainer() =
      onNodeWithTag("Contraseña text field container", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.emailCtaButton() =
      onNodeWithTag("login_email_cta_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.loginRoot() =
      onNodeWithTag("login_root", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.googleButton() =
      onNodeWithTag("login_google_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.microsoftButton() =
      onNodeWithTag("login_microsoft_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.forgotPasswordButton() =
      onNodeWithTag("login_forgot_password_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.registerButton() =
      onNodeWithTag("login_register_button", useUnmergedTree = true)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.providerButton(text: String) =
      actionButton(text)

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.actionButton(text: String) =
      onNode(hasText(text) and buttonRoleMatcher())

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.supportingText(text: String) =
      onNode(hasText(text))

  private fun androidx.compose.ui.test.SemanticsNodeInteraction.performBackgroundTap() {
    performTouchInput { click(androidx.compose.ui.geometry.Offset(5f, 5f)) }
  }

  private fun androidx.compose.ui.test.SemanticsNodeInteraction.performFieldContainerTap() {
    performTouchInput { click(androidx.compose.ui.geometry.Offset(8f, height - 8f)) }
  }

  private fun buttonRoleMatcher() =
      SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
}
