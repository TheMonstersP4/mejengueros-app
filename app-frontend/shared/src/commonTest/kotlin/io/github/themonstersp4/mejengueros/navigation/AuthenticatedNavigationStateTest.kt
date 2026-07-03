package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.data.auth.AuthSecureStorageWriteException
import io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
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
  fun restoreSavedSelectedRouteNameAcceptsLegacyObjectRouteValues() {
    assertEquals("Home", restoreSavedAuthenticatedTopLevelRouteName(HomeRoute))
    assertEquals("Kit", restoreSavedAuthenticatedTopLevelRouteName(KitRoute))
    assertEquals("Kit", restoreSavedAuthenticatedTopLevelRouteName(AvailabilitySelectorsRoute))
    assertEquals("Pokedex", restoreSavedAuthenticatedTopLevelRouteName(PokedexRoute))
    assertEquals("Pokedex", restoreSavedAuthenticatedTopLevelRouteName(PokemonDetailRoute(25)))
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
  fun restoreNormalizationMovesLegacyOwnerFlowFromHomeObjectSelectionIntoMyComplex() {
    val availabilityRoute = CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")

    val normalized =
        normalizeRestoredAuthenticatedNavigation(
            savedSelectedRouteName = restoreSavedAuthenticatedTopLevelRouteName(HomeRoute),
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
    state.selectMyComplex()

    state.openCreateComplex()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute, CreateComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun openCreateComplexFromCatalogReturnsToCatalogOnBack() {
    val state = testNavigationState()

    state.openCreateComplex()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute, CreateComplexRoute), state.currentBackStack.toList())

    state.closeCurrentDetail()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun openCatalogCourtDetailKeepsSearchSelectedAndAppendsDetailRoute() {
    val state = testNavigationState()

    state.openCatalogCourtDetail(
        CatalogCourtDetailRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )
    )

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(
        listOf(
            SearchRoute,
            CatalogCourtDetailRoute(
                courtId = "court-id",
                complexId = "complex-id",
                complexName = "Mejengas CR",
                courtName = "Cancha 1",
            ),
        ),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun openCatalogReservationKeepsSearchSelectedAndAppendsReservationRoute() {
    val state = testNavigationState()

    state.openCatalogReservation(
        CatalogReservationRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )
    )

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(
        listOf(
            SearchRoute,
            CatalogCourtDetailRoute(
                courtId = "court-id",
                complexId = "complex-id",
                complexName = "Mejengas CR",
                courtName = "Cancha 1",
            ),
            CatalogReservationRoute(
                courtId = "court-id",
                complexId = "complex-id",
                complexName = "Mejengas CR",
                courtName = "Cancha 1",
            ),
        ),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun returnToSearchRootClearsReservationFlowBackToCatalogRoot() {
    val state = testNavigationState()

    state.openCatalogReservation(
        CatalogReservationRoute(
            courtId = "court-id",
            complexId = "complex-id",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )
    )

    state.returnToSearchRoot()

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun openComplexDetailKeepsMyComplexSelectedAndAppendsDetailRoute() {
    val state = testNavigationState()

    state.openComplexDetail("complex-id")

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(
        listOf(MyComplexRoute, ComplexDetailRoute("complex-id")),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun openAddCourtKeepsComplexDetailAndAppendsAddCourtRoute() {
    val state = testNavigationState()

    state.openComplexDetail("complex-id")
    state.openAddCourt("complex-id", "North Sports Center")

    assertEquals(
        listOf(
            MyComplexRoute,
            ComplexDetailRoute("complex-id"),
            AddCourtRoute("complex-id", "North Sports Center"),
        ),
        state.currentBackStack.toList(),
    )
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
    state.selectMyComplex()

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
    state.selectMyComplex()

    state.openCreateComplex()
    state.openCourtAvailability(
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
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
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
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
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
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
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
    )
    state.returnToMyComplexRoot()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun returnToMyComplexRootRequestsMyComplexHubReload() {
    val state = testNavigationState()

    state.openCourtAvailability(
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
    )
    state.returnToMyComplexRoot()

    assertEquals(1, state.myComplexHubReloadRequestKey)
  }

  @Test
  fun closeCurrentDetailRequestsMyComplexHubReloadWhenLeavingAvailability() {
    val state = testNavigationState()

    state.openCourtAvailability(
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
    )

    state.closeCurrentDetail()

    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
    assertEquals(1, state.myComplexHubReloadRequestKey)
  }

  @Test
  fun closeCurrentDetailReturnsToMyComplexRoot() {
    val state = testNavigationState()
    state.selectMyComplex()

    state.openCreateComplex()
    state.closeCurrentDetail()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun closeAddCourtAfterSuccessReturnsToComplexDetailAndRequestsHubReload() {
    val state = testNavigationState()

    state.openComplexDetail("complex-id")
    state.openAddCourt("complex-id", "North Sports Center")
    state.closeAddCourtAfterSuccess()

    assertEquals(
        listOf(MyComplexRoute, ComplexDetailRoute("complex-id")),
        state.currentBackStack.toList(),
    )
    assertEquals(1, state.myComplexHubReloadRequestKey)
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
        OwnerCourtAvailabilityEntrypoint(
            courtId = "court-id",
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
        )
    )

    state.reset()

    assertEquals(null, state.ownerCourtAvailabilityEntrypoint)
  }

  @Test
  fun switchToPlayerViewSetsViewingAsPlayerAndNavigatesToSearch() {
    val state = testNavigationState().apply { openCatalogCourtDetail(sampleCatalogDetailRoute()) }

    state.switchToPlayerView()

    assertEquals(true, state.viewingAsPlayer)
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun switchToOwnerViewClearsViewingAsPlayerAndNavigatesToMyComplex() {
    val state = testNavigationState().apply { openComplexDetail("complex-id") }

    state.switchToPlayerView()
    state.switchToOwnerView()

    assertEquals(false, state.viewingAsPlayer)
    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun resetSetsViewingAsPlayerTrue() {
    val state = testNavigationState()

    state.switchToOwnerView()
    state.reset()

    assertEquals(true, state.viewingAsPlayer)
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
  }

  @Test
  fun defaultViewingAsPlayerIsTrue() {
    val state = testNavigationState()

    assertEquals(true, state.viewingAsPlayer)
  }

  @Test
  fun playerRoutesSetViewingAsPlayerTrue() {
    val state = testNavigationState()
    state.switchToOwnerView() // start in owner mode

    state.selectSearch()
    assertEquals(true, state.viewingAsPlayer)

    state.switchToOwnerView()
    state.selectNotifications()
    assertEquals(true, state.viewingAsPlayer)

    state.switchToOwnerView()
    state.openCatalogCourtDetail(
        CatalogCourtDetailRoute(
            courtId = "c",
            complexId = "cx",
            complexName = "X",
            courtName = "C",
        )
    )
    assertEquals(true, state.viewingAsPlayer)
  }

  @Test
  fun ownerRoutesSetViewingAsPlayerFalse() {
    val state = testNavigationState()
    // starts as true (player mode); navigating to owner routes must flip to false

    state.selectMyComplex()
    assertEquals(false, state.viewingAsPlayer)

    state.switchToPlayerView()
    state.openComplexDetail("complex-id")
    assertEquals(false, state.viewingAsPlayer)

    state.switchToPlayerView()
    state.openAddCourt("complex-id", "North")
    assertEquals(false, state.viewingAsPlayer)

    state.switchToPlayerView()
    state.openCourtAvailability(
        OwnerCourtAvailabilityEntrypoint(
            courtId = "c",
            courtName = "Cancha 1",
            complexName = "X",
        )
    )
    assertEquals(false, state.viewingAsPlayer)

    state.switchToPlayerView()
    state.returnToMyComplexRoot()
    assertEquals(false, state.viewingAsPlayer)
  }

  @Test
  fun reservationsPreservesViewingAsPlayer() {
    val state = testNavigationState()

    // While in player mode, Reservations keeps viewingAsPlayer = true
    state.selectReservations()
    assertEquals(true, state.viewingAsPlayer)

    // While in owner mode, Reservations keeps viewingAsPlayer = false
    state.switchToOwnerView()
    state.selectReservations()
    assertEquals(false, state.viewingAsPlayer)
  }

  @Test
  fun ownerPreferenceOwnerHydratesMyComplexRootOncePerUserSession() {
    val state = testNavigationState().apply { openCatalogCourtDetail(sampleCatalogDetailRoute()) }

    state.applyOwnerViewPreference(
        userId = "owner-1",
        isOwner = true,
        preference = OwnerViewPreference.OWNER,
    )

    assertEquals(false, state.viewingAsPlayer)
    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())

    state.switchToPlayerView()
    state.applyOwnerViewPreference(
        userId = "owner-1",
        isOwner = true,
        preference = OwnerViewPreference.OWNER,
    )

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
  }

  @Test
  fun ownerPreferencePlayerHydratesSearchRoot() {
    val state = testNavigationState().apply { openComplexDetail("complex-id") }

    state.applyOwnerViewPreference(
        userId = "owner-1",
        isOwner = true,
        preference = OwnerViewPreference.PLAYER,
    )

    assertEquals(true, state.viewingAsPlayer)
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun nonOwnerIgnoresOwnerPreferenceAndLeavesSharedRouteOutsideOwnerShell() {
    val state =
        testNavigationState().apply {
          switchToOwnerView()
          selectReservations()
        }

    state.applyOwnerViewPreference(
        userId = "player-1",
        isOwner = false,
        preference = OwnerViewPreference.OWNER,
    )

    assertEquals(AuthenticatedTopLevelRoute.Reservations, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
    assertEquals(listOf(ReservationsRoute), state.currentBackStack.toList())
  }

  @Test
  fun nonOwnerHydrationRedirectsOwnerShellToSearch() {
    val state = testNavigationState().apply { switchToOwnerView() }

    state.applyOwnerViewPreference(
        userId = "player-1",
        isOwner = false,
        preference = OwnerViewPreference.OWNER,
    )

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun switchToPlayerViewBumpsCatalogReloadKey() {
    val state = testNavigationState()

    val keyBefore = state.catalogReloadRequestKey
    state.switchToPlayerView()

    assertEquals(keyBefore + 1, state.catalogReloadRequestKey)
  }

  @Test
  fun switchToOwnerViewDoesNotBumpCatalogReloadKey() {
    val state = testNavigationState()

    val keyBefore = state.catalogReloadRequestKey
    state.switchToPlayerView() // bumps once
    val keyAfterPlayer = state.catalogReloadRequestKey
    state.switchToOwnerView()

    assertEquals(keyAfterPlayer, state.catalogReloadRequestKey)
    assertEquals(keyBefore + 1, state.catalogReloadRequestKey)
  }

  @Test
  fun resetClearsCatalogReloadKey() {
    val state = testNavigationState()

    state.switchToPlayerView() // bumps key
    state.reset()

    assertEquals(0, state.catalogReloadRequestKey)
  }

  @Test
  fun coordinatorHydratesOwnerShellForStoredOwnerPreference() = runTest {
    val state = testNavigationState().apply { openCatalogCourtDetail(sampleCatalogDetailRoute()) }
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences = mapOf("owner-1" to OwnerViewPreference.OWNER)
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(AuthStateFactory.owner(userId = "owner-1"))

    assertEquals(listOf("owner-1"), storage.readUserIds)
    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(false, state.viewingAsPlayer)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun coordinatorDefersOwnerPreferenceHydrationUntilAuthenticatedStartupResolves() = runTest {
    val state = testNavigationState().apply { switchToOwnerView() }
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences = mapOf("owner-1" to OwnerViewPreference.OWNER)
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(
        io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState(
            userId = "owner-1",
            isAuthenticated = true,
            isOwner = false,
            isResolvingAuthenticatedStartup = true,
        )
    )

    assertEquals(emptyList(), storage.readUserIds)
    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(false, state.viewingAsPlayer)
    assertEquals(listOf(MyComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun coordinatorHydratesPlayerShellForStoredPlayerPreference() = runTest {
    val state = testNavigationState().apply { openComplexDetail("complex-id") }
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences = mapOf("owner-1" to OwnerViewPreference.PLAYER)
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(AuthStateFactory.owner(userId = "owner-1"))

    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
    assertEquals(listOf(SearchRoute), state.currentBackStack.toList())
  }

  @Test
  fun coordinatorPersistsExplicitAppBarSwitchesPerAuthenticatedOwner() = runTest {
    val state = testNavigationState()
    val storage = RecordingOwnerViewPreferenceStorage()
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)
    val ownerState = AuthStateFactory.owner(userId = " owner-1 ")

    coordinator.switchToOwnerView(ownerState)
    coordinator.switchToPlayerView(ownerState)
    advanceUntilIdle()

    assertEquals(
        listOf(
            SavedOwnerPreference(userId = " owner-1 ", preference = OwnerViewPreference.OWNER),
            SavedOwnerPreference(userId = " owner-1 ", preference = OwnerViewPreference.PLAYER),
        ),
        storage.savedPreferences,
    )
    assertEquals(OwnerViewPreference.PLAYER, storage.getOwnerViewPreference("owner-1"))
  }

  @Test
  fun coordinatorIgnoresOwnerPreferenceSaveFailuresWithoutCancellingSwitchFlow() = runTest {
    val state = testNavigationState()
    val storage = RecordingOwnerViewPreferenceStorage(throwOnSave = true)
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)
    val ownerState = AuthStateFactory.owner(userId = "owner-1")

    coordinator.switchToOwnerView(ownerState)
    advanceUntilIdle()

    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)
    assertEquals(false, state.viewingAsPlayer)

    coordinator.switchToPlayerView(ownerState)
    advanceUntilIdle()

    assertEquals(
        listOf(
            SavedOwnerPreference(userId = "owner-1", preference = OwnerViewPreference.OWNER),
            SavedOwnerPreference(userId = "owner-1", preference = OwnerViewPreference.PLAYER),
        ),
        storage.savedPreferences,
    )
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
    assertEquals(1, state.catalogReloadRequestKey)
  }

  @Test
  fun coordinatorHydratesEachAuthenticatedUserFromOwnStoredPreference() = runTest {
    val state = testNavigationState()
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences =
                mapOf(
                    "owner-1" to OwnerViewPreference.OWNER,
                    "owner-2" to OwnerViewPreference.PLAYER,
                )
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(AuthStateFactory.owner(userId = "owner-1"))
    assertEquals(AuthenticatedTopLevelRoute.MyComplex, state.selectedRoute)

    coordinator.switchToOwnerView(AuthStateFactory.owner(userId = "owner-1"))
    advanceUntilIdle()
    coordinator.hydrate(AuthStateFactory.owner(userId = "owner-2"))

    assertEquals(listOf("owner-1", "owner-2"), storage.readUserIds)
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
  }

  @Test
  fun coordinatorNonOwnerIgnoresStoredOwnerPreference() = runTest {
    val state = testNavigationState().apply { switchToOwnerView() }
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences = mapOf("player-1" to OwnerViewPreference.OWNER)
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(AuthStateFactory.player(userId = "player-1"))

    assertEquals(emptyList(), storage.readUserIds)
    assertEquals(AuthenticatedTopLevelRoute.Search, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
  }

  @Test
  fun sharedRouteNavigationDoesNotPersistOrOverwriteOwnerPreference() = runTest {
    val state = testNavigationState()
    val storage =
        RecordingOwnerViewPreferenceStorage(
            ownerPreferences = mapOf("owner-1" to OwnerViewPreference.PLAYER)
        )
    val coordinator = OwnerViewPreferenceCoordinator(state, storage, this)

    coordinator.hydrate(AuthStateFactory.owner(userId = "owner-1"))
    state.selectReservations()
    advanceUntilIdle()

    assertEquals(emptyList(), storage.savedPreferences)
    assertEquals(OwnerViewPreference.PLAYER, storage.getOwnerViewPreference("owner-1"))
    assertEquals(AuthenticatedTopLevelRoute.Reservations, state.selectedRoute)
    assertEquals(true, state.viewingAsPlayer)
  }

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
          viewingAsPlayerState = mutableStateOf(true),
          hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
      )

  private fun sampleCatalogDetailRoute() =
      CatalogCourtDetailRoute(
          courtId = "court-id",
          complexId = "complex-id",
          complexName = "Mejengas CR",
          courtName = "Cancha 1",
      )

  private data class SavedOwnerPreference(
      val userId: String,
      val preference: OwnerViewPreference,
  )

  private class RecordingOwnerViewPreferenceStorage(
      ownerPreferences: Map<String, OwnerViewPreference> = emptyMap(),
      private val throwOnSave: Boolean = false,
  ) : io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage {
    private val ownerViewPreferences = ownerPreferences.toMutableMap()
    val readUserIds = mutableListOf<String>()
    val savedPreferences = mutableListOf<SavedOwnerPreference>()

    override suspend fun getSession() = error("Not used in this test")

    override suspend fun saveSession(
        session: io.github.themonstersp4.mejengueros.domain.model.AuthSession
    ) = error("Not used in this test")

    override suspend fun clearSession() = error("Not used in this test")

    override suspend fun getOAuthState() = error("Not used in this test")

    override suspend fun saveOAuthState(
        state: io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
    ) = error("Not used in this test")

    override suspend fun clearOAuthState() = error("Not used in this test")

    override suspend fun getOwnerViewPreference(userId: String): OwnerViewPreference? {
      readUserIds += userId
      return ownerViewPreferences[userId.trim()]
    }

    override suspend fun saveOwnerViewPreference(userId: String, preference: OwnerViewPreference) {
      savedPreferences += SavedOwnerPreference(userId = userId, preference = preference)
      if (throwOnSave) {
        throw AuthSecureStorageWriteException("Failed to securely persist owner view preference.")
      }
      ownerViewPreferences[userId.trim()] = preference
    }

    override suspend fun clearOwnerViewPreference(userId: String) {
      ownerViewPreferences.remove(userId.trim())
    }
  }

  private object AuthStateFactory {
    fun owner(userId: String) =
        io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState(
            userId = userId,
            isAuthenticated = true,
            isOwner = true,
        )

    fun player(userId: String) =
        io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState(
            userId = userId,
            isAuthenticated = true,
            isOwner = false,
        )
  }
}
