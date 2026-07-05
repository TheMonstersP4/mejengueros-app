package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ReservableDay
import io.github.themonstersp4.mejengueros.domain.model.ReservableSlot
import io.github.themonstersp4.mejengueros.domain.model.ReservationAvailabilityStatus
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.presentation.catalog.CatalogFilterOption
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationContext
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationUiState
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationViewModel
import io.github.themonstersp4.mejengueros.screens.placeholder.ProductPlaceholderScreen
import io.github.themonstersp4.mejengueros.screens.reservation.ReservationScreen
import io.github.themonstersp4.mejengueros.screens.reservation.ReservationScreenActions
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import io.github.themonstersp4.mejengueros.ui.components.CourtImagePickerController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchCatalogNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun catalogLivesUnderSearchAndHandsOffToDetailAndReservation() = runTest {
    stopKoin()
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
            provinceName = "San José",
            cantonName = "Escazú",
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
    startKoin {
      modules(
          module {
            single<IReservationRepository> { FakeReservationRepository() }
            viewModel { parameters ->
              ReservationViewModel(parameters.get<ReservationContext>(), get())
            }
          }
      )
    }

    try {
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

      composeRule
          .onNodeWithTag("catalog_court_card_court-id", useUnmergedTree = true)
          .performClick()

      composeRule.runOnIdle {
        assertEquals(expectedDetailRoute, openedDetailRoute.value)
        assertEquals(expectedDetailRoute, renderedRoute.value)
        assertEquals(listOf(SearchRoute, expectedDetailRoute), visitedRoutes.toList())
      }

      composeRule.onNodeWithText("Detalle de cancha").assertExists()
      composeRule.onNodeWithTag("court_detail_title").assertExists()
      composeRule.onNodeWithTag("court_detail_disponibilidad_section").assertExists()
      composeRule.onNodeWithTag("court_detail_reserve_button").assertExists().performClick()
      composeRule.onNodeWithText("Reservar").assertExists()
      composeRule.onNodeWithText("Mejengas CR · Cancha 1").assertDoesNotExist()
      composeRule.onNodeWithText("ELEGÍ EL DÍA").assertExists()
      composeRule.onNodeWithText("CONFIRMAR RESERVA").assertExists()
      composeRule.onNodeWithText("Reserva pendiente").assertDoesNotExist()
      composeRule.runOnIdle {
        assertEquals(expectedDetailRoute, openedDetailRoute.value)
        assertEquals(expectedReservationRoute, openedReservationRoute.value)
        assertEquals(
            listOf(SearchRoute, expectedDetailRoute, expectedReservationRoute),
            visitedRoutes.toList(),
        )
      }
    } finally {
      stopKoin()
    }
  }

  @Test
  fun reopeningSameCourtReservationAfterSuccessStartsFreshSelectionState() = runTest {
    stopKoin()
    val navigationState = testNavigationState()
    val reservationScope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val createdReservationViewModels = mutableListOf<ReservationViewModel>()
    val state =
        CourtCatalogUiState(
            isLoading = false,
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
    startKoin {
      modules(
          module {
            single<IReservationRepository> { FakeReservationRepository() }
            viewModel { parameters ->
              ReservationViewModel(
                      context = parameters.get<ReservationContext>(),
                      repository = get(),
                      coroutineScope = reservationScope,
                  )
                  .also(createdReservationViewModels::add)
            }
          }
      )
    }

    try {
      advanceUntilIdle()

      composeRule.setContent {
        MejenguerosTheme {
          SearchReservationFlowTestHost(
              navigationState = navigationState,
              catalogState = state,
              detailViewModel = detailViewModel,
          )
        }
      }

      composeRule
          .onNodeWithTag("catalog_court_card_court-id", useUnmergedTree = true)
          .performClick()
      composeRule.onNodeWithTag("court_detail_reserve_button").performClick()
      composeRule.runOnIdle { assertEquals(1, createdReservationViewModels.size) }
      createdReservationViewModels
          .last()
          .selectSlot(createdReservationViewModels.last().uiState.value.slots.first().id)
      composeRule.onNode(hasText("CONFIRMAR RESERVA") and hasClickAction()).performClick()
      advanceUntilIdle()
      composeRule.onNodeWithText("¡GOOOL! RESERVA CONFIRMADA").assertExists()
      composeRule.onNodeWithText("VER MIS RESERVAS").assertExists()
      navigationState.returnToSearchRoot()
      navigationState.selectReservations()
      composeRule.runOnIdle {
        assertEquals(AuthenticatedTopLevelRoute.Reservations, navigationState.selectedRoute)
      }

      navigationState.selectSearch()
      composeRule.waitForIdle()

      composeRule
          .onNodeWithTag("catalog_court_card_court-id", useUnmergedTree = true)
          .performClick()
      composeRule.onNodeWithTag("court_detail_reserve_button").performClick()
      composeRule.runOnIdle { assertEquals(2, createdReservationViewModels.size) }

      composeRule.onNodeWithText("¡GOOOL! RESERVA CONFIRMADA").assertDoesNotExist()
      composeRule.onNodeWithText("VER MIS RESERVAS").assertDoesNotExist()
      composeRule.onNodeWithText("ELEGÍ EL DÍA").assertExists()
      composeRule.onNodeWithText("CONFIRMAR RESERVA").assertExists()
      composeRule.runOnIdle {
        val route = navigationState.currentBackStack.last() as CatalogReservationRoute
        assertEquals(2L, route.attemptId)
      }
    } finally {
      stopKoin()
    }
  }

  @Test
  fun reservationFlowShowsEmptyDiscoveryStateWhenNoReservableDaysAreReturned() = runTest {
    stopKoin()
    val renderedRoute = mutableStateOf<AppRoute>(SearchRoute)
    val visitedRoutes = NavBackStack<NavKey>(SearchRoute)
    val shellActions =
        shellActions(
            onDetailOpened = { route ->
              if (visitedRoutes.lastOrNull() != route) {
                visitedRoutes.add(route)
              }
              renderedRoute.value = route
            },
            onReservationOpened = { route ->
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
    startKoin {
      modules(
          module {
            single<IReservationRepository> {
              FakeReservationRepository(reservableDays = emptyList())
            }
            viewModel { parameters ->
              ReservationViewModel(parameters.get<ReservationContext>(), get())
            }
          }
      )
    }

    try {
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

      composeRule
          .onNodeWithTag("catalog_court_card_court-id", useUnmergedTree = true)
          .performClick()
      composeRule.onNodeWithTag("court_detail_reserve_button").performClick()

      composeRule.onNodeWithText("Reservar").assertExists()
      composeRule.onNodeWithText("Sin fechas disponibles").assertExists()
      composeRule
          .onNodeWithText(
              "No encontramos días con horarios disponibles para esta cancha en este momento. Intentá de nuevo más tarde."
          )
          .assertExists()
    } finally {
      stopKoin()
    }
  }

  @Test
  fun reservationScreenShowsDiscoveryLoadErrorAndRetryAction() {
    composeRule.setContent {
      MejenguerosTheme {
        ReservationScreen(
            state =
                ReservationUiState(
                    isLoadingSlots = false,
                    loadErrorMessage =
                        "No pudimos cargar los horarios disponibles. Intentá nuevamente.",
                ),
            contentPadding = PaddingValues(),
            actions =
                ReservationScreenActions(
                    onDateSelected = {},
                    onSlotSelected = {},
                    onConfirmReservation = {},
                    onRetryLoad = {},
                    onViewOtherHours = {},
                    onViewReservations = {},
                    onReturnToCatalog = {},
                ),
        )
      }
    }

    composeRule.onNodeWithText("No pudimos cargar horarios").assertExists()
    composeRule
        .onNodeWithText("No pudimos cargar los horarios disponibles. Intentá nuevamente.")
        .assertExists()
    composeRule.onNode(hasText("Reintentar") and hasClickAction()).assertExists()
  }

  @Test
  fun reservationScreenShowsInlineLoadingIndicatorForSlots() {
    composeRule.setContent {
      MejenguerosTheme {
        ReservationScreen(
            state = ReservationUiState(isLoadingSlots = true),
            contentPadding = PaddingValues(),
            actions =
                ReservationScreenActions(
                    onDateSelected = {},
                    onSlotSelected = {},
                    onConfirmReservation = {},
                    onRetryLoad = {},
                    onViewOtherHours = {},
                    onViewReservations = {},
                    onReturnToCatalog = {},
                ),
        )
      }
    }

    composeRule.onNodeWithTag("reservation_slots_loading", useUnmergedTree = true).assertExists()
    composeRule
        .onNodeWithTag("reservation_slots_loading_indicator", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithText("Cargando horarios disponibles").assertExists()
  }

  @Test
  fun nonOwnerSearchShowsCreateComplexAppBarActionAndPushesCreateComplexRoute() = runTest {
    val navigationState = testNavigationState()
    val viewModel =
        CreateComplexViewModel(
            complexRepository = CreateComplexCatalogRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

    composeRule.setContent {
      SearchCatalogCreateComplexTestHost(
          navigationState = navigationState,
          createComplexViewModel = viewModel,
      )
    }

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
  fun createComplexRouteShowsImagePickerCtaThroughAndroidEntryWiring() = runTest {
    val navigationState = testNavigationState().apply { openCreateComplex() }
    val viewModel =
        CreateComplexViewModel(
            complexRepository = CreateComplexCatalogRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()
    viewModel.updateComplexName("North Sports Center")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.goToFirstCourtStep()

    composeRule.setContent {
      SearchCatalogCreateComplexTestHost(
          navigationState = navigationState,
          isOwner = true,
          createComplexViewModel = viewModel,
      )
    }

    composeRule.onNodeWithTag("create_complex_root", useUnmergedTree = true).assertExists()
    composeRule
        .onNodeWithText("Opcional. Podés agregar una imagen ahora o dejarla para más adelante.")
        .assertExists()
    composeRule.onNodeWithTag("create_complex_pick_court_image_button").assertExists()
    composeRule.onNodeWithText("Seleccionar imagen").assertExists()
  }

  @Test
  fun createComplexRouteHidesImagePickerCtaWhenPickerSeamReportsUnavailable() = runTest {
    val navigationState = testNavigationState().apply { openCreateComplex() }
    val viewModel =
        CreateComplexViewModel(
            complexRepository = CreateComplexCatalogRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()
    viewModel.updateComplexName("North Sports Center")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.goToFirstCourtStep()

    composeRule.setContent {
      SearchCatalogCreateComplexTestHost(
          navigationState = navigationState,
          isOwner = true,
          createComplexViewModel = viewModel,
          courtImagePickerController = CourtImagePickerController(isAvailable = false, launch = {}),
      )
    }

    composeRule.onNodeWithTag("create_complex_root", useUnmergedTree = true).assertExists()
    composeRule.onNodeWithTag("create_complex_pick_court_image_button").assertDoesNotExist()
    composeRule
        .onNodeWithText("Opcional. Podés agregar una imagen ahora o dejarla para más adelante.")
        .assertDoesNotExist()
  }

  @Test
  fun createComplexRouteUpdatesPreviewWhenInjectedPickerLaunchReturnsImage() = runTest {
    val navigationState = testNavigationState().apply { openCreateComplex() }
    val viewModel =
        CreateComplexViewModel(
            complexRepository = CreateComplexCatalogRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    val pickedImage = localCourtImage(fileName = "route-picked-first-court.png")
    advanceUntilIdle()
    viewModel.updateComplexName("North Sports Center")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.goToFirstCourtStep()

    composeRule.setContent {
      SearchCatalogCreateComplexTestHost(
          navigationState = navigationState,
          isOwner = true,
          createComplexViewModel = viewModel,
          courtImagePickerController =
              CourtImagePickerController(
                  isAvailable = true,
                  launch = { viewModel.updateSelectedCourtImage(pickedImage) },
              ),
      )
    }

    composeRule
        .onNodeWithTag("create_complex_pick_court_image_button")
        .performScrollTo()
        .performClick()

    composeRule.onNodeWithTag("create_complex_court_image_preview").assertExists()
    composeRule.onNodeWithText("route-picked-first-court.png").assertExists()
    composeRule.onNodeWithText("Cambiar imagen").assertExists()
    composeRule.onNodeWithTag("create_complex_clear_court_image_button").assertExists()
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

  @Test
  fun nonOwnerRestoredCreateComplexOwnerRouteRedirectsToSearchWithoutRenderingCreateComplex() =
      runTest {
        val navigationState =
            testNavigationState().apply {
              selectMyComplex()
              openCreateComplex()
            }
        val viewModel =
            CreateComplexViewModel(
                complexRepository = CreateComplexCatalogRepository(),
                coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
            )
        advanceUntilIdle()

        composeRule.setContent {
          SearchCatalogCreateComplexTestHost(
              navigationState = navigationState,
              isOwner = false,
              createComplexViewModel = viewModel,
          )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Canchas").assertExists()
        composeRule
            .onNodeWithTag("create_complex_root", useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.runOnIdle {
          assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
          assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
        }
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
          returnToSearchRoot = {},
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
          hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
      )

  @Composable
  private fun SearchReservationFlowTestHost(
      navigationState: AuthenticatedNavigationState,
      catalogState: CourtCatalogUiState,
      detailViewModel: CourtDetailViewModel,
  ) {
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
      AuthenticatedTopLevelRoute.Search ->
          when (val route = navigationState.currentBackStack.lastOrNull()) {
            SearchRoute ->
                SearchCatalogEntryContent(
                    state = catalogState,
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
            else -> Text("Ruta inesperada")
          }
      AuthenticatedTopLevelRoute.Reservations ->
          AuthenticatedScaffold(
              selectedRoute = AuthenticatedTopLevelRoute.Reservations,
              onSearchSelected = shellActions.selectSearch,
              onReservationsSelected = shellActions.selectReservations,
              onNotificationsSelected = shellActions.selectNotifications,
              onMyComplexSelected = shellActions.selectMyComplex,
              onSignOut = shellActions.signOut,
              isOwner = shellActions.isOwner,
              viewingAsPlayer = shellActions.viewingAsPlayer,
              onSwitchToPlayerView = shellActions.switchToPlayerView,
              onSwitchToOwnerView = shellActions.switchToOwnerView,
              chrome = AuthenticatedScaffoldChrome(title = "Mis reservas"),
          ) { contentPadding ->
            ProductPlaceholderScreen(
                title = "Mis reservas",
                description =
                    "Pronto vas a poder revisar tus reservas activas e historial desde aquí. Por ahora esta área sigue siendo un placeholder controlado.",
                contentPadding = contentPadding,
            )
          }
      else -> Text("Ruta inesperada")
    }
  }

  @Composable
  private fun SearchCatalogCreateComplexTestHost(
      navigationState: AuthenticatedNavigationState,
      isOwner: Boolean = false,
      createComplexViewModel: CreateComplexViewModel? = null,
      courtImagePickerController: CourtImagePickerController? = null,
  ) {
    MejenguerosTheme {
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
              isOwner = isOwner,
          )

      when (navigationState.currentBackStack.lastOrNull()) {
        SearchRoute ->
            SearchCatalogEntryContent(
                state = CourtCatalogUiState(isLoading = false),
                shellActions = shellActions,
                onSearchQueryChange = {},
                onProvinceSelected = {},
                onCantonSelected = {},
                onRetryLoad = {},
            )
        CreateComplexRoute ->
            CreateComplexEntryContent(
                authenticatedNavigationState = navigationState,
                shellActions = shellActions,
                createComplexViewModel = createComplexViewModel ?: error("Missing test view model"),
                courtImagePickerController = courtImagePickerController,
            )
        else -> Text("Ruta inesperada")
      }
    }
  }
}

private class CreateComplexCatalogRepository : IComplexRepository {
  override suspend fun getProvinces(): List<Province> =
      listOf(Province(id = "province-1", code = "SJ", name = "San José"))

  override suspend fun getCantons(provinceId: String): List<Canton> =
      listOf(
          Canton(
              id = "canton-1",
              provinceId = provinceId,
              code = "SJ-ESC",
              name = "Escazú",
          )
      )

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> =
      when (scope) {
        ServiceScope.COMPLEX ->
            listOf(ServiceCatalogItem(id = "complex-service-id", name = "Parking", scope = scope))
        ServiceScope.COURT ->
            listOf(
                ServiceCatalogItem(
                    id = "court-service-id",
                    name = "Synthetic Grass",
                    scope = scope,
                )
            )
      }

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
      error("Unused in this test")

  override suspend fun addCourt(
      complexId: String,
      request: CreateCourtRequest,
  ): CreatedCourt = error("Unused in this test")

  override suspend fun getMyComplexHub(): MyComplexHub = error("Unused in this test")
}

private fun localCourtImage(fileName: String) =
    LocalCourtImage(
        fileName = fileName,
        contentType = "image/png",
        bytes = byteArrayOf(1, 2, 3),
        previewUrl = "content://$fileName",
    )

private class FakeCourtDetailRepository : ICourtDetailRepository {
  override suspend fun getUpcomingReservableSlotsPreview(
      courtId: String,
  ): ReservationDayAvailability =
      ReservationDayAvailability(
          dateUtc = "2026-07-01",
          availabilityStatus = ReservationAvailabilityStatus.Available,
          slots =
              listOf(
                  ReservableSlot(
                      startsAtUtc = "2026-07-01T14:00:00.000Z",
                      endsAtUtc = "2026-07-01T15:00:00.000Z",
                  ),
                  ReservableSlot(
                      startsAtUtc = "2026-07-01T15:00:00.000Z",
                      endsAtUtc = "2026-07-01T16:00:00.000Z",
                  ),
              ),
      )
}

private class FakeReservationRepository(
    private val reservableDays: List<ReservableDay>? = null,
    private var discoveryFailuresRemaining: Int = 0,
    private val discoveryBlock: (suspend (String, Int) -> ReservationDayDiscovery)? = null,
) : IReservationRepository {
  val discoveryRequests = mutableListOf<Pair<String, Int>>()
  val requestedSlotDates = mutableListOf<String>()

  override suspend fun getReservableDays(
      courtId: String,
      fromUtcDate: String,
      days: Int,
  ): ReservationDayDiscovery {
    discoveryRequests += fromUtcDate to days
    if (discoveryFailuresRemaining > 0) {
      discoveryFailuresRemaining -= 1
      throw AppApiException(500, "Discovery failed")
    }
    return discoveryBlock?.invoke(fromUtcDate, days)
        ?: ReservationDayDiscovery(
            fromUtc = fromUtcDate,
            days = days,
            reservableDays =
                reservableDays
                    ?: listOf(
                        ReservableDay(
                            dateUtc = fromUtcDate,
                            availabilityStatus = ReservationAvailabilityStatus.Available,
                            availableSlotsCount = 1,
                        )
                    ),
        )
  }

  override suspend fun getReservableSlots(
      courtId: String,
      dateUtc: String,
  ): ReservationDayAvailability {
    requestedSlotDates += dateUtc
    return ReservationDayAvailability(
        dateUtc = dateUtc,
        availabilityStatus = ReservationAvailabilityStatus.Available,
        slots =
            listOf(
                ReservableSlot(
                    startsAtUtc = "${dateUtc}T21:00:00.000Z",
                    endsAtUtc = "${dateUtc}T22:00:00.000Z",
                )
            ),
    )
  }

  override suspend fun createReservation(
      courtId: String,
      startsAtUtc: String,
  ): ReservationConfirmation =
      ReservationConfirmation(
          id = "reservation-id",
          courtId = courtId,
          startsAtUtc = startsAtUtc,
          endsAtUtc = startsAtUtc.replace("T18:00:00.000Z", "T19:00:00.000Z"),
          status = "CONFIRMED",
      )

  override suspend fun getMyReservations(): MyReservations =
      MyReservations(emptyList(), emptyList())
}
