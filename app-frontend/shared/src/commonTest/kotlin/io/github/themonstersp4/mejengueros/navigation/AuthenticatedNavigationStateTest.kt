package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthenticatedNavigationStateTest {

  @Test
  fun startsAtHomeStack() {
    val state = testNavigationState()

    assertEquals(AuthenticatedTopLevelRoute.Home, state.selectedRoute)
    assertEquals(listOf(HomeRoute), state.currentBackStack.toList())
  }

  @Test
  fun openPokemonDetailSelectsPokedexAndAppendsDetailRoute() {
    val state = testNavigationState()

    state.openPokemonDetail(25)

    assertEquals(AuthenticatedTopLevelRoute.Pokedex, state.selectedRoute)
    assertEquals(listOf(PokedexRoute, PokemonDetailRoute(25)), state.currentBackStack.toList())
  }

  @Test
  fun openCreateComplexKeepsHomeSelectedAndAppendsHomeDetailRoute() {
    val state = testNavigationState()

    state.openCreateComplex()

    assertEquals(AuthenticatedTopLevelRoute.Home, state.selectedRoute)
    assertEquals(listOf(HomeRoute, CreateComplexRoute), state.currentBackStack.toList())
  }

  @Test
  fun returnToHomeRootClearsHomeDetailStack() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.returnToHomeRoot()

    assertEquals(AuthenticatedTopLevelRoute.Home, state.selectedRoute)
    assertEquals(listOf(HomeRoute), state.currentBackStack.toList())
  }

  @Test
  fun switchingTabsPreservesPokedexDetailStack() {
    val state = testNavigationState()

    state.openPokemonDetail(25)
    state.selectHome()
    state.selectPokedex()

    assertEquals(listOf(PokedexRoute, PokemonDetailRoute(25)), state.currentBackStack.toList())
  }

  @Test
  fun selectingKitShowsComponentKitRoot() {
    val state = testNavigationState()

    state.selectKit()

    assertEquals(AuthenticatedTopLevelRoute.Kit, state.selectedRoute)
    assertEquals(listOf(KitRoute), state.currentBackStack.toList())
  }

  @Test
  fun openAvailabilitySelectorsSelectsKitAndAppendsDemoRoute() {
    val state = testNavigationState()

    state.openAvailabilitySelectors()

    assertEquals(AuthenticatedTopLevelRoute.Kit, state.selectedRoute)
    assertEquals(listOf(KitRoute, AvailabilitySelectorsRoute), state.currentBackStack.toList())
  }

  @Test
  fun openCourtAvailabilityReplacesCreateComplexDetailInsideHomeFlow() {
    val state = testNavigationState()

    state.openCreateComplex()
    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )

    assertEquals(AuthenticatedTopLevelRoute.Home, state.selectedRoute)
    assertEquals(
        listOf(HomeRoute, CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")),
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
  fun reopenOwnerCourtAvailabilityUsesStoredContextFromHomeRoot() {
    val state = testNavigationState()

    state.openCourtAvailability(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
    )
    state.returnToHomeRoot()
    state.openOwnerCourtAvailabilityEntrypoint()

    assertEquals(
        listOf(HomeRoute, CourtAvailabilityRoute("court-id", "Cancha 1", "Mejengas CR")),
        state.currentBackStack.toList(),
    )
  }

  @Test
  fun closeAvailabilitySelectorsReturnsToKitRoot() {
    val state = testNavigationState()

    state.openAvailabilitySelectors()
    state.closeCurrentDetail()

    assertEquals(AuthenticatedTopLevelRoute.Kit, state.selectedRoute)
    assertEquals(listOf(KitRoute), state.currentBackStack.toList())
  }

  @Test
  fun closeCurrentDetailDoesNotRemoveRootRoute() {
    val state = testNavigationState()

    state.selectPokedex()
    state.closeCurrentDetail()

    assertEquals(listOf(PokedexRoute), state.currentBackStack.toList())
  }

  @Test
  fun resetRestoresAuthenticatedRootStacks() {
    val state = testNavigationState()

    state.openPokemonDetail(25)
    state.selectHome()
    state.reset()

    assertEquals(AuthenticatedTopLevelRoute.Home, state.selectedRoute)
    assertEquals(listOf(HomeRoute), state.currentBackStack.toList())

    state.selectKit()
    assertEquals(listOf(KitRoute), state.currentBackStack.toList())

    state.selectPokedex()
    assertEquals(listOf(PokedexRoute), state.currentBackStack.toList())
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
          selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Home),
          homeBackStack = NavBackStack<NavKey>(HomeRoute),
          kitBackStack = NavBackStack<NavKey>(KitRoute),
          pokedexBackStack = NavBackStack<NavKey>(PokedexRoute),
          ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
      )
}
