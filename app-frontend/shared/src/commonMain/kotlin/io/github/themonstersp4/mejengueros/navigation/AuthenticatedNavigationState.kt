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
  val searchBackStack = rememberNavBackStack(savedStateConfiguration, SearchRoute)
  val reservationsBackStack = rememberNavBackStack(savedStateConfiguration, ReservationsRoute)
  val notificationsBackStack = rememberNavBackStack(savedStateConfiguration, NotificationsRoute)
  val myComplexBackStack = rememberNavBackStack(savedStateConfiguration, MyComplexRoute)

  val selectedRoute = rememberSaveable { mutableStateOf(AuthenticatedTopLevelRoute.Search) }
  val ownerCourtAvailabilityEntrypointState =
      rememberSaveable(stateSaver = ownerCourtAvailabilityEntrypointSaver()) {
        mutableStateOf<OwnerCourtAvailabilityEntrypoint?>(null)
      }

  return remember(
      searchBackStack,
      reservationsBackStack,
      notificationsBackStack,
      myComplexBackStack,
      selectedRoute,
      ownerCourtAvailabilityEntrypointState,
  ) {
    AuthenticatedNavigationState(
        selectedRoute = selectedRoute,
        searchBackStack = searchBackStack,
        reservationsBackStack = reservationsBackStack,
        notificationsBackStack = notificationsBackStack,
        myComplexBackStack = myComplexBackStack,
        ownerCourtAvailabilityEntrypointState = ownerCourtAvailabilityEntrypointState,
    )
  }
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
