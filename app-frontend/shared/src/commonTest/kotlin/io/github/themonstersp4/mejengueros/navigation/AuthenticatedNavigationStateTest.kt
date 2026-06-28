package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthenticatedNavigationStateTest {

  private enum class LegacySelectedRoute {
    Home,
    MyComplex,
  }

  @Test
  fun restoreNormalizationMapsLegacyHomeSelectionToSearchLanding() {
    val normalized =
        normalizeRestoredAuthenticatedNavigation(
            savedSelectedRouteName = "Home",
            searchStack = listOf(HomeRoute),
            reservationsStack = listOf(ReservationsRoute),
            notificationsStack = listOf(NotificationsRoute),
            myComplexStack = listOf(MyComplexRoute),
        )

    assertEquals(AuthenticatedTopLevelRoute.Search, normalized.selectedRoute)
    assertEquals(listOf(SearchRoute), normalized.searchStack)
  }

  @Test
  fun restoreNormalizationMovesLegacyOwnerFlowFromHomeStackIntoMyComplex() {
    val availabilityRoute = CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")

    val normalized =
        normalizeRestoredAuthenticatedNavigation(
            savedSelectedRouteName = "Home",
            searchStack = listOf(HomeRoute, availabilityRoute),
            reservationsStack = listOf(ReservationsRoute),
            notificationsStack = listOf(NotificationsRoute),
            myComplexStack = listOf(MyComplexRoute),
        )

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, normalized.selectedRoute)
    assertEquals(listOf(SearchRoute), normalized.searchStack)
    assertEquals(listOf(MyComplexRoute, availabilityRoute), normalized.myComplexStack)
  }

  @Test
  fun restoreSavedSelectedRouteNameAcceptsLegacyEnumValues() {
    assertEquals("Home", restoreSavedAuthenticatedTopLevelRouteName(LegacySelectedRoute.Home))
    assertEquals(
        "MyComplex",
        restoreSavedAuthenticatedTopLevelRouteName(LegacySelectedRoute.MyComplex),
    )
  }

  @Test
  fun restoreSavedSelectedRouteNameKeepsCurrentEnumValuesCompatible() {
    assertEquals(
        AuthenticatedTopLevelRoute.Reservations.name,
        restoreSavedAuthenticatedTopLevelRouteName(AuthenticatedTopLevelRoute.Reservations),
    )
  }

  @Test
  fun restoreNormalizationMovesLegacyCreateComplexFlowFromHomeStackIntoMyComplex() {
    val normalized =
        normalizeRestoredAuthenticatedNavigation(
            savedSelectedRouteName = "Home",
            searchStack = listOf(HomeRoute, CreateComplexRoute),
            reservationsStack = listOf(ReservationsRoute),
            notificationsStack = listOf(NotificationsRoute),
            myComplexStack = listOf(MyComplexRoute),
        )

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, normalized.selectedRoute)
    assertEquals(listOf(SearchRoute), normalized.searchStack)
    assertEquals(listOf(MyComplexRoute, CreateComplexRoute), normalized.myComplexStack)
  }

  @Test
  fun restoreNormalizationResetsLegacyKitAndPokedexStacksToCurrentRoots() {
    val normalized =
        normalizeRestoredAuthenticatedNavigation(
            savedSelectedRouteName = "Kit",
            searchStack = listOf(SearchRoute),
            reservationsStack = listOf(KitRoute, AvailabilitySelectorsRoute),
            notificationsStack = listOf(PokedexRoute, PokemonDetailRoute(25)),
            myComplexStack = listOf(MyComplexRoute),
        )

    assertEquals(AuthenticatedTopLevelRoute.Search, normalized.selectedRoute)
    assertEquals(listOf(ReservationsRoute), normalized.reservationsStack)
    assertEquals(listOf(NotificationsRoute), normalized.notificationsStack)
  }

  @Test
  fun startsAtSearchStack() {
    val state = testNavigationState()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun selectingReservationsShowsReservationsRoot() {
    val state = testNavigationState()

    state.selectReservations()

    assertEquals(AuthenticatedTopLevelRoute.Reservations, state.selectedRoute)
    assertEquals(listOf(ReservationsRoute), state.currentBackStack.toList())
  }

  @Test
  fun selectingNotificationsShowsNotificationsRoot() {
    val state = testNavigationState()

    state.selectNotifications()

    assertEquals(AuthenticatedTopLevelRoute.Notifications, state.selectedRoute)
    assertEquals(listOf(NotificationsRoute), state.currentBackStack.toList())
  }

  @Test
  fun openCreateComplexKeepsMyComplexSelectedAndAppendsDetailRoute() {
    val state = testNavigationState()

    state.openCreateComplex()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute, CreateComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun returnToMyComplexRootClearsMyComplexDetailStack() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.returnToMyComplexRoot()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun switchingTopLevelRoutesPreservesMyComplexDetailStack() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.selectSearch()
    state.selectMyComplex()

    assertEquals(listOf(MyComplexRoute, CreateComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun switchingTopLevelRoutesPreservesSearchSelection() {
    val state = testNavigationState()

    state.selectReservations()
    state.selectSearch()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun openCourtAvailabilityReplacesCreateComplexDetailInsideMyComplexFlow() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(
        listOf(MyComplexRoute, CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun openCourtAvailabilityStoresOwnerAvailabilityEntrypointContext() {
    val state = testNavigationState()

    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )

    assertEquals(
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        ),
        state.ownerCourtAvailabilityEntrypoint,
    )
  }

  @Test
  fun reopenOwnerCourtAvailabilityUsesStoredContextFromMyComplexRoot() {
    val state = testNavigationState()

    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )
    state.returnToMyComplexRoot()
    state.openOwnerCourtAvailabilityEntrypoint()

    assertEquals(
        listOf(MyComplexRoute, CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun returnToMyComplexRootClosesAvailabilityDetailAndKeepsMyComplexSelected() {
    val state = testNavigationState()

    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )
    state.returnToMyComplexRoot()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun closeCurrentDetailReturnsToMyComplexRoot() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.closeCurrentDetail()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun closeCurrentDetailDoesNotRemoveRootRoute() {
    val state = testNavigationState()

    state.selectNotifications()
    state.closeCurrentDetail()

    assertEquals(listOf(NotificationsRoute), state.currentBackStack.toList())
  }

  @Test
  fun resetRestoresAuthenticatedRootStacks() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.selectReservations()
    state.reset()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())

    state.selectReservations()
    assertEquals(listOf(ReservationsRoute), state.currentBackStack.toList())

    state.selectNotifications()
    assertEquals(listOf(NotificationsRoute), state.currentBackStack.toList())

    state.selectMyComplex()
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun resetClearsOwnerAvailabilityEntrypoint() {
    val state = testNavigationState()

    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )

    state.reset()

    assertEquals(null, state.ownerCourtAvailabilityEntrypoint)
  }

  private fun testNavigationState(): AuthenticatedNavigationState =
      AuthenticatedNavigationState(
          selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
          searchBackStack = NavBackStack<NavKey>(SearchRoute),
          reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
          notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
          myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
          ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
      )
}
