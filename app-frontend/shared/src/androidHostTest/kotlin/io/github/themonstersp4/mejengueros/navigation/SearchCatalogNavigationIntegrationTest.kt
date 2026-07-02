package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.presentation.catalog.CatalogFilterOption
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchCatalogNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun catalogLivesUnderSearchAndHandsOffToDetailAndReservation() = runTest {
    val renderedRoute = mutableStateOf<AppRoute>(SearchRoute)
    val visitedRoutes = NavBackStack<NavKey>(SearchRoute)
    val expectedDetailRoute =
        CatalogCourtDetailRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
            provinceName = "San José",
            cantonName = "Escazú",
            services = listOf("Parqueo"),
            ratingAverage = 4.8,
            ratingCount = 12,
            imageUrl = null,
            isReservableToday = true,
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
    val detailViewModel =
        CourtDetailViewModel(
            courtId = "court-id",
            repository = FakeCourtDetailRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

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
              CatalogCourtDetailEntryContent(
                  route = route,
                  viewModel = detailViewModel,
                  shellActions = shellActions,
              )
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

    composeRule.onNodeWithTag("court_detail_title").assertExists()
    composeRule.onNodeWithTag("court_detail_disponibilidad_section").assertExists()
    composeRule.onNodeWithTag("court_detail_reserve_button").assertExists().performClick()
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

  @Test
  fun nonOwnerSearchShowsCreateComplexAppBarActionAndPushesCreateComplexRoute() {
    val navigationState = testNavigationState()

    composeRule.setContent { SearchCatalogCreateComplexTestHost(navigationState = navigationState) }

    composeRule.onNodeWithContentDescription("Crear complejo").assertExists().performClick()
    composeRule.runOnIdle {
      assertEquals(CreateComplexRoute, navigationState.currentBackStack.lastOrNull())
      assertEquals(
          listOf(SearchRoute, CreateComplexRoute),
          navigationState.currentBackStack.toList(),
      )
    }
  }

  @Test
  fun ownerSearchDoesNotShowCreateComplexAppBarAction() {
    composeRule.setContent {
      MejenguerosTheme {
        SearchCatalogEntryContent(
            state = CourtCatalogUiState(isLoading = false),
            shellActions =
                shellActions(
                    onDetailOpened = {},
                    onReservationOpened = {},
                    onBack = {},
                    isOwner = true,
                    viewingAsPlayer = true,
                ),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Crear complejo").assertDoesNotExist()
  }

  private fun shellActions(
      onDetailOpened: (CatalogCourtDetailRoute) -> Unit,
      onReservationOpened: (CatalogReservationRoute) -> Unit,
      onBack: () -> Unit,
      onCreateComplexOpened: () -> Unit = {},
      isOwner: Boolean = false,
      viewingAsPlayer: Boolean = false,
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
          openCreateComplex = onCreateComplexOpened,
          openCourtAvailability = {},
          closeAddCourtAfterSuccess = {},
          closeCurrentDetail = onBack,
          signOut = {},
          refreshOwnerRole = {},
          isOwner = isOwner,
          viewingAsPlayer = viewingAsPlayer,
      )

  private fun testNavigationState(): AuthenticatedNavigationState =
      AuthenticatedNavigationState(
          selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
          searchBackStack = NavBackStack<NavKey>(SearchRoute),
          reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
          notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
          myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
          ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
          myComplexHubReloadRequestKeyState = mutableStateOf(0),
          catalogReloadRequestKeyState = mutableStateOf(0),
          viewingAsPlayerState = mutableStateOf(false),
      )

  @Composable
  private fun SearchCatalogCreateComplexTestHost(navigationState: AuthenticatedNavigationState) {
    MejenguerosTheme {
      SearchCatalogEntryContent(
          state = CourtCatalogUiState(isLoading = false),
          shellActions =
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
                  refreshOwnerRole = {},
              ),
          onSearchQueryChange = {},
          onProvinceSelected = {},
          onCantonSelected = {},
          onRetryLoad = {},
      )
    }
  }
}

private class FakeCourtDetailRepository : ICourtDetailRepository {
  override suspend fun getReservableSlotsForToday(courtId: String): List<ReservableSlot> =
      listOf(
          ReservableSlot(
              startsAtUtc = "2026-07-01T08:00:00.000Z",
              endsAtUtc = "2026-07-01T09:00:00.000Z",
          ),
          ReservableSlot(
              startsAtUtc = "2026-07-01T09:00:00.000Z",
              endsAtUtc = "2026-07-01T10:00:00.000Z",
          ),
      )
}
