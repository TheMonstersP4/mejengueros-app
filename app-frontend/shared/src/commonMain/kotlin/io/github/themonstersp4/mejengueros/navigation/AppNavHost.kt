package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel

internal fun appNavigationSerializersModule(): SerializersModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(LoginRoute::class, LoginRoute.serializer())
    subclass(RegisterRoute::class, RegisterRoute.serializer())
    subclass(VerifyAccountRoute::class, VerifyAccountRoute.serializer())
    subclass(ForgotPasswordRoute::class, ForgotPasswordRoute.serializer())
    subclass(ResetPasswordRoute::class, ResetPasswordRoute.serializer())
    subclass(HomeRoute::class, HomeRoute.serializer())
    subclass(SearchRoute::class, SearchRoute.serializer())
    subclass(CatalogCourtDetailRoute::class, CatalogCourtDetailRoute.serializer())
    subclass(CatalogReservationRoute::class, CatalogReservationRoute.serializer())
    subclass(ReservationsRoute::class, ReservationsRoute.serializer())
    subclass(NotificationsRoute::class, NotificationsRoute.serializer())
    subclass(MyComplexRoute::class, MyComplexRoute.serializer())
    subclass(ComplexDetailRoute::class, ComplexDetailRoute.serializer())
    subclass(AddCourtRoute::class, AddCourtRoute.serializer())
    subclass(CreateComplexRoute::class, CreateComplexRoute.serializer())
    subclass(KitRoute::class, KitRoute.serializer())
    subclass(AvailabilitySelectorsRoute::class, AvailabilitySelectorsRoute.serializer())
    subclass(CourtAvailabilityRoute::class, CourtAvailabilityRoute.serializer())
    subclass(PokedexRoute::class, PokedexRoute.serializer())
    subclass(PokemonDetailRoute::class, PokemonDetailRoute.serializer())
  }
}

internal fun createAppNavigationSavedStateConfiguration() = SavedStateConfiguration {
  serializersModule = appNavigationSerializersModule()
}

private val appNavigationSavedStateConfiguration = createAppNavigationSavedStateConfiguration()

@Composable
fun AppNavHost() {
  val loginBackStack = rememberNavBackStack(appNavigationSavedStateConfiguration, LoginRoute)
  val authenticatedNavigationState =
      rememberAuthenticatedNavigationState(appNavigationSavedStateConfiguration)
  val authViewModel = koinViewModel<AuthViewModel>()
  val authState by authViewModel.uiState.collectAsState()

  val loginActions =
      LoginNavigationActions(
          onSignedIn = {
            authenticatedNavigationState.reset()
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
          openRegister = {
            if (loginBackStack.lastOrNull() != RegisterRoute) {
              loginBackStack.add(RegisterRoute)
            }
          },
          openVerification = {
            if (loginBackStack.lastOrNull() != VerifyAccountRoute) {
              loginBackStack.add(VerifyAccountRoute)
            }
          },
          openForgotPassword = {
            if (loginBackStack.lastOrNull() != ForgotPasswordRoute) {
              loginBackStack.add(ForgotPasswordRoute)
            }
          },
          openPasswordReset = {
            if (loginBackStack.lastOrNull() != ResetPasswordRoute) {
              loginBackStack.add(ResetPasswordRoute)
            }
          },
          closeAuthStep = { if (loginBackStack.size > 1) loginBackStack.removeLastOrNull() },
          backToLogin = {
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
      )
  val shellActions =
      AuthenticatedShellActions(
          selectSearch = authenticatedNavigationState::selectSearch,
          selectReservations = authenticatedNavigationState::selectReservations,
          selectNotifications = authenticatedNavigationState::selectNotifications,
          selectMyComplex = authenticatedNavigationState::selectMyComplex,
          returnToMyComplexRoot = authenticatedNavigationState::returnToMyComplexRoot,
          openCatalogCourtDetail = authenticatedNavigationState::openCatalogCourtDetail,
          openCatalogReservation = authenticatedNavigationState::openCatalogReservation,
          openComplexDetail = authenticatedNavigationState::openComplexDetail,
          openAddCourt = authenticatedNavigationState::openAddCourt,
          openCreateComplex = authenticatedNavigationState::openCreateComplex,
          openCourtAvailability = authenticatedNavigationState::openCourtAvailability,
          closeAddCourtAfterSuccess = authenticatedNavigationState::closeAddCourtAfterSuccess,
          closeCurrentDetail = authenticatedNavigationState::closeCurrentDetail,
          signOut = {
            authViewModel.signOut()
            authenticatedNavigationState.reset()
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
          isOwner = authState.isOwner,
      )

  NavDisplay(
      backStack =
          if (authState.isAuthenticated) authenticatedNavigationState.currentBackStack
          else loginBackStack,
      onBack = {
        if (authState.isAuthenticated) {
          authenticatedNavigationState.closeCurrentDetail()
        } else {
          loginActions.closeAuthStep()
        }
      },
      entryProvider =
          entryProvider {
            appEntries(
                authenticatedNavigationState = authenticatedNavigationState,
                authViewModel = authViewModel,
                loginActions = loginActions,
                shellActions = shellActions,
            )
          },
  )
}
