package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CourtAvailabilityNavigationIntegrationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun confirmingAvailabilitySuccessReturnsToMyComplexAndClearsDialogState() = runTest {
    val coroutineScope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val navigationState =
        testNavigationState().apply {
          openCourtAvailability(
              courtId = "court-id",
              courtName = "Cancha 1",
              complexName = "Mejengas CR",
          )
        }
    val shellActions = shellActions(navigationState)
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = SuccessfulCourtAvailabilityRepository(),
            coroutineScope = coroutineScope,
        )

    advanceUntilIdle()
    viewModel.toggleDay(CourtAvailabilityWeekday.MONDAY)
    viewModel.updateStartTime("07:00")
    viewModel.updateEndTime("09:00")
    viewModel.save()
    advanceUntilIdle()

    composeRule.setContent {
      AvailabilityNavigationTestHost(
          navigationState = navigationState,
          viewModel = viewModel,
          shellActions = shellActions,
      )
    }

    composeRule.onNodeWithText("Disponibilidad configurada").assertExists()
    composeRule.onNodeWithTag("mejengueros_confirmation_dialog_confirm").performClick()

    composeRule.onNodeWithText("Mi complejo").assertExists()
    composeRule.onNodeWithTag("mejengueros_confirmation_dialog").assertDoesNotExist()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.MyComplex, navigationState.selectedRoute)
      assertEquals(listOf(MyComplexRoute), navigationState.currentBackStack.toList())
      assertNull(viewModel.uiState.value.successMessage)
    }

    coroutineScope.cancel()
  }
}

@Composable
private fun AvailabilityNavigationTestHost(
    navigationState: AuthenticatedNavigationState,
    viewModel: CourtAvailabilityViewModel,
    shellActions: AuthenticatedShellActions,
) {
  MejenguerosTheme {
    when (navigationState.currentBackStack.lastOrNull()) {
      is CourtAvailabilityRoute ->
          CourtAvailabilityEntryContent(viewModel = viewModel, shellActions = shellActions)
      MyComplexRoute -> Text("Mi complejo")
      else -> Text("Ruta inesperada")
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
        openCreateComplex = navigationState::openCreateComplex,
        openCourtAvailability = navigationState::openCourtAvailability,
        closeCurrentDetail = navigationState::closeCurrentDetail,
        signOut = {},
    )

private fun testNavigationState(): AuthenticatedNavigationState =
    AuthenticatedNavigationState(
        selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
        searchBackStack = NavBackStack<NavKey>(SearchRoute),
        reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
        notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
        myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
        ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
    )

private class SuccessfulCourtAvailabilityRepository : ICourtAvailabilityRepository {
  private var currentContext =
      CourtAvailabilityContext(
          courtId = "court-id",
          courtName = "Cancha 1",
          complexName = "Mejengas CR",
          availability = null,
      )

  override suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext =
      currentContext

  override suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext {
    currentContext = currentContext.copy(availability = availability)
    return currentContext
  }
}
