package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
  val searchBackStack = rememberNavBackStack(savedStateConfiguration, SearchRoute)
  val reservationsBackStack = rememberNavBackStack(savedStateConfiguration, ReservationsRoute)
  val notificationsBackStack = rememberNavBackStack(savedStateConfiguration, NotificationsRoute)
  val myComplexBackStack = rememberNavBackStack(savedStateConfiguration, MyComplexRoute)

  val selectedRouteState = rememberSavedAuthenticatedTopLevelRouteState()
  val ownerCourtAvailabilityEntrypointState =
      rememberSaveable(stateSaver = ownerCourtAvailabilityEntrypointSaver()) {
        mutableStateOf<OwnerCourtAvailabilityEntrypoint?>(null)
      }

  return remember(
      searchBackStack,
      reservationsBackStack,
      notificationsBackStack,
      myComplexBackStack,
      selectedRouteState,
      ownerCourtAvailabilityEntrypointState,
  ) {
    normalizeRestoredAuthenticatedNavigationState(
        savedSelectedRouteName = selectedRouteState.savedRouteName,
        selectedRoute = selectedRouteState.route,
        searchBackStack = searchBackStack,
        reservationsBackStack = reservationsBackStack,
        notificationsBackStack = notificationsBackStack,
        myComplexBackStack = myComplexBackStack,
    )

    AuthenticatedNavigationState(
        selectedRoute = selectedRouteState.route,
        searchBackStack = searchBackStack,
        reservationsBackStack = reservationsBackStack,
        notificationsBackStack = notificationsBackStack,
        myComplexBackStack = myComplexBackStack,
        ownerCourtAvailabilityEntrypointState = ownerCourtAvailabilityEntrypointState,
    )
  }
}

internal data class NormalizedAuthenticatedNavigation(
    val selectedRoute: AuthenticatedTopLevelRoute,
    val searchStack: List<AppRoute>,
    val reservationsStack: List<AppRoute>,
    val notificationsStack: List<AppRoute>,
    val myComplexStack: List<AppRoute>,
)

internal fun normalizeRestoredAuthenticatedNavigation(
    savedSelectedRouteName: String?,
    searchStack: List<AppRoute>,
    reservationsStack: List<AppRoute>,
    notificationsStack: List<AppRoute>,
    myComplexStack: List<AppRoute>,
): NormalizedAuthenticatedNavigation {
  val legacyOwnerFlow = legacyOwnerFlowFrom(searchStack)
  val normalizedSearchStack = normalizeSearchStack(searchStack)
  val normalizedMyComplexStack =
      normalizeMyComplexStack(myComplexStack).let { currentStack ->
        when {
          legacyOwnerFlow == null -> currentStack
          currentStack == listOf(MyComplexRoute) -> legacyOwnerFlow
          else -> currentStack
        }
      }

  return NormalizedAuthenticatedNavigation(
      selectedRoute =
          normalizeAuthenticatedTopLevelRoute(
              savedSelectedRouteName = savedSelectedRouteName,
              hasLegacyOwnerFlow = legacyOwnerFlow != null,
          ),
      searchStack = normalizedSearchStack,
      reservationsStack = normalizeReservationsStack(reservationsStack),
      notificationsStack = normalizeNotificationsStack(notificationsStack),
      myComplexStack = normalizedMyComplexStack,
  )
}

private fun normalizeRestoredAuthenticatedNavigationState(
    savedSelectedRouteName: State<String>,
    selectedRoute: MutableState<AuthenticatedTopLevelRoute>,
    searchBackStack: NavBackStack<NavKey>,
    reservationsBackStack: NavBackStack<NavKey>,
    notificationsBackStack: NavBackStack<NavKey>,
    myComplexBackStack: NavBackStack<NavKey>,
) {
  val normalized =
      normalizeRestoredAuthenticatedNavigation(
          savedSelectedRouteName = savedSelectedRouteName.value,
          searchStack = searchBackStack.toAppRoutes(),
          reservationsStack = reservationsBackStack.toAppRoutes(),
          notificationsStack = notificationsBackStack.toAppRoutes(),
          myComplexStack = myComplexBackStack.toAppRoutes(),
      )

  selectedRoute.value = normalized.selectedRoute
  searchBackStack.replaceWith(normalized.searchStack)
  reservationsBackStack.replaceWith(normalized.reservationsStack)
  notificationsBackStack.replaceWith(normalized.notificationsStack)
  myComplexBackStack.replaceWith(normalized.myComplexStack)
}

