package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.notifications.NotificationsViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
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
    subclass(OwnerReceivedReviewsRoute::class, OwnerReceivedReviewsRoute.serializer())
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
  val secureStorage = koinInject<IAuthSecureStorage>()
  val authState by authViewModel.uiState.collectAsState()
  val currentUserId = authState.userId?.takeIf { it.isNotBlank() }
  val coroutineScope = rememberCoroutineScope()
  val startupOwnerPreferenceHydrationGate = remember { StartupOwnerPreferenceHydrationGate() }
  val ownerViewPreferenceCoordinator =
      remember(authenticatedNavigationState, secureStorage, coroutineScope) {
        OwnerViewPreferenceCoordinator(
            navigationState = authenticatedNavigationState,
            secureStorage = secureStorage,
            coroutineScope = coroutineScope,
        )
      }

  LaunchedEffect(
      authState.isRestoringSession,
      authState.isAuthenticated,
      authState.isOwner,
      authState.userId,
      authState.isResolvingAuthenticatedStartup,
  ) {
    val userId = currentUserId

    if (authState.isRestoringSession || authState.isResolvingAuthenticatedStartup) {
      if (authState.isAuthenticated && userId != null) {
        startupOwnerPreferenceHydrationGate.waitForAuthenticatedStartup(userId)
      } else {
        startupOwnerPreferenceHydrationGate.clear()
      }
      return@LaunchedEffect
    }

    if (!authState.isAuthenticated) {
      startupOwnerPreferenceHydrationGate.clear()
      ownerViewPreferenceCoordinator.hydrate(authState)
      return@LaunchedEffect
    }

    if (
        authState.isOwner &&
            userId != null &&
            startupOwnerPreferenceHydrationGate.isWaitingFor(userId)
    ) {
      startupOwnerPreferenceHydrationGate.beginOwnerPreferenceHydration(userId)
    } else if (!authState.isOwner || userId == null) {
      startupOwnerPreferenceHydrationGate.clear()
    }

    ownerViewPreferenceCoordinator.hydrate(authState)

    if (startupOwnerPreferenceHydrationGate.isHydratingFor(userId)) {
      startupOwnerPreferenceHydrationGate.clear()
    }
  }

  val isAwaitingStartupOwnerPreferenceHydration =
      authState.isAuthenticated &&
          authState.isOwner &&
          currentUserId != null &&
          startupOwnerPreferenceHydrationGate.isBlockingNavigationFor(currentUserId)

  if (
      authState.isRestoringSession ||
          authState.isResolvingAuthenticatedStartup ||
          isAwaitingStartupOwnerPreferenceHydration
  ) {
    AuthSessionRestorationScreen()
    return
  }

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

  if (!authState.isAuthenticated) {
    NavDisplay(
        backStack = loginBackStack,
        onBack = { loginActions.closeAuthStep() },
        entryProvider =
            entryProvider {
              authEntries(
                  authViewModel = authViewModel,
                  loginActions = loginActions,
              )
            },
    )
    return
  }

  val notificationsViewModel = koinViewModel<NotificationsViewModel>()
  val notificationsState by notificationsViewModel.uiState.collectAsState()
  val notificationUnreadCountState = rememberUpdatedState(notificationsState.unreadCount)
  val switchToPlayerView = { ownerViewPreferenceCoordinator.switchToPlayerView(authState) }
  val switchToOwnerView = { ownerViewPreferenceCoordinator.switchToOwnerView(authState) }

  LaunchedEffect(currentUserId) {
    val userId = currentUserId
    if (userId != null) {
      notificationsViewModel.activate(userId)
    } else {
      notificationsViewModel.deactivate()
    }
  }

  val shellActions =
      AuthenticatedShellActions(
          selectSearch = authenticatedNavigationState::selectSearch,
          selectReservations = authenticatedNavigationState::selectReservations,
          selectNotifications = {
            authenticatedNavigationState.selectNotifications()
            notificationsViewModel.refresh()
          },
          selectMyComplex = authenticatedNavigationState::selectMyComplex,
          returnToSearchRoot = authenticatedNavigationState::returnToSearchRoot,
          returnToMyComplexRoot = authenticatedNavigationState::returnToMyComplexRoot,
          openCatalogCourtDetail = authenticatedNavigationState::openCatalogCourtDetail,
          openCatalogReservation = authenticatedNavigationState::openCatalogReservation,
          openComplexDetail = authenticatedNavigationState::openComplexDetail,
          openAddCourt = authenticatedNavigationState::openAddCourt,
          openCreateComplex = authenticatedNavigationState::openCreateComplex,
          openCourtAvailability = authenticatedNavigationState::openCourtAvailability,
          openOwnerReceivedReviews = authenticatedNavigationState::openOwnerReceivedReviews,
          closeAddCourtAfterSuccess = authenticatedNavigationState::closeAddCourtAfterSuccess,
          closeCurrentDetail = authenticatedNavigationState::closeCurrentDetail,
          signOut = {
            authViewModel.signOut()
            authenticatedNavigationState.clearOwnerViewPreferenceHydration()
            authenticatedNavigationState.reset()
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
          refreshOwnerRole = authViewModel::refreshProfileAfterOwnerTransition,
          switchToPlayerView = switchToPlayerView,
          switchToOwnerView = switchToOwnerView,
          isOwner = authState.isOwner,
          viewingAsPlayer = authenticatedNavigationState.viewingAsPlayer,
          notificationUnreadCount = notificationUnreadCountState,
      )

  NavDisplay(
      backStack = authenticatedNavigationState.currentBackStack,
      onBack = { authenticatedNavigationState.closeCurrentDetail() },
      entryProvider =
          entryProvider {
            appEntries(
                authenticatedNavigationState = authenticatedNavigationState,
                authViewModel = authViewModel,
                notificationsViewModel = notificationsViewModel,
                loginActions = loginActions,
                shellActions = shellActions,
            )
          },
  )
}

private enum class StartupOwnerPreferenceHydrationPhase {
  Idle,
  WaitingForAuthenticatedStartup,
  HydratingOwnerPreference,
}

private class StartupOwnerPreferenceHydrationGate {
  private var phase by mutableStateOf(StartupOwnerPreferenceHydrationPhase.Idle)
  private var pendingUserId by mutableStateOf<String?>(null)

  fun waitForAuthenticatedStartup(userId: String) {
    pendingUserId = userId
    phase = StartupOwnerPreferenceHydrationPhase.WaitingForAuthenticatedStartup
  }

  fun beginOwnerPreferenceHydration(userId: String) {
    pendingUserId = userId
    phase = StartupOwnerPreferenceHydrationPhase.HydratingOwnerPreference
  }

  fun isWaitingFor(userId: String): Boolean {
    return phase == StartupOwnerPreferenceHydrationPhase.WaitingForAuthenticatedStartup &&
        pendingUserId == userId
  }

  fun isHydratingFor(userId: String?): Boolean {
    return phase == StartupOwnerPreferenceHydrationPhase.HydratingOwnerPreference &&
        pendingUserId == userId
  }

  fun isBlockingNavigationFor(userId: String?): Boolean {
    return pendingUserId == userId && phase != StartupOwnerPreferenceHydrationPhase.Idle
  }

  fun clear() {
    pendingUserId = null
    phase = StartupOwnerPreferenceHydrationPhase.Idle
  }
}

@Composable
internal fun AuthSessionRestorationScreen(modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.fillMaxSize().testTag("auth_session_restore_root"),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CircularProgressIndicator(modifier = Modifier.testTag("auth_session_restore_loading"))
        Text(
            text = "Restaurando tu sesión...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
      }
    }
  }
}
