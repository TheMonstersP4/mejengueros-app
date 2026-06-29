package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    val renderedRoute = mutableStateOf<AppRoute>(SearchRoute)
    val visitedRoutes = NavBackStack<NavKey>(SearchRoute)
    val expectedDetailRoute =
        CatalogCourtDetailRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )
    val expectedReservationRoute =
        CatalogReservationRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )
    val openedDetailRoute = mutableStateOf<CatalogCourtDetailRoute?>(null)
    val openedReservationRoute = mutableStateOf<CatalogReservationRoute?>(null)
    val shellActions =
        shellActions(
            onDetailOpened = { route ->
              openedDetailRoute.value = route
              if (visitedRoutes.lastOrNull() != route) {
                visitedRoutes.add(route)
              }
              renderedRoute.value = route
            },
            onReservationOpened = { route ->
              openedReservationRoute.value = route
              if (visitedRoutes.lastOrNull() != route) {
                visitedRoutes.add(route)
              }
              renderedRoute.value = route
            },
            onBack = {
              visitedRoutes.removeLastOrNull()
              renderedRoute.value = (visitedRoutes.lastOrNull() as? AppRoute) ?: SearchRoute
            },
        )
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
        when (val route = renderedRoute.value) {
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
        .onNodeWithTag("catalog_court_card_court-id", useUnmergedTree = true)
        .assertExists()
        .assert(SemanticsMatcher("has click action") { hasClickAction().matches(it) })

    composeRule.runOnIdle {
      openedDetailRoute.value = expectedDetailRoute
      visitedRoutes.add(expectedDetailRoute)
      renderedRoute.value = expectedDetailRoute
    }

    composeRule.onNodeWithText("Detalle pendiente").assertExists()
    composeRule.onNodeWithTag("catalog_detail_continue_to_reservation_button").performClick()
    composeRule.onNodeWithText("Reserva pendiente").assertExists()
    composeRule.runOnIdle {
      assertEquals(expectedDetailRoute, openedDetailRoute.value)
      assertEquals(expectedReservationRoute, openedReservationRoute.value)
      assertEquals(
          listOf(SearchRoute, expectedDetailRoute, expectedReservationRoute),
          visitedRoutes.toList(),
      )
    }
  }

  private fun shellActions(
      onDetailOpened: (CatalogCourtDetailRoute) -> Unit,
      onReservationOpened: (CatalogReservationRoute) -> Unit,
      onBack: () -> Unit,
  ): AuthenticatedShellActions =
      AuthenticatedShellActions(
          selectSearch = {},
          selectReservations = {},
          selectNotifications = {},
          selectMyComplex = {},
          returnToMyComplexRoot = {},
          openCatalogCourtDetail = onDetailOpened,
          openCatalogReservation = onReservationOpened,
          openComplexDetail = {},
          openAddCourt = { _, _ -> },
          openCreateComplex = {},
          openCourtAvailability = {},
          closeAddCourtAfterSuccess = {},
          closeCurrentDetail = onBack,
          signOut = {},
      )
}