private fun NavBackStack<NavKey>.toAppRoutes(): List<AppRoute> = map { route ->
  route as? AppRoute ?: SearchRoute
}

private fun NavBackStack<NavKey>.replaceWith(routes: List<AppRoute>) {
  clear()
  routes.forEach(::add)
}

@Composable
private fun rememberSavedAuthenticatedTopLevelRouteState(): SavedAuthenticatedTopLevelRouteState {
  val savedRouteName =
      rememberSaveable(saver = savedAuthenticatedTopLevelRouteNameStateSaver()) {
        mutableStateOf(AuthenticatedTopLevelRoute.Search.name)
      }
  val routeState =
      remember(savedRouteName) {
        object : MutableState<AuthenticatedTopLevelRoute> {
          override var value: AuthenticatedTopLevelRoute
            get() = normalizeAuthenticatedTopLevelRoute(savedRouteName.value, false)
            set(value) {
              savedRouteName.value = value.name
            }

          override fun component1(): AuthenticatedTopLevelRoute = value

          override fun component2(): (AuthenticatedTopLevelRoute) -> Unit = { value = it }
        }
      }
  return remember(savedRouteName, routeState) {
    SavedAuthenticatedTopLevelRouteState(savedRouteName = savedRouteName, route = routeState)
  }
}

private data class SavedAuthenticatedTopLevelRouteState(
    val savedRouteName: MutableState<String>,
    val route: MutableState<AuthenticatedTopLevelRoute>,
)

private fun savedAuthenticatedTopLevelRouteNameStateSaver(): Saver<MutableState<String>, Any> =
    Saver(
        save = { it.value },
        restore = { savedValue ->
          mutableStateOf(restoreSavedAuthenticatedTopLevelRouteName(savedValue))
        },
    )

internal fun restoreSavedAuthenticatedTopLevelRouteName(savedValue: Any?): String =
    when (savedValue) {
      is String -> savedValue
      is CharSequence -> savedValue.toString()
      is AuthenticatedTopLevelRoute -> savedValue.name
      is Enum<*> -> savedValue.name
      else -> AuthenticatedTopLevelRoute.Search.name
    }

private fun normalizeAuthenticatedTopLevelRoute(
    savedSelectedRouteName: String?,
    hasLegacyOwnerFlow: Boolean,
): AuthenticatedTopLevelRoute =
    when (savedSelectedRouteName) {
      AuthenticatedTopLevelRoute.Search.name -> AuthenticatedTopLevelRoute.Search
      AuthenticatedTopLevelRoute.Reservations.name -> AuthenticatedTopLevelRoute.Reservations
      AuthenticatedTopLevelRoute.Notifications.name -> AuthenticatedTopLevelRoute.Notifications
      AuthenticatedTopLevelRoute.MyComplex.name -> AuthenticatedTopLevelRoute.MyComplex
      "Home" ->
          if (hasLegacyOwnerFlow) AuthenticatedTopLevelRoute.MyComplex
          else AuthenticatedTopLevelRoute.Search
      "Kit",
      "Pokedex" -> AuthenticatedTopLevelRoute.Search
      else -> AuthenticatedTopLevelRoute.Search
    }

private fun normalizeSearchStack(routes: List<AppRoute>): List<AppRoute> =
    if (legacyOwnerFlowFrom(routes) != null) listOf(SearchRoute)
    else listOf(routes.firstOrNull()?.normalizeForSearchRoot() ?: SearchRoute)

private fun normalizeReservationsStack(routes: List<AppRoute>): List<AppRoute> =
    if (routes.firstOrNull() == ReservationsRoute) listOf(ReservationsRoute)
    else listOf(ReservationsRoute)

private fun normalizeNotificationsStack(routes: List<AppRoute>): List<AppRoute> =
    if (routes.firstOrNull() == NotificationsRoute) listOf(NotificationsRoute)
    else listOf(NotificationsRoute)

