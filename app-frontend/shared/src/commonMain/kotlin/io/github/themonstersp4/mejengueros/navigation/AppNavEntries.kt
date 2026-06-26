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
import io.github.themonstersp4.mejengueros.screens.complexes.CreateComplexScreenActions
import io.github.themonstersp4.mejengueros.screens.home.HomeScreen
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokedexScreen
import io.github.themonstersp4.mejengueros.screens.pokedex.PokemonDetailScreen
import io.github.themonstersp4.mejengueros.ui.components.DefaultMejenguerosLocationPickerCenter
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerActions
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerOverlay
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
  val selectedLatitude = state.latitude
  val selectedLongitude = state.longitude
  val selectedLocation =
      if (selectedLatitude == null || selectedLongitude == null) {
        null
      } else {
        SelectedLocation(latitude = selectedLatitude, longitude = selectedLongitude)
      }
  val locationPickerCoordinator =
      rememberLocationPickerCoordinator(selectedLocation = selectedLocation) { confirmedLocation ->
        createComplexViewModel.updateSelectedLocation(
            latitude = confirmedLocation.latitude,
            longitude = confirmedLocation.longitude,
        )
      }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Home,
      onHomeSelected = shellActions.returnToHomeRoot,
      onKitSelected = shellActions.selectKit,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
      overlayVisible = locationPickerCoordinator.isOpen,
      overlayContent = {
        LocationPickerOverlayHost(
            coordinator = locationPickerCoordinator,
            selectedLocation = selectedLocation,
        )
      },
  ) { contentPadding ->
    CreateComplexScreen(
        state = state,
        contentPadding = contentPadding,
        actions =
            CreateComplexScreenActions(
                onRetryCatalogs = createComplexViewModel::refreshCatalogs,
                onRetryCantons = createComplexViewModel::retrySelectedProvinceCantons,
                onComplexNameChange = createComplexViewModel::updateComplexName,
                onProvinceSelected = createComplexViewModel::selectProvince,
                onCantonSelected = createComplexViewModel::selectCanton,
                onComplexAddressChange = createComplexViewModel::updateComplexAddress,
                onOpenLocationPicker = locationPickerCoordinator.open,
                onClearLocation = createComplexViewModel::clearSelectedLocation,
                onToggleComplexService = createComplexViewModel::toggleComplexService,
                onFirstCourtNameChange = createComplexViewModel::updateFirstCourtName,
                onToggleCourtService = createComplexViewModel::toggleCourtService,
                onNext = createComplexViewModel::goToFirstCourtStep,
                onBack = createComplexViewModel::goToComplexStep,
                onSubmit = createComplexViewModel::submit,
                onSuccessAcknowledged = {
                  createComplexViewModel.acknowledgeSuccess()
                  shellActions.returnToHomeRoot()
                },
            ),
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
  val locationPickerCoordinator =
      rememberLocationPickerCoordinator(selectedLocation = selectedLocation) { confirmedLocation ->
        selectedLatitude = confirmedLocation.latitude
        selectedLongitude = confirmedLocation.longitude
      }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Kit,
      onHomeSelected = shellActions.selectHome,
      onKitSelected = shellActions.selectKit,
      onPokedexSelected = shellActions.selectPokedex,
      onSignOut = shellActions.signOut,
      overlayVisible = locationPickerCoordinator.isOpen,
      overlayContent = {
        LocationPickerOverlayHost(
            coordinator = locationPickerCoordinator,
            selectedLocation = selectedLocation,
        )
      },
  ) { contentPadding ->
    ComponentKitScreen(
        contentPadding = contentPadding,
        onOpenAvailabilitySelectors = shellActions.openAvailabilitySelectors,
        selectedLocation = selectedLocation,
        onOpenLocationPicker = locationPickerCoordinator.open,
    )
  }
}

private data class LocationPickerCoordinator(
    val isOpen: Boolean,
    val open: () -> Unit,
    val actions: MejenguerosLocationPickerActions,
    val draftLocation: SelectedLocation,
)

@Composable
private fun rememberLocationPickerCoordinator(
    selectedLocation: SelectedLocation?,
    onLocationConfirmed: (SelectedLocation) -> Unit,
): LocationPickerCoordinator {
  var draftLatitude by rememberSaveable {
    mutableStateOf(DefaultMejenguerosLocationPickerCenter.latitude)
  }
  var draftLongitude by rememberSaveable {
    mutableStateOf(DefaultMejenguerosLocationPickerCenter.longitude)
  }
  var isLocationPickerOpen by rememberSaveable { mutableStateOf(false) }

  return LocationPickerCoordinator(
      isOpen = isLocationPickerOpen,
      open = {
        val initialLocation = selectedLocation ?: DefaultMejenguerosLocationPickerCenter
        draftLatitude = initialLocation.latitude
        draftLongitude = initialLocation.longitude
        isLocationPickerOpen = true
      },
      actions =
          MejenguerosLocationPickerActions(
              onDraftLocationChange = { updatedLocation ->
                draftLatitude = updatedLocation.latitude
                draftLongitude = updatedLocation.longitude
              },
              onConfirm = { confirmedLocation ->
                onLocationConfirmed(confirmedLocation)
                draftLatitude = confirmedLocation.latitude
                draftLongitude = confirmedLocation.longitude
                isLocationPickerOpen = false
              },
              onDismiss = { isLocationPickerOpen = false },
          ),
      draftLocation = SelectedLocation(latitude = draftLatitude, longitude = draftLongitude),
  )
}

@Composable
private fun LocationPickerOverlayHost(
    coordinator: LocationPickerCoordinator,
    selectedLocation: SelectedLocation?,
) {
  if (!coordinator.isOpen) return

  MejenguerosLocationPickerOverlay(
      state =
          MejenguerosLocationPickerState(
              draftLocation = coordinator.draftLocation,
              selectedLocation = selectedLocation,
          ),
      actions = coordinator.actions,
  )
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
