package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTextFieldBehaviorTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun labelTapFocusesEnabledTextFieldAndAllowsInput() {
    composeRule.setTextFieldContent(enabled = true, isError = false, supportingText = null)

    composeRule.textField("Display name").assertIsNotFocused()

    composeRule.onNodeWithText("Display name").performClick()

    composeRule.textField("Display name").assertIsFocused().performTextInput("Misty")
    composeRule.textField("Display name").assertTextEquals("Misty")
  }

  @Test
  fun containerTapFocusesEnabledEditableTextField() {
    composeRule.setTextFieldContent(enabled = true, isError = false, supportingText = null)

    composeRule.textField("Display name").assertIsNotFocused()

    composeRule.onNodeWithTag("Display name text field container").performTouchInput {
      click(Offset(8f, height - 8f))
    }

    composeRule.textField("Display name").assertIsFocused()
  }

  @Test
  fun disabledFieldDoesNotExposeClickableLabelSemantics() {
    composeRule.setTextFieldContent(enabled = false, isError = false, supportingText = null)

    composeRule
        .onNodeWithText("Display name")
        .assert(SemanticsMatcher("has no click action") { !hasClickAction().matches(it) })
  }

  @Test
  fun errorStateExposesSupportingMessageThroughFieldSemantics() {
    composeRule.setTextFieldContent(
        enabled = true,
        isError = true,
        supportingText = "Display name is required",
    )

    composeRule
        .onNodeWithText("Display name is required")
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
    composeRule
        .textField("Display name")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Display name is required",
            )
        )
  }

  @Test
  fun nonErrorStateDoesNotExposeErrorSemantics() {
    composeRule.setTextFieldContent(
        enabled = true,
        isError = false,
        supportingText = "Optional helper",
    )

    composeRule
        .textField("Display name")
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
  }

  @Test
  fun trailingIconDoesNotExpandSingleLineFieldHeight() {
    composeRule.setContent {
      var email by remember { mutableStateOf("player@example.com") }
      var password by remember { mutableStateOf("secret") }

      MaterialTheme {
        Column {
          MejenguerosTextField(
              value = email,
              onValueChange = { email = it },
              label = "Email",
          )
          MejenguerosPasswordField(
              value = password,
              onValueChange = { password = it },
              label = "Password",
          )
        }
      }
    }

    val emailBounds =
        composeRule.onNodeWithTag("Email text field container").getUnclippedBoundsInRoot()
    val emailHeight = emailBounds.bottom - emailBounds.top
    val passwordHeight =
        composeRule.onNodeWithTag("Password text field container").getUnclippedBoundsInRoot().let {
          it.bottom - it.top
        }

    assertEquals(emailHeight, passwordHeight)
  }

  @Test
  fun trailingSlotConstrainsOversizedTrailingContent() {
    composeRule.setContent {
      var plain by remember { mutableStateOf("plain") }
      var withTrailing by remember { mutableStateOf("trailing") }

      MaterialTheme {
        Column {
          MejenguerosTextField(
              value = plain,
              onValueChange = { plain = it },
              label = "Plain",
          )
          MejenguerosTextField(
              value = withTrailing,
              onValueChange = { withTrailing = it },
              label = "With trailing",
              trailingIcon = { Box(modifier = Modifier.size(96.dp)) },
          )
        }
      }
    }

    val plainBounds =
        composeRule.onNodeWithTag("Plain text field container").getUnclippedBoundsInRoot()
    val trailingBounds =
        composeRule.onNodeWithTag("With trailing text field container").getUnclippedBoundsInRoot()

    assertEquals(plainBounds.bottom - plainBounds.top, trailingBounds.bottom - trailingBounds.top)
  }

  @Test
  fun passwordTrailingActionTogglesVisibility() {
    composeRule.setContent {
      var password by remember { mutableStateOf("secret") }

      MaterialTheme {
        MejenguerosPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
        )
      }
    }

    composeRule.onNodeWithContentDescription("Mostrar contraseña").performClick()

    composeRule.onNodeWithContentDescription("Ocultar contraseña").assertExists()
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setTextFieldContent(
      enabled: Boolean,
      isError: Boolean,
      supportingText: String?,
  ) {
    setContent {
      var value by remember { mutableStateOf("") }

      MaterialTheme {
        MejenguerosTextField(
            value = value,
            onValueChange = { value = it },
            label = "Display name",
            enabled = enabled,
            isError = isError,
            supportingText = supportingText,
        )
      }
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.textField(label: String) =
      onNode(hasSetTextAction() and hasContentDescription(label))
}
