package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.presentation.ownerreservations.OwnerReservationsViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationsDrawerNavigationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun ownerReservationsDrawerReviewsItemOpensReceivedReviews() = runTest {
    val navigationState = ownerNavigationState()
    val ownerReservationsViewModel = createOwnerReservationsViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        OwnerReservationsDrawerTestHost(
            navigationState = navigationState,
            ownerReservationsViewModel = ownerReservationsViewModel,
        )
      }
    }

    // Regression: from the owner's reservations screen, opening the drawer and tapping
    // "Reseñas" must reach the received-reviews screen. The bug left the callback as a
    // no-op because OwnerReservationsEntryContent did not forward openOwnerReceivedReviews.
    composeRule.onNodeWithContentDescription("Abrir menú").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Reseñas").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("RECEIVED_REVIEWS_REACHED").assertExists()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.MyComplex, navigationState.selectedRoute)
      assertEquals(OwnerReceivedReviewsRoute, navigationState.currentBackStack.last())
    }
  }

  @Composable
  private fun OwnerReservationsDrawerTestHost(
      navigationState: AuthenticatedNavigationState,
      ownerReservationsViewModel: OwnerReservationsViewModel,
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
            openOwnerReceivedReviews = navigationState::openOwnerReceivedReviews,
            closeAddCourtAfterSuccess = navigationState::closeAddCourtAfterSuccess,
            closeCurrentDetail = navigationState::closeCurrentDetail,
            signOut = {},
            refreshOwnerRole = {},
            isOwner = true,
            viewingAsPlayer = false,
        )

    when (navigationState.selectedRoute) {
      AuthenticatedTopLevelRoute.Reservations ->
          OwnerReservationsEntryContent(
              shellActions = shellActions,
              ownerReservationsViewModel = ownerReservationsViewModel,
          )
      AuthenticatedTopLevelRoute.MyComplex ->
          if (navigationState.currentBackStack.last() == OwnerReceivedReviewsRoute) {
            Text("RECEIVED_REVIEWS_REACHED")
          } else {
            Text("MY_COMPLEX_ROOT")
          }
      else -> Text("Ruta inesperada")
    }
  }

  private fun createOwnerReservationsViewModel(
      coroutineScope: TestScope,
  ): OwnerReservationsViewModel =
      OwnerReservationsViewModel(
          reservationRepository = EmptyOwnerReservationRepository(),
          complexRepository = EmptyComplexRepository(),
          coroutineScope = coroutineScope,
      )

  private class EmptyOwnerReservationRepository : IReservationRepository {
    override suspend fun getReservableDays(
        courtId: String,
        fromUtcDate: String,
        days: Int,
    ): ReservationDayDiscovery = error("Unused in test")

    override suspend fun getReservableSlots(
        courtId: String,
        dateUtc: String,
    ): ReservationDayAvailability = error("Unused in test")

    override suspend fun createReservation(
        courtId: String,
        startsAtUtc: String,
    ): ReservationConfirmation = error("Unused in test")

    override suspend fun getMyReservations(): MyReservations = error("Unused in test")

    override suspend fun getOwnerReservations(courtId: String?): OwnerReservations =
        OwnerReservations(selectedCourtId = null, upcoming = emptyList(), finalized = emptyList())
  }

  private class EmptyComplexRepository : IComplexRepository {
    override suspend fun getMyComplexHub(): MyComplexHub = MyComplexHub(complexes = emptyList())

    override suspend fun getProvinces() = error("Unused in test")

    override suspend fun getCantons(provinceId: String) = error("Unused in test")

    override suspend fun getServices(scope: ServiceScope) = error("Unused in test")

    override suspend fun createComplex(request: CreateComplexRequest) = error("Unused in test")

    override suspend fun addCourt(complexId: String, request: CreateCourtRequest) =
        error("Unused in test")

    override suspend fun updateCourtImage(
        complexId: String,
        courtId: String,
        imageUploadId: String,
    ) = error("Unused in test")
  }

  private fun ownerNavigationState(): AuthenticatedNavigationState =
      AuthenticatedNavigationState(
          selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Reservations),
          searchBackStack = NavBackStack<NavKey>(SearchRoute),
          reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
          notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
          myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
          ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
          myComplexHubReloadRequestKeyState = mutableStateOf(0),
          catalogReloadRequestKeyState = mutableStateOf(0),
          reservationsReloadRequestKeyState = mutableStateOf(0),
          viewingAsPlayerState = mutableStateOf(false),
          hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
      )
}
