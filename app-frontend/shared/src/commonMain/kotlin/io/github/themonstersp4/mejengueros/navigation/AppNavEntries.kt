package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.PaddingValues
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
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexViewModel
import io.github.themonstersp4.mejengueros.screens.auth.ForgotPasswordScreen
import io.github.themonstersp4.mejengueros.screens.auth.LoginScreen
import io.github.themonstersp4.mejengueros.screens.auth.PasswordResetScreen
import io.github.themonstersp4.mejengueros.screens.auth.RegisterScreen
import io.github.themonstersp4.mejengueros.screens.auth.VerifyAccountScreen
import io.github.themonstersp4.mejengueros.screens.availability.CourtAvailabilityScreen
import io.github.themonstersp4.mejengueros.screens.availability.CourtAvailabilityScreenActions
import io.github.themonstersp4.mejengueros.screens.complexes.AddCourtScreen
import io.github.themonstersp4.mejengueros.screens.complexes.AddCourtScreenActions
import io.github.themonstersp4.mejengueros.screens.complexes.CreateComplexScreen
import io.github.themonstersp4.mejengueros.screens.complexes.CreateComplexScreenActions
import io.github.themonstersp4.mejengueros.screens.mycomplex.ComplexDetailScreen
import io.github.themonstersp4.mejengueros.screens.mycomplex.MyComplexScreen
import io.github.themonstersp4.mejengueros.screens.placeholder.ProductPlaceholderScreen
import io.github.themonstersp4.mejengueros.ui.components.DefaultMejenguerosLocationPickerCenter
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerActions
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerOverlay
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerState
import io.github.themonstersp4.mejengueros.ui.components.SelectedLocation
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.appEntries(
    authenticatedNavigationState: AuthenticatedNavigationState,
    authViewModel: AuthViewModel,
    loginActions: LoginNavigationActions,
    shellActions: AuthenticatedShellActions,
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
  entry<SearchRoute> { SearchEntry(shellActions = shellActions) }
  entry<ReservationsRoute> { ReservationsEntry(shellActions = shellActions) }
  entry<NotificationsRoute> { NotificationsEntry(shellActions = shellActions) }
  entry<MyComplexRoute> {
    MyComplexEntry(
        authenticatedNavigationState = authenticatedNavigationState,
        authViewModel = authViewModel,
        shellActions = shellActions,
    )
  }
  entry<ComplexDetailRoute> { route ->
    ComplexDetailEntry(
        route = route,
        authenticatedNavigationState = authenticatedNavigationState,
        shellActions = shellActions,
    )
  }
  entry<AddCourtRoute> { route -> AddCourtEntry(route = route, shellActions = shellActions) }
  entry<CreateComplexRoute> { CreateComplexEntry(shellActions = shellActions) }
  entry<CourtAvailabilityRoute> { route ->
    CourtAvailabilityEntry(route = route, shellActions = shellActions)
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
private fun SearchEntry(shellActions: AuthenticatedShellActions) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Search,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      title = "Buscar",
  ) { contentPadding ->
    ProductPlaceholderScreen(
        title = "Buscar",
        description =
            "Aquí aparecerán los complejos y canchas disponibles para reservar. Esta sección todavía está en construcción.",
        contentPadding = contentPadding,
    )
  }
}

@Composable
private fun ReservationsEntry(shellActions: AuthenticatedShellActions) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Reservations,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      title = "Reservas",
  ) { contentPadding ->
    ProductPlaceholderScreen(
        title = "Reservas",
        description =
            "Pronto vas a poder revisar tus reservas activas e historial desde aquí. Por ahora esta área es un placeholder controlado.",
        contentPadding = contentPadding,
    )
  }
}

@Composable
private fun NotificationsEntry(shellActions: AuthenticatedShellActions) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Notifications,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      title = "Notificaciones",
  ) { contentPadding ->
    ProductPlaceholderScreen(
        title = "Notificaciones",
        description =
            "Las alertas y recordatorios del producto llegarán a esta sección cuando la funcionalidad esté lista.",
        contentPadding = contentPadding,
    )
  }
}

