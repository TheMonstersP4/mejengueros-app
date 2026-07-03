package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MejenguerosLoadingDialogBehaviorTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun hiddenDialogDoesNotRenderAndVisibleDialogShowsLoadingContentWithoutCancelAction() {
    val visibleState = mutableStateOf(false)

    composeRule.setContent {
      MaterialTheme {
        MejenguerosLoadingDialog(
            visible = visibleState.value,
            title = "Loading title",
            message = "Loading message",
        )
      }
    }

    composeRule
        .onNodeWithTag("mejengueros_loading_dialog", useUnmergedTree = true)
        .assertDoesNotExist()
    composeRule.runOnIdle { visibleState.value = true }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("mejengueros_loading_dialog", useUnmergedTree = true).assertExists()
    composeRule
        .onNodeWithTag("mejengueros_loading_dialog_indicator", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithText("Loading title").assertExists()
    composeRule.onNodeWithText("Loading message").assertExists()
    composeRule
        .onNodeWithTag("mejengueros_loading_dialog_cancel", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun visibleDialogShowsOptionalCancelButtonAndInvokesCallback() {
    var cancelClicks = 0

    composeRule.setLoadingDialogContent(
        visible = true,
        title = "Authenticating",
        message = "Please wait",
        onCancel = { cancelClicks += 1 },
        cancelText = "Stop",
    )

    composeRule.onNodeWithText("Authenticating").assertExists()
    composeRule.onNodeWithText("Please wait").assertExists()
    composeRule.onNodeWithText("Stop").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, cancelClicks) }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setLoadingDialogContent(
      visible: Boolean,
      title: String,
      message: String,
      onCancel: (() -> Unit)? = null,
      cancelText: String = "Cancelar",
  ) {
    setContent {
      MaterialTheme {
        MejenguerosLoadingDialog(
            visible = visible,
            title = title,
            message = message,
            onCancel = onCancel,
            cancelText = cancelText,
        )
      }
    }
  }
}
