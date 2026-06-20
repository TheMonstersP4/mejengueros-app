package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel

private const val NavigationTransitionDurationMillis = 220
private const val NavigationTransitionOffsetDivisor = 8

private val appNavigationSavedStateConfiguration = SavedStateConfiguration {
  serializersModule = SerializersModule {
    polymorphic(NavKey::class) {
      subclass(LoginRoute::class, LoginRoute.serializer())
      subclass(RegisterRoute::class, RegisterRoute.serializer())
      subclass(VerifyAccountRoute::class, VerifyAccountRoute.serializer())
      subclass(HomeRoute::class, HomeRoute.serializer())
      subclass(KitRoute::class, KitRoute.serializer())
      subclass(AvailabilitySelectorsRoute::class, AvailabilitySelectorsRoute.serializer())
      subclass(PokedexRoute::class, PokedexRoute.serializer())
      subclass(PokemonDetailRoute::class, PokemonDetailRoute.serializer())
    }
  }
}

internal fun navigationTransitionOffset(fullWidth: Int): Int =
    maxOf(1, fullWidth / NavigationTransitionOffsetDivisor)

internal fun predictivePopDirectionMultiplier(swipeEdge: Int): Int =
    if (swipeEdge == NavigationEvent.EDGE_RIGHT) 1 else -1

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.forwardNavigationTransition():
    ContentTransform =
    slideInHorizontally(
        animationSpec = tween(NavigationTransitionDurationMillis),
        initialOffsetX = ::navigationTransitionOffset,
    ) togetherWith
        slideOutHorizontally(
            animationSpec = tween(NavigationTransitionDurationMillis),
            targetOffsetX = { fullWidth -> -navigationTransitionOffset(fullWidth) },
        )

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.backNavigationTransition(
    directionMultiplier: Int,
): ContentTransform =
    slideInHorizontally(
        animationSpec = tween(NavigationTransitionDurationMillis),
        initialOffsetX = { fullWidth ->
          navigationTransitionOffset(fullWidth) * directionMultiplier
        },
    ) togetherWith
        slideOutHorizontally(
            animationSpec = tween(NavigationTransitionDurationMillis),
            targetOffsetX = { fullWidth ->
              -navigationTransitionOffset(fullWidth) * directionMultiplier
            },
        )

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
          closeAuthStep = { if (loginBackStack.size > 1) loginBackStack.removeLastOrNull() },
          backToLogin = {
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
      )
  val shellActions =
      AuthenticatedShellActions(
          selectHome = authenticatedNavigationState::selectHome,
          selectKit = authenticatedNavigationState::selectKit,
          openAvailabilitySelectors = authenticatedNavigationState::openAvailabilitySelectors,
          closeCurrentDetail = authenticatedNavigationState::closeCurrentDetail,
          selectPokedex = authenticatedNavigationState::selectPokedex,
          signOut = {
            authViewModel.signOut()
            authenticatedNavigationState.reset()
            loginBackStack.clear()
            loginBackStack.add(LoginRoute)
          },
      )
  val pokedexActions =
      PokedexNavigationActions(
          openPokemonDetail = authenticatedNavigationState::openPokemonDetail,
          closeDetail = authenticatedNavigationState::closeCurrentDetail,
      )

  NavDisplay(
      backStack =
          if (authState.isAuthenticated) authenticatedNavigationState.currentBackStack
          else loginBackStack,
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                authViewModel = authViewModel,
                loginActions = loginActions,
                shellActions = shellActions,
                pokedexActions = pokedexActions,
            )
          },
      transitionSpec = { forwardNavigationTransition() },
      popTransitionSpec = { backNavigationTransition(directionMultiplier = -1) },
      predictivePopTransitionSpec = { swipeEdge ->
        backNavigationTransition(directionMultiplier = predictivePopDirectionMultiplier(swipeEdge))
      },
  )
}
