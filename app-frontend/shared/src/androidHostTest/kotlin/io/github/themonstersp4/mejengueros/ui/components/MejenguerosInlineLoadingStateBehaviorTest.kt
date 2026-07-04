package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MejenguerosInlineLoadingStateBehaviorTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun inlineLoadingStateExposesIndeterminateProgressSemantics() {
    composeRule.setContent {
      MaterialTheme {
        MejenguerosInlineLoadingState(
            text = "Loading courts…",
            containerTestTag = "inline_loading",
            indicatorTestTag = "inline_loading_indicator",
        )
      }
    }

    composeRule.onNodeWithTag("inline_loading", useUnmergedTree = true).assertExists()
    composeRule
        .onNodeWithTag("inline_loading_indicator", useUnmergedTree = true)
        .assertExists()
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        )
    composeRule.onNodeWithText("Loading courts…").assertExists()
  }
}
