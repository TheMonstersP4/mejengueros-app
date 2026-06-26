package io.github.themonstersp4.mejengueros.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun ownerAvailabilityEntrypointStaysHiddenWithoutCourtContext() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            username = "Owner",
            contentPadding = PaddingValues(),
            onCreateComplex = {},
        )
      }
    }

    composeRule.onNodeWithTag("home_owner_availability_button").assertDoesNotExist()
    composeRule.onNodeWithText("Última cancha creada").assertDoesNotExist()
  }

  @Test
  fun ownerAvailabilityEntrypointShowsSavedCourtAndNavigatesOnTap() {
    var reopenClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            username = "Owner",
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            ownerAvailabilityEntrypoint =
                OwnerCourtAvailabilityEntrypoint(
                    courtId = "court-id",
                    courtName = "Cancha 1",
                    complexName = "Mejengas CR",
                ),
            onOpenOwnerAvailabilityEntrypoint = { reopenClicks += 1 },
        )
      }
    }

    composeRule.onNodeWithText("Última cancha creada").assertExists()
    composeRule.onNodeWithText("Cancha 1 · Mejengas CR").assertExists()
    composeRule.onNodeWithTag("home_owner_availability_button").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, reopenClicks) }
  }
}
