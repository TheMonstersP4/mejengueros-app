package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.catalog.CatalogFilterOption
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchCatalogNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun catalogLivesUnderSearchAndHandsOffToPendingDetailAndReservation() {
    val navigationState =
        AuthenticatedNavigationState(
            selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
            searchBackStack = NavBackStack<NavKey>(SearchRoute),
            reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
            notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
            myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
            ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
            myComplexHubReloadRequestKeyState = mutableStateOf(0),
        )
    val shellActions = shellActions(navigationState)
    val state =
        CourtCatalogUiState(
            isLoading = false,
            availableProvinces = listOf(CatalogFilterOption("province-1", "San José")),
            availableCantons = listOf(CatalogFilterOption("canton-1", "Escazú")),
            visibleCourts =
                listOf(
                    CourtCatalogItem(
                        id = "court-id",
                        complexId = "complex-id",
                        complexName = "Mejengas CR",
                        courtName = "Cancha 1",
                        provinceId = "province-1",
                        provinceName = "San José",
                        cantonId = "canton-1",
                        cantonName = "Escazú",
                        services = listOf("Parqueo"),
                        ratingAverage = 4.8,
                        ratingCount = 12,
                        imageUrl = null,
                        isReservableToday = true,
                    )
                ),
        )

    composeRule.setContent {
      MejenguerosTheme {
        when (val route = navigationState.currentBackStack.lastOrNull()) {
          SearchRoute ->
              SearchCatalogEntryContent(
                  state = state,
                  shellActions = shellActions,
                  onSearchQueryChange = {},
                  onProvinceSelected = {},
                  onCantonSelected = {},
                  onRetryLoad = {},
              )
          is CatalogCourtDetailRoute ->
              CatalogCourtDetailEntry(route = route, shellActions = shellActions)
          is CatalogReservationRoute ->
              CatalogReservationEntry(route = route, shellActions = shellActions)
          else -> error("Unexpected route $route")
        }
      }
    }

    composeRule.onNodeWithText("Canchas").assertExists()
    composeRule.onNodeWithText("Home").assertDoesNotExist()
    composeRule.onNodeWithText("Demo").assertDoesNotExist()
    composeRule
        .onNodeWithTag("catalog_court_card_court-id")
        .assertExists()
        .assert(SemanticsMatcher("has click action") { hasClickAction().matches(it) })

    composeRule.runOnIdle {
      navigationState.openCatalogCourtDetail(
          CatalogCourtDetailRoute(
              courtId = "court-id",
              complexId = "complex-id",
              complexName = "Mejengas CR",
              courtName = "Cancha 1",
          )
      )
    }
    composeRule.onNodeWithText("Detalle pendiente").assertExists()
    composeRule.runOnIdle {
      navigationState.openCatalogReservation(
          CatalogReservationRoute(
              courtId = "court-id",
              complexId = "complex-id",
              complexName = "Mejengas CR",
              courtName = "Cancha 1",
          )
      )
    }
    composeRule.onNodeWithText("Reserva pendiente").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          listOf(
              SearchRoute,
              CatalogCourtDetailRoute(
                  courtId = "court-id",
                  complexId = "complex-id",
                  complexName = "Mejengas CR",
                  courtName = "Cancha 1",
              ),
              CatalogReservationRoute(
                  courtId = "court-id",
                  complexId = "complex-id",
                  complexName = "Mejengas CR",
                  courtName = "Cancha 1",
              ),
          ),
          navigationState.currentBackStack.toList(),
      )
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
    }
  }

  private fun shellActions(
      navigationState: AuthenticatedNavigationState,
  ): AuthenticatedShellActions =
      AuthenticatedShellActions(
          selectSearch = navigationState::selectSearch,
          selectReservations = navigationState::selectReservations,
          selectNotifications = navigationState::selectNotifications,
          selectMyComplex = navigationState::selectMyComplex,
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
      )
}