private fun normalizeMyComplexStack(routes: List<AppRoute>): List<AppRoute> {
  val details = routes.drop(1).mapNotNull { it.normalizeForMyComplexStack() }
  return listOf(MyComplexRoute) + details.distinct()
}

private fun legacyOwnerFlowFrom(routes: List<AppRoute>): List<AppRoute>? {
  val details = routes.drop(1).mapNotNull { it.normalizeForMyComplexStack() }
  return if (details.isEmpty()) null else listOf(MyComplexRoute) + details.distinct()
}

private fun AppRoute.normalizeForSearchRoot(): AppRoute =
    when (this) {
      HomeRoute,
      KitRoute,
      AvailabilitySelectorsRoute,
      PokedexRoute,
      is PokemonDetailRoute,
      SearchRoute,
      CreateComplexRoute,
      is CourtAvailabilityRoute,
      MyComplexRoute,
      ReservationsRoute,
      NotificationsRoute -> SearchRoute
      else -> SearchRoute
    }

private fun AppRoute.normalizeForMyComplexStack(): AppRoute? =
    when (this) {
      CreateComplexRoute -> CreateComplexRoute
      is CourtAvailabilityRoute -> this
      else -> null
    }

class AuthenticatedNavigationState(
    selectedRoute: MutableState<AuthenticatedTopLevelRoute>,
    private val searchBackStack: NavBackStack<NavKey>,
    private val reservationsBackStack: NavBackStack<NavKey>,
    private val notificationsBackStack: NavBackStack<NavKey>,
    private val myComplexBackStack: NavBackStack<NavKey>,
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
          AuthenticatedTopLevelRoute.Search -> searchBackStack
          AuthenticatedTopLevelRoute.Reservations -> reservationsBackStack
          AuthenticatedTopLevelRoute.Notifications -> notificationsBackStack
          AuthenticatedTopLevelRoute.MyComplex -> myComplexBackStack
        }

  fun selectSearch() {
    selectedRoute = AuthenticatedTopLevelRoute.Search
  }

  fun selectReservations() {
    selectedRoute = AuthenticatedTopLevelRoute.Reservations
  }

  fun selectNotifications() {
    selectedRoute = AuthenticatedTopLevelRoute.Notifications
  }

  fun selectMyComplex() {
    selectedRoute = AuthenticatedTopLevelRoute.MyComplex
  }

  fun openCreateComplex() {
    selectedRoute = AuthenticatedTopLevelRoute.MyComplex
    if (myComplexBackStack.lastOrNull() != CreateComplexRoute) {
      myComplexBackStack.add(CreateComplexRoute)
    }
  }

  fun returnToMyComplexRoot() {
    selectedRoute = AuthenticatedTopLevelRoute.MyComplex
    myComplexBackStack.clear()
    myComplexBackStack.add(MyComplexRoute)
  }

  fun openCourtAvailability(courtId: String, courtName: String, complexName: String) {
    ownerCourtAvailabilityEntrypointState.value =
        OwnerCourtAvailabilityEntrypoint(
            courtId = courtId,
            courtName = courtName,
            complexName = complexName,
        )
    selectedRoute = AuthenticatedTopLevelRoute.MyComplex
    if (myComplexBackStack.lastOrNull() == CreateComplexRoute) {
      myComplexBackStack.removeLastOrNull()
    }

    val route =
        CourtAvailabilityRoute(courtId = courtId, courtName = courtName, complexName = complexName)
    if (myComplexBackStack.lastOrNull() != route) {
      myComplexBackStack.add(route)
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

  fun closeCurrentDetail() {
    if (currentBackStack.size > 1) {
      currentBackStack.removeLastOrNull()
    }
  }

  fun reset() {
    ownerCourtAvailabilityEntrypointState.value = null
    selectedRoute = AuthenticatedTopLevelRoute.Search
    searchBackStack.clear()
    searchBackStack.add(SearchRoute)
    reservationsBackStack.clear()
    reservationsBackStack.add(ReservationsRoute)
    notificationsBackStack.clear()
    notificationsBackStack.add(NotificationsRoute)
    myComplexBackStack.clear()
    myComplexBackStack.add(MyComplexRoute)
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
