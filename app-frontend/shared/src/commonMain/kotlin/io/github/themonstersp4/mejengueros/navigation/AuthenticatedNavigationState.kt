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
import io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference

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
  val myComplexHubReloadRequestKeyState = rememberSaveable { mutableStateOf(0) }
  val catalogReloadRequestKeyState = rememberSaveable { mutableStateOf(0) }
  val viewingAsPlayerState = rememberSaveable { mutableStateOf(true) }
  val hydratedOwnerPreferenceUserIdState = rememberSaveable { mutableStateOf<String?>(null) }

  return remember(
      searchBackStack,
      reservationsBackStack,
      notificationsBackStack,
      myComplexBackStack,
      selectedRouteState,
      ownerCourtAvailabilityEntrypointState,
      myComplexHubReloadRequestKeyState,
      catalogReloadRequestKeyState,
      viewingAsPlayerState,
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
        myComplexHubReloadRequestKeyState = myComplexHubReloadRequestKeyState,
        catalogReloadRequestKeyState = catalogReloadRequestKeyState,
        viewingAsPlayerState = viewingAsPlayerState,
        hydratedOwnerPreferenceUserIdState = hydratedOwnerPreferenceUserIdState,
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
      is AppRoute -> savedValue.toLegacyAuthenticatedTopLevelRouteName()
      is Enum<*> -> savedValue.name
      else -> AuthenticatedTopLevelRoute.Search.name
    }

private fun AppRoute.toLegacyAuthenticatedTopLevelRouteName(): String =
    when (this) {
      HomeRoute -> "Home"
      SearchRoute -> AuthenticatedTopLevelRoute.Search.name
      is CatalogCourtDetailRoute,
      is CatalogReservationRoute -> AuthenticatedTopLevelRoute.Search.name
      ReservationsRoute -> AuthenticatedTopLevelRoute.Reservations.name
      NotificationsRoute -> AuthenticatedTopLevelRoute.Notifications.name
      MyComplexRoute,
      is ComplexDetailRoute,
      is AddCourtRoute,
      CreateComplexRoute,
      is CourtAvailabilityRoute,
      OwnerReceivedReviewsRoute -> AuthenticatedTopLevelRoute.MyComplex.name
      KitRoute,
      AvailabilitySelectorsRoute -> "Kit"
      PokedexRoute,
      is PokemonDetailRoute -> "Pokedex"
      LoginRoute,
      RegisterRoute,
      VerifyAccountRoute,
      ForgotPasswordRoute,
      ResetPasswordRoute -> AuthenticatedTopLevelRoute.Search.name
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
    if (legacyOwnerFlowFrom(routes) != null) {
      listOf(SearchRoute)
    } else {
      val details = routes.drop(1).mapNotNull { it.normalizeForSearchStack() }
      listOf(routes.firstOrNull()?.normalizeForSearchRoot() ?: SearchRoute) + details.distinct()
    }

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
      is CatalogCourtDetailRoute,
      is CatalogReservationRoute,
      CreateComplexRoute,
      is CourtAvailabilityRoute,
      MyComplexRoute,
      ReservationsRoute,
      NotificationsRoute -> SearchRoute
      else -> SearchRoute
    }

private fun AppRoute.normalizeForSearchStack(): AppRoute? =
    when (this) {
      is CatalogCourtDetailRoute -> this
      is CatalogReservationRoute -> this
      else -> null
    }

