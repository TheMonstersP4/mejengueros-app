package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReservationsNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun launcherOpensFormAndBackReturnsToLauncher() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()

    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertExists()
    composeRule.onNodeWithContentDescription("Seleccionar 3 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Comentario temporal")
    composeRule.onNodeWithTag("leave_review_submit_button").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Comentario temporal").assertDoesNotExist()
  }

  @Test
  fun successBackReturnsToLauncherAndClearsDraft() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Otro comentario temporal")
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Otro comentario temporal").assertDoesNotExist()
  }

  @Test
  fun oneStarBlankCommentStaysBlockedUntilCommentIsProvided() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule
        .onNodeWithText("Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia.")
        .assertExists()

    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("El piso estaba resbaloso y no había buena iluminación")

    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertExists()
  }

  @Test
  fun validSubmitReachesSuccessAndExitPathsResetReservationsFlow() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("La cancha estaba impecable")
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()

    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertExists()
    composeRule.onNodeWithText("VOLVER A MIS RESERVAS").performClick()
    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("La cancha estaba impecable").assertDoesNotExist()
    composeRule.onNodeWithContentDescription("Volver").performClick()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    composeRule.onNodeWithText("EXPLORAR CANCHAS").performClick()

    composeRule.onNodeWithText("Buscar canchas").assertExists()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }

    navigationState.selectReservations()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()
  }

  @Test
  fun systemBackFromFormReturnsToLauncherAndClearsDraft() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Borrador que no debe sobrevivir")

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Borrador que no debe sobrevivir").assertDoesNotExist()
  }

  @Test
  fun systemBackFromSuccessReturnsToLauncherAndClearsDraft() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme { ReservationsNavigationTestHost(navigationState = navigationState) }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertExists()

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()
  }

  @Composable
  private fun ReservationsNavigationTestHost(navigationState: AuthenticatedNavigationState) {
    val shellActions =
        AuthenticatedShellActions(
            selectSearch = navigationState::selectSearch,
            selectReservations = navigationState::selectReservations,
            selectNotifications = navigationState::selectNotifications,
            selectMyComplex = navigationState::selectMyComplex,
            returnToSearchRoot = navigationState::returnToSearchRoot,
            returnToMyComplexRoot = navigationState::returnToMyComplexRoot,
            openCatalogCourtDetail = navigationState::openCatalogCourtDetail,
            openCatalogReservation = navigationState::openCatalogReservation,
            openComplexDetail = navigationState::openComplexDetail,
            openAddCourt = navigationState::openAddCourt,
            openCreateComplex = navigationState::openCreateComplex,
            openCourtAvailability = navigationState::openCourtAvailability,
            closeAddCourtAfterSuccess = navigationState::closeAddCourtAfterSuccess,
            closeCurrentDetail = navigationState::closeCurrentDetail,
            signOut = {},
            refreshOwnerRole = {},
            isOwner = false,
            viewingAsPlayer = true,
        )

    when (navigationState.selectedRoute) {
      AuthenticatedTopLevelRoute.Reservations ->
          ReservationsEntryContent(shellActions = shellActions)
      AuthenticatedTopLevelRoute.Search -> Text("Buscar canchas")
      else -> Text("Ruta inesperada")
    }
  }
}

private fun testNavigationState(): AuthenticatedNavigationState =
    AuthenticatedNavigationState(
        selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Reservations),
        searchBackStack = NavBackStack<NavKey>(SearchRoute),
        reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
        notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
        myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
        ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
        myComplexHubReloadRequestKeyState = mutableStateOf(0),
        catalogReloadRequestKeyState = mutableStateOf(0),
        viewingAsPlayerState = mutableStateOf(true),
        hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
    )
