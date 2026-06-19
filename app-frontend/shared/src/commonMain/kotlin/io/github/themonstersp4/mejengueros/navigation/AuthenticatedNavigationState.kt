package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

  return remember(homeBackStack, kitBackStack, pokedexBackStack, selectedRoute) {
    AuthenticatedNavigationState(
        selectedRoute = selectedRoute,
        homeBackStack = homeBackStack,
        kitBackStack = kitBackStack,
        pokedexBackStack = pokedexBackStack,
    )
  }
}

class AuthenticatedNavigationState(
    selectedRoute: MutableState<AuthenticatedTopLevelRoute>,
    private val homeBackStack: NavBackStack<NavKey>,
    private val kitBackStack: NavBackStack<NavKey>,
    private val pokedexBackStack: NavBackStack<NavKey>,
) {
  var selectedRoute: AuthenticatedTopLevelRoute by selectedRoute
    private set

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
    selectedRoute = AuthenticatedTopLevelRoute.Home
    homeBackStack.clear()
    homeBackStack.add(HomeRoute)
    kitBackStack.clear()
    kitBackStack.add(KitRoute)
    pokedexBackStack.clear()
    pokedexBackStack.add(PokedexRoute)
  }
}
