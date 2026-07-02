package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
    composeRule
        .onNodeWithTag("complex_detail_add_court_button_complex-id")
        .assertExists()
        .performClick()
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
}

@Composable
private fun AddCourtNavigationTestHost(
    navigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
    viewModel: AddCourtViewModel,
    onMyComplexReloadRequested: () -> Unit,
) {
  MejenguerosTheme {
    var myComplexState by remember {
      mutableStateOf(
          MyComplexUiState(
              complexes =
                  listOf(
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
                  )
          )
      )
    }

    MyComplexHubReloadEffect(
        reloadRequestKey = navigationState.myComplexHubReloadRequestKey,
        onReloadRequested = {
          onMyComplexReloadRequested()
          myComplexState =
              myComplexState.copy(
                  complexes =
                      myComplexState.complexes.map { complex ->
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
      MyComplexRoute ->
          MyComplexEntryContent(
              state = myComplexState,
              contentPadding = PaddingValues(),
              onCreateComplex = {},
              onRetry = {},
              onOpenComplexDetail = shellActions.openComplexDetail,
          )
      is ComplexDetailRoute ->
          ComplexDetailEntryContent(
              complex = myComplexState.complexes.firstOrNull { it.id == route.complexId },
              isLoading = false,
              errorMessage = null,
              contentPadding = PaddingValues(),
              onRetry = {},
              onAddCourt = shellActions.openAddCourt,
              onConfigureAvailability = {},
          )
      is AddCourtRoute -> AddCourtEntryContent(viewModel = viewModel, shellActions = shellActions)
      else -> error("Unexpected route $route")
    }
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
        refreshOwnerRole = {},
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
        viewingAsPlayerState = mutableStateOf(false),
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