@Composable
private fun MyComplexEntry(
    authenticatedNavigationState: AuthenticatedNavigationState,
    authViewModel: AuthViewModel,
    shellActions: AuthenticatedShellActions,
) {
  val authState by authViewModel.uiState.collectAsState()
  val myComplexViewModel = koinViewModel<MyComplexViewModel>()
  val state by myComplexViewModel.uiState.collectAsState()
  MyComplexInitialRefreshEffect(
      state = state,
      onInitialLoadRequested = myComplexViewModel::refresh,
  )
  MyComplexHubReloadEffect(
      reloadRequestKey = authenticatedNavigationState.myComplexHubReloadRequestKey,
      onReloadRequested = myComplexViewModel::refresh,
  )

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      title = "Mi complejo",
  ) { contentPadding ->
    MyComplexEntryContent(
        state = state,
        username = authState.title,
        contentPadding = contentPadding,
        onCreateComplex = shellActions.openCreateComplex,
        onRetry = myComplexViewModel::refresh,
        onOpenComplexDetail = shellActions.openComplexDetail,
    )
  }
}

@Composable
internal fun MyComplexEntryContent(
    state: MyComplexUiState,
    username: String,
    contentPadding: PaddingValues,
    onCreateComplex: () -> Unit,
    onRetry: () -> Unit,
    onOpenComplexDetail: (String) -> Unit,
) {
  MyComplexScreen(
      state = state,
      username = username,
      contentPadding = contentPadding,
      onCreateComplex = onCreateComplex,
      onRetry = onRetry,
      onOpenComplexDetail = onOpenComplexDetail,
  )
}

@Composable
private fun ComplexDetailEntry(
    route: ComplexDetailRoute,
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
) {
  val myComplexViewModel = koinViewModel<MyComplexViewModel>()
  val state by myComplexViewModel.uiState.collectAsState()
  MyComplexInitialRefreshEffect(state = state, onInitialLoadRequested = myComplexViewModel::refresh)
  MyComplexHubReloadEffect(
      reloadRequestKey = authenticatedNavigationState.myComplexHubReloadRequestKey,
      onReloadRequested = myComplexViewModel::refresh,
  )

  val complex = state.complexes.firstOrNull { it.id == route.complexId }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.returnToMyComplexRoot,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
      title = complex?.name ?: "Mi complejo",
  ) { contentPadding ->
    ComplexDetailEntryContent(
        complex = complex,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        contentPadding = contentPadding,
        onRetry = myComplexViewModel::refresh,
        onAddCourt = shellActions.openAddCourt,
        onConfigureAvailability = shellActions.openCourtAvailability,
    )
  }
}

@Composable
internal fun ComplexDetailEntryContent(
    complex: io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex?,
    isLoading: Boolean,
    errorMessage: String?,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onAddCourt: (String, String) -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  ComplexDetailScreen(
      complex = complex,
      isLoading = isLoading,
      errorMessage = errorMessage,
      contentPadding = contentPadding,
      onRetry = onRetry,
      onAddCourt = onAddCourt,
      onConfigureAvailability = onConfigureAvailability,
  )
}

@Composable
private fun AddCourtEntry(route: AddCourtRoute, shellActions: AuthenticatedShellActions) {
  val viewModel =
      koinViewModel<AddCourtViewModel>(
          key = "add-court-${route.complexId}",
          parameters = { parametersOf(route.complexId, route.complexName) },
      )

  AddCourtEntryContent(viewModel = viewModel, shellActions = shellActions)
}

