package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration

@Composable
fun rememberAuthenticatedNavigationState(
    savedStateConfiguration: SavedStateConfiguration,
): AuthenticatedNavigationState {
  val homeBackStack = rememberNavBackStack(savedStateConfiguration, HomeRoute)
  val kitBackStack = rememberNavBackStack(savedStateConfiguration, KitRoute)
  val pokedexBackStack = rememberNavBackStack(savedStateConfiguration, PokedexRoute)

  val selectedRoute = rememberSaveable { mutableStateOf(AuthenticatedTopLevelRoute.Home) }
  val ownerCourtAvailabilityEntrypointState =
      rememberSaveable(stateSaver = ownerCourtAvailabilityEntrypointSaver()) {
        mutableStateOf<OwnerCourtAvailabilityEntrypoint?>(null)
      }

  return remember(
      homeBackStack,
      kitBackStack,
      pokedexBackStack,
      selectedRoute,
      ownerCourtAvailabilityEntrypointState,
  ) {
    AuthenticatedNavigationState(
        selectedRoute = selectedRoute,
        homeBackStack = homeBackStack,
        kitBackStack = kitBackStack,
        pokedexBackStack = pokedexBackStack,
        ownerCourtAvailabilityEntrypointState = ownerCourtAvailabilityEntrypointState,
    )
  }
}

class AuthenticatedNavigationState(
    selectedRoute: MutableState<AuthenticatedTopLevelRoute>,
    private val homeBackStack: NavBackStack<NavKey>,
    private val kitBackStack: NavBackStack<NavKey>,
    private val pokedexBackStack: NavBackStack<NavKey>,
    private val ownerCourtAvailabilityEntrypointState:
        MutableState<OwnerCourtAvailabilityEntrypoint?>,
) {
  var selectedRoute: AuthenticatedTopLevelRoute by selectedRoute
    private set

  val ownerCourtAvailabilityEntrypoint: OwnerCourtAvailabilityEntrypoint?
    get() = ownerCourtAvailabilityEntrypointState.value

  val currentBackStack: NavBackStack<NavKey>
    get() =
        when (selectedRoute) {
          AuthenticatedTopLevelRoute.Home -> homeBackStack
          AuthenticatedTopLevelRoute.Kit -> kitBackStack
          AuthenticatedTopLevelRoute.Pokedex -> pokedexBackStack
        }

  fun selectHome() {
    selectedRoute = AuthenticatedTopLevelRoute.Home
  }

  fun openCreateComplex() {
    selectedRoute = AuthenticatedTopLevelRoute.Home
    if (homeBackStack.lastOrNull() != CreateComplexRoute) {
      homeBackStack.add(CreateComplexRoute)
    }
  }

  fun openCourtCatalogDetail(courtId: String) {
    selectedRoute = AuthenticatedTopLevelRoute.Home
    val route = CourtCatalogDetailRoute(courtId)
    if (homeBackStack.lastOrNull() != route) {
      homeBackStack.add(route)
    }
  }

  fun returnToHomeRoot() {
    selectedRoute = AuthenticatedTopLevelRoute.Home
    homeBackStack.clear()
    homeBackStack.add(HomeRoute)
  }

  fun selectKit() {
    selectedRoute = AuthenticatedTopLevelRoute.Kit
  }

  fun selectPokedex() {
    selectedRoute = AuthenticatedTopLevelRoute.Pokedex
  }

  fun openAvailabilitySelectors() {
    selectedRoute = AuthenticatedTopLevelRoute.Kit
    if (kitBackStack.lastOrNull() != AvailabilitySelectorsRoute) {
      kitBackStack.add(AvailabilitySelectorsRoute)
    }
  }

  fun openCourtAvailability(courtId: String, courtName: String, complexName: String) {
    ownerCourtAvailabilityEntrypointState.value =
        OwnerCourtAvailabilityEntrypoint(
            courtId = courtId,
            courtName = courtName,
            complexName = complexName,
        )
    selectedRoute = AuthenticatedTopLevelRoute.Home
    if (homeBackStack.lastOrNull() == CreateComplexRoute) {
      homeBackStack.removeLastOrNull()
    }

    val route =
        CourtAvailabilityRoute(courtId = courtId, courtName = courtName, complexName = complexName)
    if (homeBackStack.lastOrNull() != route) {
      homeBackStack.add(route)
    }
  }

  fun openOwnerCourtAvailabilityEntrypoint() {
    val entrypoint = ownerCourtAvailabilityEntrypointState.value ?: return
    openCourtAvailability(
        courtId = entrypoint.courtId,
        courtName = entrypoint.courtName,
        complexName = entrypoint.complexName,
    )
  }

  fun openPokemonDetail(id: Int) {
    selectedRoute = AuthenticatedTopLevelRoute.Pokedex
    pokedexBackStack.add(PokemonDetailRoute(id))
  }

  fun closeCurrentDetail() {
    if (currentBackStack.size > 1) {
      currentBackStack.removeLastOrNull()
    }
  }

  fun reset() {
    ownerCourtAvailabilityEntrypointState.value = null
    returnToHomeRoot()
    kitBackStack.clear()
    kitBackStack.add(KitRoute)
    pokedexBackStack.clear()
    pokedexBackStack.add(PokedexRoute)
  }
}

private fun ownerCourtAvailabilityEntrypointSaver() =
    listSaver<OwnerCourtAvailabilityEntrypoint?, String?>(
        save = {
          listOf(
              it?.courtId,
              it?.courtName,
              it?.complexName,
          )
        },
        restore = { values ->
          values.firstOrNull()?.let { courtId ->
            OwnerCourtAvailabilityEntrypoint(
                courtId = courtId,
                courtName = values.getOrNull(1).orEmpty(),
                complexName = values.getOrNull(2).orEmpty(),
            )
          }
        },
    )
