package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.presentation.catalog.CatalogFilterOption
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SearchCatalogNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @After
  fun tearDown() {
    stopKoin()
  }

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
                  reloadRequestKey = 0,
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

  @Test
  fun searchFieldForwardsTypedQueryFromCatalogEntry() {
    val typedQueries = mutableListOf<String>()

    composeRule.setContent {
      MejenguerosTheme {
        SearchCatalogEntryContent(
            state =
                CourtCatalogUiState(
                    isLoading = false,
                    visibleCourts =
                        listOf(
                            CourtCatalogItem(
                                id = "court-id",
                                complexId = "complex-id",
                                complexName = "test",
                                courtName = "test",
                                provinceId = "province-id",
                                provinceName = "San Jose",
                                cantonId = "canton-id",
                                cantonName = "San Jose",
                                services = listOf("Sintetico"),
                                ratingAverage = null,
                                ratingCount = 0,
                                imageUrl = null,
                                isReservableToday = true,
                            )
                        ),
                ),
            reloadRequestKey = 0,
            shellActions = shellActions(onDetailOpened = {}, onReservationOpened = {}, onBack = {}),
            onSearchQueryChange = { typedQueries += it },
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
        )
      }
    }

    composeRule.onNodeWithTag("catalog_search_field").performTextInput("test")

    composeRule.runOnIdle { assertEquals("test", typedQueries.last()) }
  }

  @Test
  fun searchEntryLoadsCatalogFromViewModelAndShowsReturnedCourt() {
    val repository = RecordingSearchEntryRepository()

    composeRule.setContent {
      MejenguerosTheme {
        KoinApplication(
            application = {
              modules(
                  module {
                    single<ICourtCatalogRepository> { repository }
                    viewModel { CourtCatalogViewModel(get()) }
                  }
              )
            }
        ) {
          SearchEntry(
              reloadRequestKey = 0,
              shellActions =
                  shellActions(
                      onDetailOpened = {},
                      onReservationOpened = {},
                      onBack = {},
                  ),
          )
        }
      }
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithText("test · test").assertExists()
    composeRule
        .onNodeWithTag("catalog_court_card_test-court", useUnmergedTree = true)
        .assertExists()

    composeRule.runOnIdle {
      assertEquals(listOf(SearchCatalogRequest("", null, null)), repository.requests)
    }
  }

  @Test
  fun searchEntryUpdatesResultsAfterTypingQueryThroughProductionViewModel() {
    val repository = QueryDrivenSearchEntryRepository()
    val testScope = TestScope(StandardTestDispatcher())

    composeRule.setContent {
      MejenguerosTheme {
        KoinApplication(
            application = {
              modules(
                  module {
                    single<ICourtCatalogRepository> { repository }
                    viewModel { CourtCatalogViewModel(get(), testScope) }
                  }
              )
            }
        ) {
          SearchEntry(
              reloadRequestKey = 0,
              shellActions =
                  shellActions(
                      onDetailOpened = {},
                      onReservationOpened = {},
                      onBack = {},
                  ),
          )
        }
      }
    }

    composeRule.runOnIdle { testScope.advanceUntilIdle() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Sin resultados").assertExists()

    composeRule.onNodeWithTag("catalog_search_field").performTextInput("test")

    composeRule.runOnIdle {
      testScope.advanceTimeBy(300)
      testScope.advanceUntilIdle()
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("test · test").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          listOf(
              SearchCatalogRequest("", null, null),
              SearchCatalogRequest("test", null, null),
          ),
          repository.requests,
      )
    }
  }

  @Test
  fun searchEntryShowsErrorStateWhenRepositoryFails() {
    val repository = FailingSearchEntryRepository()
    val testScope = TestScope(StandardTestDispatcher())

    composeRule.setContent {
      MejenguerosTheme {
        KoinApplication(
            application = {
              modules(
                  module {
                    single<ICourtCatalogRepository> { repository }
                    viewModel { CourtCatalogViewModel(get(), testScope) }
                  }
              )
            }
        ) {
          SearchEntry(
              reloadRequestKey = 0,
              shellActions =
                  shellActions(
                      onDetailOpened = {},
                      onReservationOpened = {},
                      onBack = {},
                  ),
          )
        }
      }
    }

    composeRule.runOnIdle { testScope.advanceUntilIdle() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Catálogo no disponible").assertExists()
    composeRule
        .onNodeWithText("No pudimos cargar el catálogo en este momento. Intentá nuevamente.")
        .assertExists()
    composeRule.onNodeWithTag("catalog_retry_button").assertExists()
  }

  @Test
  fun searchEntryReloadsCatalogAfterAddCourtSuccessRequestsNavigationRefresh() {
    val navigationState =
        AuthenticatedNavigationState(
            selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
            searchBackStack = NavBackStack<NavKey>(SearchRoute),
            reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
            notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
            myComplexBackStack =
                NavBackStack<NavKey>(
                    MyComplexRoute,
                    ComplexDetailRoute("complex-id"),
                    AddCourtRoute("complex-id", "North Sports Center"),
                ),
            ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
            myComplexHubReloadRequestKeyState = mutableStateOf(0),
            searchCatalogReloadRequestKeyState = mutableStateOf(0),
        )
    val repository = ReloadingSearchEntryRepository()
    val testScope = TestScope(StandardTestDispatcher())

    composeRule.setContent {
      MejenguerosTheme {
        KoinApplication(
            application = {
              modules(
                  module {
                    single<ICourtCatalogRepository> { repository }
                    viewModel { CourtCatalogViewModel(get(), testScope) }
                  }
              )
            }
        ) {
          val shellActions = remember(navigationState) { navigationShellActions(navigationState) }
          SearchEntry(
              shellActions = shellActions,
              reloadRequestKey = navigationState.searchCatalogReloadRequestKey,
          )
        }
      }
    }

    composeRule.runOnIdle { testScope.advanceUntilIdle() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Sin resultados").assertExists()

    composeRule.runOnIdle {
      repository.shouldReturnCourt = true
      navigationState.closeAddCourtAfterSuccess()
    }
    composeRule.runOnIdle { testScope.advanceUntilIdle() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("test · test").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          listOf(
              SearchCatalogRequest("", null, null),
              SearchCatalogRequest("", null, null),
          ),
          repository.requests,
      )
      assertEquals(1, navigationState.searchCatalogReloadRequestKey)
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
          requestSearchCatalogReload = {},
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

  private fun navigationShellActions(
      navigationState: AuthenticatedNavigationState,
  ): AuthenticatedShellActions =
      AuthenticatedShellActions(
          selectSearch = navigationState::selectSearch,
          selectReservations = navigationState::selectReservations,
          selectNotifications = navigationState::selectNotifications,
          selectMyComplex = navigationState::selectMyComplex,
          returnToMyComplexRoot = navigationState::returnToMyComplexRoot,
          requestSearchCatalogReload = navigationState::requestSearchCatalogReload,
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

private class RecordingSearchEntryRepository : ICourtCatalogRepository {
  val requests = mutableListOf<SearchCatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    requests += SearchCatalogRequest(searchQuery, provinceId, cantonId)
    return listOf(searchEntryCourt)
  }
}

private class QueryDrivenSearchEntryRepository : ICourtCatalogRepository {
  val requests = mutableListOf<SearchCatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    requests += SearchCatalogRequest(searchQuery, provinceId, cantonId)
    return if (searchQuery == "test") listOf(searchEntryCourt) else emptyList()
  }
}

private class FailingSearchEntryRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> = error("boom")
}

private class ReloadingSearchEntryRepository : ICourtCatalogRepository {
  var shouldReturnCourt = false
  val requests = mutableListOf<SearchCatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    requests += SearchCatalogRequest(searchQuery, provinceId, cantonId)
    return if (shouldReturnCourt) listOf(searchEntryCourt) else emptyList()
  }
}

private data class SearchCatalogRequest(
    val searchQuery: String?,
    val provinceId: String?,
    val cantonId: String?,
)

private val searchEntryCourt =
    CourtCatalogItem(
        id = "test-court",
        complexId = "test-complex",
        complexName = "test",
        courtName = "test",
        provinceId = "province-test",
        provinceName = "San Jose",
        cantonId = "canton-test",
        cantonName = "San Jose",
        services = listOf("Sintetico"),
        ratingAverage = null,
        ratingCount = 0,
        imageUrl = null,
        isReservableToday = true,
    )
