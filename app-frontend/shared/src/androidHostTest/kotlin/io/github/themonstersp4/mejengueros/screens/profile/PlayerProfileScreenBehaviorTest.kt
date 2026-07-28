package io.github.themonstersp4.mejengueros.screens.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerProfileScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysNameEmailAccessibleInitialsAndClickableFavoritesEntry() {
    setProfileContent(displayName = "María González", email = "maria@example.com")

    composeRule.onNodeWithContentDescription("Avatar de María González").assertExists()
    composeRule.onNodeWithText("MG").assertExists()
    composeRule.onNodeWithText("María González").assertExists()
    composeRule.onNodeWithText("maria@example.com").assertExists()
    composeRule.onNodeWithText("Mis canchas favoritas").assertExists()
    composeRule.onNodeWithTag("player_profile_favorite_courts").assertHasClickAction()
    composeRule.onNodeWithText("Consultá y administrá las canchas que guardaste.").assertExists()
  }

  @Test
  fun fallsBackToEmailWhenDisplayNameIsBlank() {
    setProfileContent(displayName = "   ", email = "player@example.com")

    composeRule.onNodeWithContentDescription("Avatar de player@example.com").assertExists()
    composeRule.onNodeWithText("PE").assertExists()
    composeRule.onNodeWithText("player@example.com").assertExists()
  }

  private fun setProfileContent(displayName: String?, email: String) {
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileScreen(
            displayName = displayName,
            email = email,
            contentPadding = PaddingValues(),
        )
      }
    }
  }
}
