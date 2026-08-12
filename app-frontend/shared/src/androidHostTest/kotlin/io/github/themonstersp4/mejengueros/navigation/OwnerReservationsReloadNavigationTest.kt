package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCard
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

/**
 * Regression coverage for the owner variant of the shared Reservations tab. An owner can book one
 * of their own courts from the mejenguero shell; the retained OwnerReservationsViewModel only
 * fetches in its init block, so the owner list used to stay stale until the court filter forced a
 * refetch. The reservation-created reload key must drive a refresh instead.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerReservationsReloadNavigationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun reservationCreatedFromPlayerShellRefreshesOwnerReservations() = runTest {
    val navigationState = ownerNavigationState()
    val reservationRepository =
        SequencedOwnerReservationRepository(
            listOf(
                emptyOwnerReservations(),
                ownerReservationsWithUpcomingCard(),
            )
        )
    val ownerReservationsViewModel = createOwnerReservationsViewModel(this, reservationRepository)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        OwnerReservationsReloadTestHost(
            navigationState = navigationState,
            ownerReservationsViewModel = ownerReservationsViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(OwnerReservationCardTag).assertDoesNotExist()

    // The mejenguero shell signals the booking through the shared reload key.
    composeRule.runOnIdle { navigationState.notifyReservationCreated() }
    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(OwnerReservationCardTag).assertExists()
    assertEquals(2, reservationRepository.getOwnerReservationsCalls)
  }

  @Test
  fun ownerReservationsAreNotRefetchedWhileNoReservationIsCreated() = runTest {
    val navigationState = ownerNavigationState()
    val reservationRepository =
        SequencedOwnerReservationRepository(listOf(ownerReservationsWithUpcomingCard()))
    val ownerReservationsViewModel = createOwnerReservationsViewModel(this, reservationRepository)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        OwnerReservationsReloadTestHost(
            navigationState = navigationState,
            ownerReservationsViewModel = ownerReservationsViewModel,
        )
      }
    }

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(OwnerReservationCardTag).assertExists()
    assertEquals(1, reservationRepository.getOwnerReservationsCalls)
  }

  @Composable
  private fun OwnerReservationsReloadTestHost(
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
              reservationsReloadRequestKey = navigationState.reservationsReloadRequestKey,
          )
      else -> Text("Ruta inesperada")
    }
  }

  private fun createOwnerReservationsViewModel(
      coroutineScope: TestScope,
      reservationRepository: IReservationRepository,
  ): OwnerReservationsViewModel =
      OwnerReservationsViewModel(
          reservationRepository = reservationRepository,
          complexRepository = EmptyComplexRepository(),
          coroutineScope = coroutineScope,
      )

  private class SequencedOwnerReservationRepository(
      private val results: List<OwnerReservations>,
  ) : IReservationRepository {
    var getOwnerReservationsCalls: Int = 0

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

    override suspend fun getOwnerReservations(courtId: String?): OwnerReservations {
      val result = results.getOrElse(getOwnerReservationsCalls) { results.last() }
      getOwnerReservationsCalls += 1
      return result
    }
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
          catalogCourtDetailReloadRequestKeyState = mutableStateOf(0),
          reservationsReloadRequestKeyState = mutableStateOf(0),
          viewingAsPlayerState = mutableStateOf(false),
          hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
      )
}

private const val OwnerReservationId = "owner-reservation-id"
private const val OwnerReservationCardTag = "owner_reservation_card_$OwnerReservationId"

private fun emptyOwnerReservations(): OwnerReservations =
    OwnerReservations(selectedCourtId = null, upcoming = emptyList(), finalized = emptyList())

private fun ownerReservationsWithUpcomingCard(): OwnerReservations =
    OwnerReservations(
        selectedCourtId = null,
        upcoming =
            listOf(
                OwnerReservationCard(
                    id = OwnerReservationId,
                    complexName = "Moravia FC",
                    courtName = "Cancha A",
                    startsAt = "2026-07-10T18:00:00.000Z",
                    endsAt = "2026-07-10T19:00:00.000Z",
                    status = "CONFIRMED",
                    section = "UPCOMING",
                )
            ),
        finalized = emptyList(),
    )