@Composable
internal fun AddCourtEntryContent(
    viewModel: AddCourtViewModel,
    shellActions: AuthenticatedShellActions,
) {
  val state by viewModel.uiState.collectAsState()

  state.createdCourt?.let { createdCourt ->
    LaunchedEffect(createdCourt.id) {
      viewModel.acknowledgeSuccess()
      shellActions.closeAddCourtAfterSuccess()
    }
  }

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.returnToMyComplexRoot,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
      title = "Agregar cancha",
  ) { contentPadding ->
    AddCourtScreen(
        state = state,
        contentPadding = contentPadding,
        actions =
            AddCourtScreenActions(
                onRetryServices = viewModel::refreshServices,
                onCourtNameChange = viewModel::updateCourtName,
                onToggleService = viewModel::toggleCourtService,
                onSubmit = viewModel::submit,
            ),
    )
  }
}

@Composable
internal fun MyComplexInitialRefreshEffect(
    state: MyComplexUiState,
    onInitialLoadRequested: () -> Unit,
) {
  LaunchedEffect(state.isLoading, state.complexes, state.errorMessage) {
    if (state.isLoading && state.complexes.isEmpty() && state.errorMessage == null) {
      onInitialLoadRequested()
    }
  }
}

@Composable
internal fun MyComplexHubReloadEffect(
    reloadRequestKey: Int,
    onReloadRequested: () -> Unit,
) {
  LaunchedEffect(reloadRequestKey) {
    if (reloadRequestKey > 0) {
      onReloadRequested()
    }
  }
}

@Composable
private fun CreateComplexEntry(
    shellActions: AuthenticatedShellActions,
) {
  val createComplexViewModel = koinViewModel<CreateComplexViewModel>()
  val state by createComplexViewModel.uiState.collectAsState()
  val createdComplex = state.createdComplex
  if (createdComplex != null) {
    LaunchedEffect(createdComplex.firstCourtId) {
      createComplexViewModel.acknowledgeSuccess()
      shellActions.openCourtAvailability(
          OwnerCourtAvailabilityEntrypoint(
              courtId = createdComplex.firstCourtId,
              courtName = createdComplex.firstCourtName,
              complexName = createdComplex.complexName,
          )
      )
    }
    return
  }
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
      selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.returnToMyComplexRoot,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
      title = "Mi complejo",
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
                  shellActions.returnToMyComplexRoot()
                },
            ),
    )
  }
}

@Composable
private fun CourtAvailabilityEntry(
    route: CourtAvailabilityRoute,
    shellActions: AuthenticatedShellActions,
) {
  val viewModel =
      koinViewModel<CourtAvailabilityViewModel>(
          key = "court-availability-${route.courtId}",
          parameters = { parametersOf(route.courtId, route.courtName, route.complexName) },
      )

  CourtAvailabilityEntryContent(viewModel = viewModel, shellActions = shellActions)
}

@Composable
internal fun CourtAvailabilityEntryContent(
    viewModel: CourtAvailabilityViewModel,
    shellActions: AuthenticatedShellActions,
) {
  val state by viewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.returnToMyComplexRoot,
      onSignOut = shellActions.signOut,
      onNavigateBack = shellActions.closeCurrentDetail,
      title = state.appBarTitle,
  ) { contentPadding ->
    CourtAvailabilityScreen(
        state = state,
        contentPadding = contentPadding,
        actions =
            CourtAvailabilityScreenActions(
                onToggleDay = viewModel::toggleDay,
                onStartTimeSelected = viewModel::updateStartTime,
                onEndTimeSelected = viewModel::updateEndTime,
                onRetry = viewModel::load,
                onSave = viewModel::save,
                onSuccessAcknowledged =
                    availabilitySuccessAcknowledgedHandler(
                        viewModel = viewModel,
                        shellActions = shellActions,
                    ),
            ),
    )
  }
}

internal fun availabilitySuccessAcknowledgedHandler(
    viewModel: CourtAvailabilityViewModel,
    shellActions: AuthenticatedShellActions,
): () -> Unit = {
  viewModel.acknowledgeSuccess()
  shellActions.returnToMyComplexRoot()
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
