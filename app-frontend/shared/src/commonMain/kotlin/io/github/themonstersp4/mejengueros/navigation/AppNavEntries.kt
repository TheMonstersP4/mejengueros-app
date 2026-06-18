package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListViewModel
import io.github.themonstersp4.mejengueros.screens.auth.LoginScreen
import io.github.themonstersp4.mejengueros.screens.home.HomeScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokedexScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokemonDetailScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.appEntries(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
    shellActions: AuthenticatedShellActions,
    pokedexActions: PokedexNavigationActions,
) {
  entry<LoginRoute> {
    LoginEntry(
        authViewModel = authViewModel,
        loginActions = loginActions,
    )
  }
  entry<HomeRoute> {
    HomeEntry(
        authViewModel = authViewModel,
        shellActions = shellActions,
    )
  }
  entry<PokedexRoute> {
    PokedexEntry(
        shellActions = shellActions,
        pokedexActions = pokedexActions,
    )
  }
  entry<PokemonDetailRoute> { route ->
    PokemonDetailEntry(
        route = route,
        shellActions = shellActions,
        pokedexActions = pokedexActions,
    )
  }
}

@Composable
private fun LoginEntry(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
) {
  val state by authViewModel.uiState.collectAsState()

  LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) loginActions.onSignedIn() }

  LoginScreen(
      state = state,
      onEmailSignIn = authViewModel::signInWithEmail,
      onGoogleSignIn = authViewModel::signInWithGoogle,
      onMicrosoftSignIn = authViewModel::signInWithMicrosoft,
      onForgotPassword = authViewModel::requestPasswordReset,
      onRegister = authViewModel::openRegistration,
  )
}

@Composable
private fun HomeEntry(
    authViewModel: AuthViewModel,
    shellActions: AuthenticatedShellActions,
) {
  val state by authViewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Home,
      onHomeSelected = shellActions.selectHome,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
  ) { contentPadding ->
    HomeScreen(username = state.title, contentPadding = contentPadding)
  }
}

@Composable
private fun PokedexEntry(
    shellActions: AuthenticatedShellActions,
    pokedexActions: PokedexNavigationActions,
) {
  val pokemonListViewModel = koinViewModel<PokemonListViewModel>()
  val pokemonListState by pokemonListViewModel.uiState.collectAsState()

  LaunchedEffect(Unit) { pokemonListViewModel.syncFavoriteStates() }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Pokedex,
      onHomeSelected = shellActions.selectHome,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
  ) { contentPadding ->
    PokedexScreen(
        state = pokemonListState,
        onPokemonClick = pokedexActions.openPokemonDetail,
        onFavoriteClick = pokemonListViewModel::toggleFavorite,
        onModeChange = pokemonListViewModel::setMode,
        onLoadMore = pokemonListViewModel::loadNextPage,
        onRefresh = pokemonListViewModel::refresh,
        onSearchQueryChange = pokemonListViewModel::updateSearchQuery,
        contentPadding = contentPadding,
    )
  }
}

@Composable
private fun PokemonDetailEntry(
    route: PokemonDetailRoute,
    shellActions: AuthenticatedShellActions,
    pokedexActions: PokedexNavigationActions,
) {
  val pokemonDetailViewModel =
      koinViewModel<PokemonDetailViewModel>(
          key = "pokemon-detail-${route.id}",
          parameters = { parametersOf(route.id) },
      )
  val pokemonDetailState by pokemonDetailViewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Pokedex,
      onHomeSelected = shellActions.selectHome,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      onNavigateBack = pokedexActions.closeDetail,
  ) { contentPadding ->
    PokemonDetailScreen(
        state = pokemonDetailState,
        onFavoriteClick = pokemonDetailViewModel::toggleFavorite,
        onRetry = pokemonDetailViewModel::loadPokemon,
        contentPadding = contentPadding,
    )
  }
}
