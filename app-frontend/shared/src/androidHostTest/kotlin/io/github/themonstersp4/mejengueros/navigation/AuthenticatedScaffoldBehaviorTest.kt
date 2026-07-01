package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitDemoLocationPickerCenter
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationMapState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerActions
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerScreen
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerState
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthenticatedScaffoldBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun backNavigationUsesIconOnlyAndHidesLegacyLabel() {
    var backClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            onNavigateBack = { backClicks += 1 },
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("scaffold_body"))
        }
      }
    }

    composeRule.onNodeWithContentDescription("Volver").assertExists().performClick()
    composeRule.onNodeWithText("Back").assertDoesNotExist()
    composeRule.onNodeWithText("Volver").assertDoesNotExist()
    composeRule.runOnIdle { assertEquals(1, backClicks) }
  }

  @Test
  fun signOutRequiresConfirmationBeforeInvokingCallback() {
    var signOutClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.Search,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = { signOutClicks += 1 },
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("scaffold_body"))
        }
      }
    }

    composeRule.onNodeWithContentDescription("Cerrar sesión").assertExists().performClick()
    composeRule.runOnIdle { assertEquals(0, signOutClicks) }
    composeRule.onNodeWithText("¿Cerrar sesión?").assertExists()
    composeRule.onNodeWithText("Cancelar").performClick()
    composeRule.onNodeWithText("¿Cerrar sesión?").assertDoesNotExist()
    composeRule.runOnIdle { assertEquals(0, signOutClicks) }

    composeRule.onNodeWithContentDescription("Cerrar sesión").performClick()
    composeRule.onNodeWithText("Cerrar sesión").performClick()
    composeRule.runOnIdle { assertEquals(1, signOutClicks) }
  }

  @Test
  fun fullScreenOverlayHidesBackgroundScaffoldSemanticsWhileKeepingPickerReachable() {
    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            overlayVisible = true,
            overlayContent = {
              MejenguerosLocationPickerScreen(
                  state =
                      MejenguerosLocationPickerState(
                          draftLocation = ComponentKitDemoLocationPickerCenter,
                          selectedLocation = null,
                      ),
                  actions =
                      MejenguerosLocationPickerActions(
                          onDraftLocationChange = {},
                          onConfirm = {},
                          onDismiss = {},
                      ),
                  mapContent = { scope ->
                    SideEffect { scope.onMapStateChange(MejenguerosLocationMapState.Ready) }
                    Box(modifier = scope.modifier.testTag("fake_map"))
                  },
              )
            },
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("scaffold_body")) {
            Text("Scaffold body")
          }
        }
      }
    }

    composeRule.onNodeWithTag("location_picker_overlay").assertExists()
    composeRule
        .onNodeWithTag("location_picker_overlay")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.PaneTitle,
                "Location picker",
            )
        )
    composeRule.onNodeWithText("Cancelar").assertExists()
    composeRule.onNodeWithText("Usar esta ubicación").assertExists()
    composeRule.onNodeWithTag("fake_map").assertExists()
    composeRule.onNodeWithText("Mejengueros").assertDoesNotExist()
    composeRule.onNodeWithText("Mi complejo").assertDoesNotExist()
    composeRule.onNodeWithText("Scaffold body").assertDoesNotExist()
  }

  @Test
  fun bottomBarShowsProductDestinationsAndMarksSelectedTab() {
    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            isOwner = true,
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("scaffold_body")) {
            Text("Scaffold body")
          }
        }
      }
    }

    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithText("Reservas").assertExists()
    composeRule.onNodeWithText("Notificaciones").assertExists()
    composeRule.onNodeWithText("Mi complejo").assertExists().assertIsSelected()
  }

  @Test
  fun playerShellHidesMyComplexTab() {
    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.Search,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            isOwner = false,
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding))
        }
      }
    }

    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithText("Reservas").assertExists()
    composeRule.onNodeWithText("Notificaciones").assertExists()
    composeRule.onNodeWithText("Mi complejo").assertDoesNotExist()
  }

  @Test
  fun ownerShellShowsMyComplexTab() {
    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.Search,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            isOwner = true,
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding))
        }
      }
    }

    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithText("Reservas").assertExists()
    composeRule.onNodeWithText("Notificaciones").assertExists()
    composeRule.onNodeWithText("Mi complejo").assertExists()
  }

  @Test
  fun hiddenOverlayKeepsScaffoldVisibleAndDoesNotInvokeOverlayContent() {
    var overlayInvocationCount = 0

    composeRule.setContent {
      MejenguerosTheme {
        AuthenticatedScaffold(
            selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
            onSearchSelected = {},
            onReservationsSelected = {},
            onNotificationsSelected = {},
            onMyComplexSelected = {},
            onSignOut = {},
            isOwner = true,
            overlayVisible = false,
            overlayContent = {
              overlayInvocationCount += 1
              Text("Unexpected overlay", modifier = Modifier.testTag("hidden_overlay_content"))
            },
        ) { contentPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("scaffold_body")) {
            Text("Scaffold body")
          }
        }
      }
    }

    composeRule.onNodeWithText("Mejengueros").assertExists()
    composeRule.onNodeWithText("Mi complejo").assertExists()
    composeRule.onNodeWithText("Scaffold body").assertExists()
    composeRule.onNodeWithTag("hidden_overlay_content").assertDoesNotExist()
    composeRule.onNodeWithText("Unexpected overlay").assertDoesNotExist()
    assertEquals(0, overlayInvocationCount)
  }
}
