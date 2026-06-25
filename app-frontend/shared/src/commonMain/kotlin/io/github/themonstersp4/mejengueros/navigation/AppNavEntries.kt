package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListViewModel
import io.github.themonstersp4.mejengueros.screens.auth.ForgotPasswordScreen
import io.github.themonstersp4.mejengueros.screens.auth.LoginScreen
import io.github.themonstersp4.mejengueros.screens.auth.PasswordResetScreen
import io.github.themonstersp4.mejengueros.screens.auth.RegisterScreen
import io.github.themonstersp4.mejengueros.screens.auth.VerifyAccountScreen
import io.github.themonstersp4.mejengueros.screens.availability.AvailabilitySelectorsScreen
import io.github.themonstersp4.mejengueros.screens.complexes.CreateComplexScreen
import io.github.themonstersp4.mejengueros.screens.home.HomeScreen
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitDemoLocationPickerCenter
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitLocationPickerOverlay
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokedexScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokemonDetailScreen
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerActions
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerState
import io.github.themonstersp4.mejengueros.ui.components.SelectedLocation
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
  entry<RegisterRoute> {
    RegisterEntry(
        authViewModel = authViewModel,
        loginActions = loginActions,
    )
  }
  entry<VerifyAccountRoute> {
    VerifyAccountEntry(
        authViewModel = authViewModel,
        loginActions = loginActions,
    )
  }
  entry<ForgotPasswordRoute> {
    ForgotPasswordEntry(
        authViewModel = authViewModel,
        loginActions = loginActions,
    )
  }
  entry<ResetPasswordRoute> {
    PasswordResetEntry(
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
  entry<CreateComplexRoute> { CreateComplexEntry(shellActions = shellActions) }
  entry<KitRoute> { ComponentKitEntry(shellActions = shellActions) }
  entry<AvailabilitySelectorsRoute> { AvailabilitySelectorsEntry(shellActions = shellActions) }
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
      onForgotPassword = loginActions.openForgotPassword,
      onRegister = loginActions.openRegister,
  )
}

@Composable
private fun RegisterEntry(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
) {
  val state by authViewModel.uiState.collectAsState()

  RegisterScreen(
      state = state,
      onBackToLogin = loginActions.backToLogin,
      onRegister = { fullName, email, password ->
        authViewModel.registerWithEmail(
            fullName = fullName,
            email = email,
            password = password,
            onCodeSent = loginActions.openVerification,
        )
      },
  )
}

@Composable
private fun VerifyAccountEntry(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
) {
  val state by authViewModel.uiState.collectAsState()

  VerifyAccountScreen(
      state = state,
      onBackToRegister = loginActions.closeAuthStep,
      onBackToLogin = loginActions.backToLogin,
      onConfirmRegistration = { code ->
        authViewModel.confirmRegistration(
            code = code,
            onConfirmed = loginActions.backToLogin,
        )
      },
      onResendRegistrationCode = authViewModel::resendRegistrationCode,
  )
}

@Composable
private fun ForgotPasswordEntry(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
) {
  val state by authViewModel.uiState.collectAsState()

  LaunchedEffect(Unit) { authViewModel.clearFeedback() }

  ForgotPasswordScreen(
      state = state,
      onBackToLogin = loginActions.backToLogin,
      onSendCode = { email ->
        authViewModel.requestPasswordReset(
            email = email,
            onCodeSent = loginActions.openPasswordReset,
        )
      },
  )
}

@Composable
private fun PasswordResetEntry(
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
) {
  val state by authViewModel.uiState.collectAsState()
  val backToLogin = {
    authViewModel.clearFeedback()
    loginActions.backToLogin()
  }

  PasswordResetScreen(
      state = state,
      onBackToLogin = backToLogin,
      onConfirmPasswordReset = { code, newPassword ->
        authViewModel.confirmPasswordReset(
            code = code,
            newPassword = newPassword,
            onConfirmed = {},
        )
      },
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
      onKitSelected = shellActions.selectKit,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
  ) { contentPadding ->
    HomeScreen(
        username = state.title,
        contentPadding = contentPadding,
        onCreateComplex = shellActions.openCreateComplex,
    )
  }
}

@Composable
private fun CreateComplexEntry(
    shellActions: AuthenticatedShellActions,
) {
  val createComplexViewModel = koinViewModel<CreateComplexViewModel>()
  val state by createComplexViewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Home,
      onHomeSelected = shellActions.closeCurrentDetail,
      onKitSelected = shellActions.selectKit,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
  ) { contentPadding ->
    CreateComplexScreen(
        state = state,
        contentPadding = contentPadding,
        onComplexNameChange = createComplexViewModel::updateComplexName,
        onComplexAddressChange = createComplexViewModel::updateComplexAddress,
        onFirstCourtNameChange = createComplexViewModel::updateFirstCourtName,
        onSubmit = createComplexViewModel::submit,
    )
  }
}

@Composable
private fun ComponentKitEntry(
    shellActions: AuthenticatedShellActions,
) {
  var selectedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
  var selectedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
  val selectedLocation =
      if (selectedLatitude == null || selectedLongitude == null) {
        null
      } else {
        SelectedLocation(latitude = selectedLatitude!!, longitude = selectedLongitude!!)
      }
  var draftLatitude by rememberSaveable {
    mutableStateOf(ComponentKitDemoLocationPickerCenter.latitude)
  }
  var draftLongitude by rememberSaveable {
    mutableStateOf(ComponentKitDemoLocationPickerCenter.longitude)
  }
  val draftLocation = SelectedLocation(latitude = draftLatitude, longitude = draftLongitude)
  var isLocationPickerOpen by rememberSaveable { mutableStateOf(false) }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Kit,
      onHomeSelected = shellActions.selectHome,
      onKitSelected = shellActions.selectKit,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      overlayVisible = isLocationPickerOpen,
      overlayContent = {
        if (isLocationPickerOpen) {
          ComponentKitLocationPickerOverlay(
              state =
                  MejenguerosLocationPickerState(
                      draftLocation = draftLocation,
                      selectedLocation = selectedLocation,
                  ),
              actions =
                  MejenguerosLocationPickerActions(
                      onDraftLocationChange = { updatedLocation ->
                        draftLatitude = updatedLocation.latitude
                        draftLongitude = updatedLocation.longitude
                      },
                      onConfirm = { confirmedLocation ->
                        selectedLatitude = confirmedLocation.latitude
                        selectedLongitude = confirmedLocation.longitude
                        draftLatitude = confirmedLocation.latitude
                        draftLongitude = confirmedLocation.longitude
                        isLocationPickerOpen = false
                      },
                      onDismiss = { isLocationPickerOpen = false },
                  ),
          )
        }
      },
  ) { contentPadding ->
    ComponentKitScreen(
        contentPadding = contentPadding,
        onOpenAvailabilitySelectors = shellActions.openAvailabilitySelectors,
        selectedLocation = selectedLocation,
        onOpenLocationPicker = {
          val initialLocation = selectedLocation ?: ComponentKitDemoLocationPickerCenter
          draftLatitude = initialLocation.latitude
          draftLongitude = initialLocation.longitude
          isLocationPickerOpen = true
        },
    )
  }
}

@Composable
private fun AvailabilitySelectorsEntry(
    shellActions: AuthenticatedShellActions,
) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Kit,
      onHomeSelected = shellActions.selectHome,
      onKitSelected = shellActions.closeCurrentDetail,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
  ) { contentPadding ->
    AvailabilitySelectorsScreen(contentPadding = contentPadding)
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
      onKitSelected = shellActions.selectKit,
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
      onKitSelected = shellActions.selectKit,
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
