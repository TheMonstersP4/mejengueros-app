package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtViewModel
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
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
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AddCourtNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun addCourtFlowNavigatesFromListToDetailToAddCourtAndShowsPendingCourtAfterReload() = runTest {
    var myComplexReloadRequests = 0
    val navigationState = testNavigationState()
    val shellActions = shellActions(navigationState)
    val repository = SuccessfulAddCourtRepository()
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions,
          viewModel = viewModel,
          onMyComplexReloadRequested = { myComplexReloadRequests += 1 },
      )
    }

    composeRule.onNodeWithTag("my_complex_list_item_complex-id").assertExists().performClick()
    composeRule.onAllNodesWithContentDescription("Agregar cancha").assertCountEquals(1)
    composeRule.onNodeWithContentDescription("Agregar cancha").assertExists().performClick()
    composeRule.onNodeWithTag("add_court_root").assertExists()
    composeRule.inputField("Nombre de la cancha").performTextInput("Court B")
    composeRule.onNodeWithTag("add_court_service_court-service-id").performScrollTo().performClick()
    composeRule.onNodeWithTag("add_court_submit_button").performScrollTo().performClick()
    advanceUntilIdle()

    composeRule.onNodeWithTag("complex_detail_root").assertExists()
    composeRule.onNodeWithText("Court B").assertExists()
    composeRule.onNodeWithText("Pendiente").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          listOf(MyComplexRoute, ComplexDetailRoute("complex-id")),
          navigationState.currentBackStack.toList(),
      )
      assertEquals(1, myComplexReloadRequests)
      assertEquals(
          CreateCourtRequest(name = "Court B", serviceIds = listOf("court-service-id")),
          repository.request,
      )
    }
  }

  @Test
  fun addCourtSubmitFailureStaysOnScreenAndShowsVisibleError() = runTest {
    val navigationState =
        testNavigationState().apply {
          openComplexDetail("complex-id")
          openAddCourt("complex-id", "North Sports Center")
        }
    val shellActions = shellActions(navigationState)
    val repository = FailingAddCourtRepository()
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions,
          viewModel = viewModel,
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.inputField("Nombre de la cancha").performTextInput("Court B")
    composeRule.onNodeWithTag("add_court_service_court-service-id").performScrollTo().performClick()
    composeRule.onNodeWithTag("add_court_submit_button").performScrollTo().performClick()
    advanceUntilIdle()

    composeRule.onNodeWithText("No pudimos guardar la cancha. Intentá de nuevo.").assertExists()
    composeRule.onNodeWithTag("add_court_root").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          AddCourtRoute("complex-id", "North Sports Center"),
          navigationState.currentBackStack.lastOrNull(),
      )
    }
  }

  @Test
  fun addCourtRouteShowsImagePickerCtaThroughAndroidEntryWiring() = runTest {
    val navigationState =
        testNavigationState().apply {
          openComplexDetail("complex-id")
          openAddCourt("complex-id", "North Sports Center")
        }
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = SuccessfulAddCourtRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = viewModel,
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.onNodeWithTag("add_court_root").assertExists()
    composeRule
        .onNodeWithText("Opcional. Podés agregar una imagen ahora o dejarla para más adelante.")
        .assertExists()
    composeRule.onNodeWithTag("add_court_pick_image_button").assertExists()
    composeRule.onNodeWithText("Seleccionar imagen").assertExists()
  }

  @Test
  fun addCourtRouteHidesImagePickerCtaWhenPickerSeamReportsUnavailable() = runTest {
    val navigationState =
        testNavigationState().apply {
          openComplexDetail("complex-id")
          openAddCourt("complex-id", "North Sports Center")
        }
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = SuccessfulAddCourtRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    advanceUntilIdle()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = viewModel,
          onMyComplexReloadRequested = {},
          courtImagePickerController = CourtImagePickerController(isAvailable = false, launch = {}),
      )
    }

    composeRule.onNodeWithTag("add_court_root").assertExists()
    composeRule.onNodeWithTag("add_court_pick_image_button").assertDoesNotExist()
    composeRule
        .onNodeWithText("Opcional. Podés agregar una imagen ahora o dejarla para más adelante.")
        .assertDoesNotExist()
  }

  @Test
  fun addCourtRouteUpdatesPreviewWhenInjectedPickerLaunchReturnsImage() = runTest {
    val navigationState =
        testNavigationState().apply {
          openComplexDetail("complex-id")
          openAddCourt("complex-id", "North Sports Center")
        }
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = SuccessfulAddCourtRepository(),
            coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        )
    val pickedImage = localCourtImage(fileName = "route-picked-court.png")
    advanceUntilIdle()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = viewModel,
          onMyComplexReloadRequested = {},
          courtImagePickerController =
              CourtImagePickerController(
                  isAvailable = true,
                  launch = { viewModel.updateSelectedCourtImage(pickedImage) },
              ),
      )
    }

    composeRule.onNodeWithTag("add_court_pick_image_button").performScrollTo().performClick()

    composeRule.onNodeWithTag("add_court_image_preview").assertExists()
    composeRule.onNodeWithText("route-picked-court.png").assertExists()
    composeRule.onNodeWithText("Cambiar imagen").assertExists()
    composeRule.onNodeWithTag("add_court_clear_image_button").assertExists()
  }

  @Test
  fun complexDetailRouteHidesAddCourtActionWhileLoading() {
    val navigationState = testNavigationState().apply { openComplexDetail("complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = unusedAddCourtViewModel(),
          myComplexState = MyComplexUiState(isLoading = true, complexes = listOf(defaultComplex())),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("complex_detail_root").assertExists()
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").assertDoesNotExist()
  }

  @Test
  fun complexDetailRouteHidesAddCourtActionWhileShowingError() {
    val navigationState = testNavigationState().apply { openComplexDetail("complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = unusedAddCourtViewModel(),
          myComplexState =
              MyComplexUiState(
                  complexes = listOf(defaultComplex()),
                  errorMessage = "No pudimos cargar tu hub de complejos. Intentá de nuevo.",
              ),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("complex_detail_root").assertExists()
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").assertDoesNotExist()
  }

  @Test
  fun complexDetailRouteHidesAddCourtActionWhenComplexIsMissing() {
    val navigationState = testNavigationState().apply { openComplexDetail("missing-complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState),
          viewModel = unusedAddCourtViewModel(),
          myComplexState = MyComplexUiState(complexes = listOf(defaultComplex())),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.onNodeWithText("No encontramos el complejo seleccionado.").assertExists()
    composeRule.onNodeWithContentDescription("Agregar cancha").assertDoesNotExist()
  }

  @Test
  fun complexDetailRouteHidesAddCourtActionInOwnerPlayerShell() {
    val navigationState = testNavigationState().apply { openComplexDetail("complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState, isOwner = true, viewingAsPlayer = true),
          viewModel = unusedAddCourtViewModel(),
          myComplexState = MyComplexUiState(complexes = listOf(defaultComplex())),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("complex_detail_root").assertExists()
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").assertDoesNotExist()
  }

  @Test
  fun complexDetailRouteShowsAddCourtActionOnlyForOwnerShellWithCleanState() {
    val navigationState = testNavigationState().apply { openComplexDetail("complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState, isOwner = true, viewingAsPlayer = false),
          viewModel = unusedAddCourtViewModel(),
          myComplexState = MyComplexUiState(complexes = listOf(defaultComplex())),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("complex_detail_root").assertExists()
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").assertExists()
  }

  @Test
  fun nonOwnerRestoredMyComplexRouteRedirectsToSearchWithoutRenderingOwnerHub() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState, isOwner = false),
          viewModel = unusedAddCourtViewModel(),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithTag("my_complex_root").assertDoesNotExist()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }
  }

  @Test
  fun nonOwnerRestoredComplexDetailRouteRedirectsToSearchWithoutRenderingOwnerDetail() {
    val navigationState = testNavigationState().apply { openComplexDetail("complex-id") }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState, isOwner = false),
          viewModel = unusedAddCourtViewModel(),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithTag("complex_detail_root").assertDoesNotExist()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }
  }

  @Test
  fun nonOwnerRestoredAddCourtRouteRedirectsToSearchWithoutRenderingOwnerScreen() {
    val navigationState =
        testNavigationState().apply {
          openComplexDetail("complex-id")
          openAddCourt("complex-id", "North Sports Center")
        }

    composeRule.setContent {
      AddCourtNavigationTestHost(
          navigationState = navigationState,
          shellActions = shellActions(navigationState, isOwner = false),
          viewModel = unusedAddCourtViewModel(),
          onMyComplexReloadRequested = {},
      )
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Buscar").assertExists()
    composeRule.onNodeWithTag("add_court_root").assertDoesNotExist()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }
  }
}

@Composable
private fun AddCourtNavigationTestHost(
    navigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
    viewModel: AddCourtViewModel,
    myComplexState: MyComplexUiState = MyComplexUiState(complexes = listOf(defaultComplex())),
    onMyComplexReloadRequested: () -> Unit,
    courtImagePickerController: CourtImagePickerController? = null,
) {
  MejenguerosTheme {
    var currentMyComplexState by remember { mutableStateOf(myComplexState) }

    MyComplexHubReloadEffect(
        reloadRequestKey = navigationState.myComplexHubReloadRequestKey,
        onReloadRequested = {
          onMyComplexReloadRequested()
          currentMyComplexState =
              currentMyComplexState.copy(
                  complexes =
                      currentMyComplexState.complexes.map { complex ->
                        if (complex.id != "complex-id") {
                          complex
                        } else {
                          complex.copy(
                              courts =
                                  listOf(
                                      MyComplexHubCourt(
                                          id = "court-new-id",
                                          name = "Court B",
                                          status = "ACTIVE",
                                          availabilityStatus = CourtAvailabilitySetupStatus.PENDING,
                                      )
                                  )
                          )
                        }
                      }
              )
        },
    )

    when (val route = navigationState.currentBackStack.lastOrNull()) {
      SearchRoute -> Text("Buscar")
      MyComplexRoute ->
          MyComplexRouteContent(
              state = currentMyComplexState,
              shellActions = shellActions,
              onRetry = {},
          )
      is ComplexDetailRoute ->
          ComplexDetailRouteContent(
              route = route,
              state = currentMyComplexState,
              shellActions = shellActions,
              onRetry = {},
          )
      is AddCourtRoute ->
          AddCourtEntryContent(
              viewModel = viewModel,
              shellActions = shellActions,
              courtImagePickerController = courtImagePickerController,
          )
      else -> error("Unexpected route $route")
    }
  }
}

private fun shellActions(
    navigationState: AuthenticatedNavigationState,
    isOwner: Boolean = true,
    viewingAsPlayer: Boolean = false,
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
        refreshOwnerRole = {},
        isOwner = isOwner,
        viewingAsPlayer = viewingAsPlayer,
    )

private fun testNavigationState(): AuthenticatedNavigationState =
    AuthenticatedNavigationState(
        selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.MyComplex),
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

private fun defaultComplex() =
    MyComplexHubComplex(
        id = "complex-id",
        name = "North Sports Center",
        address = "123 Main Street",
        provinceId = "province-id",
        cantonId = "canton-id",
        latitude = 9.935,
        longitude = -84.091,
        status = "ACTIVE",
        courts = emptyList(),
    )

private fun localCourtImage(fileName: String) =
    LocalCourtImage(
        fileName = fileName,
        contentType = "image/png",
        bytes = byteArrayOf(1, 2, 3),
        previewUrl = "content://$fileName",
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun unusedAddCourtViewModel() =
    AddCourtViewModel(
        complexId = "complex-id",
        complexName = "North Sports Center",
        repository = SuccessfulAddCourtRepository(),
        coroutineScope = TestScope(UnconfinedTestDispatcher()),
    )

private class SuccessfulAddCourtRepository : IComplexRepository {
  var request: CreateCourtRequest? = null

  override suspend fun getProvinces(): List<Province> = emptyList()

  override suspend fun getCantons(provinceId: String): List<Canton> = emptyList()

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> =
      listOf(ServiceCatalogItem(id = "court-service-id", name = "Lighting", scope = scope))

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
      error("Unused in this test")

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt {
    this.request = request
    return CreatedCourt(id = "court-id", complexId = complexId, name = request.name)
  }

  override suspend fun getMyComplexHub(): MyComplexHub = error("Unused in this test")
}

private class FailingAddCourtRepository : IComplexRepository {
  override suspend fun getProvinces(): List<Province> = emptyList()

  override suspend fun getCantons(provinceId: String): List<Canton> = emptyList()

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> =
      listOf(ServiceCatalogItem(id = "court-service-id", name = "Lighting", scope = scope))

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
      error("Unused in this test")

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt {
    throw AppApiException(statusCode = 500, message = "Backend exploded")
  }

  override suspend fun getMyComplexHub(): MyComplexHub = error("Unused in this test")
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.inputField(label: String) =
    onNode(hasSetTextAction() and hasContentDescription(label))
