package io.github.themonstersp4.mejengueros.screens.kit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import io.github.themonstersp4.mejengueros.ui.components.toCoordinatePairText
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComponentKitScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun compactLocationFieldOpensPickerFromKitFlow() {
    var openPickerCalls = 0

    composeRule.setContent {
      MejenguerosTheme {
        ComponentKitScreen(
            contentPadding = PaddingValues(),
            onOpenAvailabilitySelectors = {},
            selectedLocation = null,
            onOpenLocationPicker = { openPickerCalls += 1 },
        )
      }
    }

    composeRule.onNodeWithText("Seleccionar ubicación").performScrollTo().performClick()

    composeRule.onNodeWithText("Ubicación").assertExists()
    composeRule.runOnIdle { assertEquals(1, openPickerCalls) }
  }

  @Test
  fun compactLocationFieldShowsSelectedCoordinatesAndChangeAction() {
    val selectedLocation = ComponentKitDemoLocationPickerCenter

    composeRule.setContent {
      MejenguerosTheme {
        ComponentKitScreen(
            contentPadding = PaddingValues(),
            onOpenAvailabilitySelectors = {},
            selectedLocation = selectedLocation,
            onOpenLocationPicker = {},
        )
      }
    }

    composeRule.onNodeWithText("Ubicación seleccionada").performScrollTo().assertExists()
    composeRule
        .onNodeWithText("Coordenadas: ${selectedLocation.toCoordinatePairText()}")
        .assertExists()
    composeRule.onNodeWithText("Cambiar ubicación").assertExists()
  }
}