private fun AppRoute.normalizeForMyComplexStack(): AppRoute? =
    when (this) {
      is ComplexDetailRoute -> this
      is AddCourtRoute -> this
      CreateComplexRoute -> CreateComplexRoute
      is CourtAvailabilityRoute -> this
      OwnerReceivedReviewsRoute -> OwnerReceivedReviewsRoute
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
    private val myComplexHubReloadRequestKeyState: MutableState<Int>,
    private val catalogReloadRequestKeyState: MutableState<Int>,
    private val viewingAsPlayerState: MutableState<Boolean>,
    private val hydratedOwnerPreferenceUserIdState: MutableState<String?>,
) {
  private var nextCatalogReservationAttemptId: Long =
      searchBackStack
          .filterIsInstance<CatalogReservationRoute>()
          .maxOfOrNull { it.attemptId }
          ?.plus(1) ?: 1L

  var selectedRoute: AuthenticatedTopLevelRoute by selectedRoute
    private set

  // True when an owner is temporarily viewing the app in mejenguero (player) mode.
  // Non-owners are unaffected — this flag is only meaningful when isOwner is true.
  val viewingAsPlayer: Boolean
    get() = viewingAsPlayerState.value

  val ownerCourtAvailabilityEntrypoint: OwnerCourtAvailabilityEntrypoint?
    get() = ownerCourtAvailabilityEntrypointState.value

  val myComplexHubReloadRequestKey: Int
    get() = myComplexHubReloadRequestKeyState.value

  val catalogReloadRequestKey: Int
    get() = catalogReloadRequestKeyState.value

  val currentBackStack: NavBackStack<NavKey>
    get() =
        when (selectedRoute) {
          AuthenticatedTopLevelRoute.Search -> searchBackStack
          AuthenticatedTopLevelRoute.Reservations -> reservationsBackStack
          AuthenticatedTopLevelRoute.Notifications -> notificationsBackStack
          AuthenticatedTopLevelRoute.MyComplex -> myComplexBackStack
        }

  fun selectSearch() {
    navigateTo(AuthenticatedTopLevelRoute.Search)
  }

  fun selectReservations() {
    navigateTo(AuthenticatedTopLevelRoute.Reservations)
  }

  fun selectNotifications() {
    navigateTo(AuthenticatedTopLevelRoute.Notifications)
  }

  fun selectMyComplex() {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
  }

  fun returnToSearchRoot() {
    showSearchRoot()
  }

  fun openCreateComplex() {
    // Push onto the current stack so "back" returns to where the flow started:
    // the catalog when a mejenguero taps the on-ramp, the owner hub otherwise.
    if (currentBackStack.lastOrNull() != CreateComplexRoute) {
      currentBackStack.add(CreateComplexRoute)
    }
  }

  fun openCatalogCourtDetail(route: CatalogCourtDetailRoute) {
    navigateTo(AuthenticatedTopLevelRoute.Search)
    while (searchBackStack.size > 1) {
      searchBackStack.removeLastOrNull()
    }
    if (searchBackStack.lastOrNull() != route) {
      searchBackStack.add(route)
    }
  }

  fun openCatalogReservation(route: CatalogReservationRoute) {
    navigateTo(AuthenticatedTopLevelRoute.Search)
    val reservationRoute =
        if (route.requiresGeneratedAttemptId()) {
          route.copy(attemptId = nextCatalogReservationAttemptId++)
        } else {
          route
        }
    val detailRoute =
        CatalogCourtDetailRoute(
            courtId = reservationRoute.courtId,
            complexId = reservationRoute.complexId,
            complexName = reservationRoute.complexName,
            courtName = reservationRoute.courtName,
            provinceName = reservationRoute.provinceName,
            cantonName = reservationRoute.cantonName,
        )
    if (searchBackStack.none { it == detailRoute }) {
      while (searchBackStack.size > 1) {
        searchBackStack.removeLastOrNull()
      }
      searchBackStack.add(detailRoute)
    } else {
      while (searchBackStack.lastOrNull() != detailRoute && searchBackStack.size > 1) {
        searchBackStack.removeLastOrNull()
      }
    }
    if (searchBackStack.lastOrNull() != reservationRoute) {
      searchBackStack.add(reservationRoute)
    }
  }

  private fun CatalogReservationRoute.requiresGeneratedAttemptId(): Boolean =
      attemptId == UnspecifiedCatalogReservationAttemptId

  fun openComplexDetail(complexId: String) {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    while (myComplexBackStack.size > 1) {
      myComplexBackStack.removeLastOrNull()
    }
    val route = ComplexDetailRoute(complexId)
    if (myComplexBackStack.lastOrNull() != route) {
      myComplexBackStack.add(route)
    }
  }

  fun openOwnerReceivedReviews() {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    if (myComplexBackStack.lastOrNull() != OwnerReceivedReviewsRoute) {
      myComplexBackStack.add(OwnerReceivedReviewsRoute)
    }
  }

  fun openAddCourt(complexId: String, complexName: String) {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    val detailRoute = ComplexDetailRoute(complexId)
    if (myComplexBackStack.lastOrNull() != detailRoute) {
      if (myComplexBackStack.none { it == detailRoute }) {
        myComplexBackStack.add(detailRoute)
      } else {
        while (myComplexBackStack.lastOrNull() != detailRoute && myComplexBackStack.size > 1) {
          myComplexBackStack.removeLastOrNull()
        }
      }
    }

    val route = AddCourtRoute(complexId = complexId, complexName = complexName)
    if (myComplexBackStack.lastOrNull() != route) {
      myComplexBackStack.add(route)
    }
  }

  fun returnToMyComplexRoot() {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    myComplexBackStack.clear()
    myComplexBackStack.add(MyComplexRoute)
    requestMyComplexHubReload()
  }

  fun openCourtAvailability(entrypoint: OwnerCourtAvailabilityEntrypoint) {
    ownerCourtAvailabilityEntrypointState.value = entrypoint
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    if (myComplexBackStack.lastOrNull() == CreateComplexRoute) {
      myComplexBackStack.removeLastOrNull()
    }
    // The create-complex step may have been opened from the catalog; drop it there too
    // so it does not linger in the mejenguero search stack after the owner is created.
    while (searchBackStack.lastOrNull() == CreateComplexRoute) {
      searchBackStack.removeLastOrNull()
    }

    val route =
        CourtAvailabilityRoute(
            courtId = entrypoint.courtId,
            courtName = entrypoint.courtName,
            complexName = entrypoint.complexName,
        )
    if (myComplexBackStack.lastOrNull() != route) {
      myComplexBackStack.add(route)
    }
  }

  fun closeAddCourtAfterSuccess() {
    if (myComplexBackStack.lastOrNull() is AddCourtRoute) {
      myComplexBackStack.removeLastOrNull()
      requestMyComplexHubReload()
    }
  }

  fun openOwnerCourtAvailabilityEntrypoint() {
    val entrypoint = ownerCourtAvailabilityEntrypointState.value ?: return
    openCourtAvailability(entrypoint)
  }

  fun closeCurrentDetail() {
    val currentDetail = currentBackStack.lastOrNull()
    if (currentBackStack.size > 1) {
      currentBackStack.removeLastOrNull()
      if (
          currentDetail is CourtAvailabilityRoute && currentBackStack.lastOrNull() == MyComplexRoute
      ) {
        requestMyComplexHubReload()
      }
    }
  }

  // Owner switches to the mejenguero (player) shell and lands on Buscar.
  fun switchToPlayerView() {
    showSearchRoot()
    requestCatalogReload()
  }

  // Owner returns to the owner shell (drawer) and lands on Mi complejo.
  fun switchToOwnerView() {
    showMyComplexRoot()
  }

  fun applyOwnerViewPreference(
      userId: String?,
      isOwner: Boolean,
      preference: OwnerViewPreference?,
  ) {
    if (!isOwner) {
      hydratedOwnerPreferenceUserIdState.value = null
      viewingAsPlayerState.value = true
      if (selectedRoute == AuthenticatedTopLevelRoute.MyComplex) {
        showSearchRoot()
      }
      return
    }

    val normalizedUserId = userId?.trim().orEmpty()
    if (normalizedUserId.isEmpty()) {
      hydratedOwnerPreferenceUserIdState.value = null
      return
    }
    if (hydratedOwnerPreferenceUserIdState.value == normalizedUserId) {
      return
    }

    when (preference) {
      OwnerViewPreference.OWNER -> showMyComplexRoot()
      OwnerViewPreference.PLAYER -> showSearchRoot()
      null -> Unit
    }
    hydratedOwnerPreferenceUserIdState.value = normalizedUserId
  }

  fun clearOwnerViewPreferenceHydration() {
    hydratedOwnerPreferenceUserIdState.value = null
  }

  fun reset() {
    ownerCourtAvailabilityEntrypointState.value = null
    myComplexHubReloadRequestKeyState.value = 0
    catalogReloadRequestKeyState.value = 0
    navigateTo(AuthenticatedTopLevelRoute.Search)
    searchBackStack.clear()
    searchBackStack.add(SearchRoute)
    reservationsBackStack.clear()
    reservationsBackStack.add(ReservationsRoute)
    notificationsBackStack.clear()
    notificationsBackStack.add(NotificationsRoute)
    myComplexBackStack.clear()
    myComplexBackStack.add(MyComplexRoute)
  }

  private fun requestMyComplexHubReload() {
    myComplexHubReloadRequestKeyState.value += 1
  }

  private fun requestCatalogReload() {
    catalogReloadRequestKeyState.value += 1
  }

  private fun showSearchRoot() {
    navigateTo(AuthenticatedTopLevelRoute.Search)
    searchBackStack.clear()
    searchBackStack.add(SearchRoute)
  }

  private fun showMyComplexRoot() {
    navigateTo(AuthenticatedTopLevelRoute.MyComplex)
    myComplexBackStack.clear()
    myComplexBackStack.add(MyComplexRoute)
  }

  // Sets the selected route and keeps viewingAsPlayer in sync with the route's domain:
  // - Player routes (Search, Notifications) → viewingAsPlayer = true
  // - Owner routes (MyComplex) → viewingAsPlayer = false
  // - Shared routes (Reservations) → viewingAsPlayer unchanged
  private fun navigateTo(route: AuthenticatedTopLevelRoute) {
    when (route) {
      AuthenticatedTopLevelRoute.Search,
      AuthenticatedTopLevelRoute.Notifications -> viewingAsPlayerState.value = true
      AuthenticatedTopLevelRoute.MyComplex -> viewingAsPlayerState.value = false
      AuthenticatedTopLevelRoute.Reservations -> {
        // Shared route: preserve the current viewingAsPlayer value.
      }
    }
    selectedRoute = route
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
