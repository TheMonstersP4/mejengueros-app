package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityUiState
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityViewModel
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexUiState
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationCardUiModel
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationsUiState
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationsViewModel
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationContext
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationViewModel
import io.github.themonstersp4.mejengueros.presentation.review.ReviewUiState
import io.github.themonstersp4.mejengueros.presentation.review.ReviewViewModel
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
import io.github.themonstersp4.mejengueros.screens.courtdetail.CourtDetailScreen
import io.github.themonstersp4.mejengueros.screens.home.HomeScreen
import io.github.themonstersp4.mejengueros.screens.mycomplex.ComplexDetailScreen
import io.github.themonstersp4.mejengueros.screens.mycomplex.MyComplexScreen
import io.github.themonstersp4.mejengueros.screens.placeholder.ProductPlaceholderScreen
import io.github.themonstersp4.mejengueros.screens.reservation.ReservationScreen
import io.github.themonstersp4.mejengueros.screens.reservation.ReservationScreenActions
import io.github.themonstersp4.mejengueros.screens.reservations.MyReservationsScreen
import io.github.themonstersp4.mejengueros.screens.reservations.MyReservationsScreenActions
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewReservationContext
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewScreen
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewScreenActions
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewUiMode
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewUiState
import io.github.themonstersp4.mejengueros.ui.components.CourtImagePickerController
import io.github.themonstersp4.mejengueros.ui.components.DefaultMejenguerosLocationPickerCenter
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosConfirmationDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLoadingDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerActions
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerOverlay
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationPickerState
import io.github.themonstersp4.mejengueros.ui.components.PlatformBackHandler
import io.github.themonstersp4.mejengueros.ui.components.ReviewEvidenceImagePickerController
import io.github.themonstersp4.mejengueros.ui.components.SelectedLocation
import io.github.themonstersp4.mejengueros.ui.components.rememberCourtImagePicker
import io.github.themonstersp4.mejengueros.ui.components.rememberReviewEvidenceImagePicker
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
  entry<SearchRoute> {
    SearchEntry(
        authenticatedNavigationState = authenticatedNavigationState,
        shellActions = shellActions,
    )
  }
  entry<CatalogCourtDetailRoute> { route ->
    CatalogCourtDetailEntry(route = route, shellActions = shellActions)
  }
  entry<CatalogReservationRoute> { route ->
    CatalogReservationEntry(route = route, shellActions = shellActions)
  }
  entry<ReservationsRoute> { ReservationsEntry(shellActions = shellActions) }
  entry<NotificationsRoute> { NotificationsEntry(shellActions = shellActions) }
  entry<MyComplexRoute> {
    MyComplexEntry(
        authenticatedNavigationState = authenticatedNavigationState,
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
  entry<CreateComplexRoute> {
    CreateComplexEntry(
        authenticatedNavigationState = authenticatedNavigationState,
        shellActions = shellActions,
    )
  }
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
      onCancelExternalAuth = authViewModel::cancelExternalAuth,
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
private fun SearchEntry(
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
) {
  val courtCatalogViewModel = koinViewModel<CourtCatalogViewModel>()
  val state by courtCatalogViewModel.uiState.collectAsState()

  CatalogReloadEffect(
      catalogReloadRequestKey = authenticatedNavigationState.catalogReloadRequestKey,
      onReloadRequested = courtCatalogViewModel::retryLoad,
  )

  SearchCatalogEntryContent(
      state = state,
      shellActions = shellActions,
      onSearchQueryChange = courtCatalogViewModel::updateSearchQuery,
      onProvinceSelected = courtCatalogViewModel::selectProvince,
      onCantonSelected = courtCatalogViewModel::selectCanton,
      onRetryLoad = courtCatalogViewModel::retryLoad,
  )
}

@Composable
internal fun SearchCatalogEntryContent(
    state: io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState,
    shellActions: AuthenticatedShellActions,
    onSearchQueryChange: (String) -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onCantonSelected: (String?) -> Unit,
    onRetryLoad: () -> Unit,
) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Search,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      isOwner = shellActions.isOwner,
      viewingAsPlayer = shellActions.viewingAsPlayer,
      onSwitchToPlayerView = shellActions.switchToPlayerView,
      onSwitchToOwnerView = shellActions.switchToOwnerView,
      chrome = AuthenticatedScaffoldChrome(title = "Buscar"),
      topBarActions = {
        if (!shellActions.isOwner) {
          IconButton(onClick = shellActions.openCreateComplex) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Crear complejo")
          }
        }
      },
  ) { contentPadding ->
    HomeScreen(
        state = state,
        contentPadding = contentPadding,
        onSearchQueryChange = onSearchQueryChange,
        onProvinceSelected = onProvinceSelected,
        onCantonSelected = onCantonSelected,
        onRetryLoad = onRetryLoad,
        onOpenCourtDetail = { court ->
          shellActions.openCatalogCourtDetail(
              CatalogCourtDetailRoute(
                  courtId = court.id,
                  complexId = court.complexId,
                  complexName = court.complexName,
                  courtName = court.courtName,
                  provinceName = court.provinceName,
                  cantonName = court.cantonName,
                  services = court.services,
                  ratingAverage = court.ratingAverage,
                  ratingCount = court.ratingCount,
                  imageUrl = court.imageUrl,
                  isReservableToday = court.isReservableToday,
              )
          )
        },
    )
  }
}

@Composable
private fun CatalogCourtDetailEntry(
    route: CatalogCourtDetailRoute,
    shellActions: AuthenticatedShellActions,
) {
  val viewModel =
      koinViewModel<CourtDetailViewModel>(
          key = "catalog-court-detail-${route.courtId}",
          parameters = { parametersOf(route.courtId) },
      )
  CatalogCourtDetailEntryContent(route = route, viewModel = viewModel, shellActions = shellActions)
}

@Composable
internal fun CatalogCourtDetailEntryContent(
    route: CatalogCourtDetailRoute,
    viewModel: CourtDetailViewModel,
    shellActions: AuthenticatedShellActions,
) {
  val state by viewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Search,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      isOwner = shellActions.isOwner,
      viewingAsPlayer = shellActions.viewingAsPlayer,
      onSwitchToPlayerView = shellActions.switchToPlayerView,
      onSwitchToOwnerView = shellActions.switchToOwnerView,
      chrome =
          AuthenticatedScaffoldChrome(
              title = "Detalle de cancha",
              onNavigateBack = shellActions.closeCurrentDetail,
          ),
  ) { contentPadding ->
    CourtDetailScreen(
        courtName = route.courtName,
        complexName = route.complexName,
        provinceName = route.provinceName,
        cantonName = route.cantonName,
        services = route.services,
        ratingAverage = route.ratingAverage,
        ratingCount = route.ratingCount,
        imageUrl = route.imageUrl,
        state = state,
        contentPadding = contentPadding,
        onRetryReviews = viewModel::retryLoadReviews,
        onReserve = {
          shellActions.openCatalogReservation(
              CatalogReservationRoute(
                  courtId = route.courtId,
                  complexId = route.complexId,
                  complexName = route.complexName,
                  courtName = route.courtName,
                  provinceName = route.provinceName,
                  cantonName = route.cantonName,
              )
          )
        },
        onRetrySlots = viewModel::retryLoad,
    )
  }
}

@Composable
internal fun CatalogReservationEntry(
    route: CatalogReservationRoute,
    shellActions: AuthenticatedShellActions,
) {
  val viewModel =
      koinViewModel<ReservationViewModel>(
          key = "catalog-reservation-${route.courtId}-${route.attemptId}",
          parameters = { parametersOf(route.toReservationContext()) },
      )
  val state by viewModel.uiState.collectAsState()

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Search,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      isOwner = shellActions.isOwner,
      viewingAsPlayer = shellActions.viewingAsPlayer,
      onSwitchToPlayerView = shellActions.switchToPlayerView,
      onSwitchToOwnerView = shellActions.switchToOwnerView,
      chrome =
          AuthenticatedScaffoldChrome(
              title = "Reservar",
              onNavigateBack = shellActions.closeCurrentDetail,
              showBottomBar = false,
          ),
  ) { contentPadding ->
    ReservationScreen(
        state = state,
        contentPadding = contentPadding,
        actions =
            ReservationScreenActions(
                onDateSelected = viewModel::selectDate,
                onSlotSelected = viewModel::selectSlot,
                onConfirmReservation = viewModel::confirmReservation,
                onRetryLoad = viewModel::retryLoad,
                onViewOtherHours = viewModel::viewOtherHours,
                onViewReservations = {
                  shellActions.returnToSearchRoot()
                  shellActions.selectReservations()
                },
                onReturnToCatalog = shellActions.returnToSearchRoot,
            ),
    )
  }
}

@Composable
private fun ReservationsEntry(shellActions: AuthenticatedShellActions) {
  ReservationsEntryContent(
      shellActions = shellActions,
      reservationsViewModel = koinViewModel(),
      reviewViewModel = koinViewModel(),
  )
}

@Composable
internal fun ReservationsEntryContent(
    shellActions: AuthenticatedShellActions,
    reservationsViewModel: MyReservationsViewModel,
    reviewViewModel: ReviewViewModel,
    reviewEvidenceImagePickerController: ReviewEvidenceImagePickerController? = null,
) {
  val reservationsState by reservationsViewModel.uiState.collectAsState()
  val reviewState by reviewViewModel.uiState.collectAsState()
  val reviewEvidenceImagePicker =
      reviewEvidenceImagePickerController
          ?: rememberReviewEvidenceImagePicker(reviewViewModel::updateSelectedEvidenceImage)

  LaunchedEffect(reviewEvidenceImagePicker.isAvailable) {
    reviewViewModel.updateEvidenceImagePickerAvailability(reviewEvidenceImagePicker.isAvailable)
  }

  var reviewEntryMode by rememberSaveable {
    mutableStateOf(ReservationReviewEntryMode.Launcher.name)
  }
  val currentMode = ReservationReviewEntryMode.valueOf(reviewEntryMode)
  val leaveReviewState = reviewState.toLeaveReviewUiState(currentMode)

  LaunchedEffect(reviewState.submittedReview?.id) {
    if (reviewState.submittedReview != null) {
      reviewEntryMode = ReservationReviewEntryMode.Success.name
      reservationsViewModel.refresh()
    }
  }

  fun resetReviewFlow() {
    reviewEntryMode = ReservationReviewEntryMode.Launcher.name
    reviewViewModel.resetFlow(reloadLatestReservation = false)
  }

  PlatformBackHandler(
      enabled = currentMode != ReservationReviewEntryMode.Launcher,
      onBack = ::resetReviewFlow,
  )

  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Reservations,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      isOwner = shellActions.isOwner,
      viewingAsPlayer = shellActions.viewingAsPlayer,
      onSwitchToPlayerView = shellActions.switchToPlayerView,
      onSwitchToOwnerView = shellActions.switchToOwnerView,
      chrome =
          when (currentMode) {
            ReservationReviewEntryMode.Launcher ->
                AuthenticatedScaffoldChrome(title = "Mis reservas")
            ReservationReviewEntryMode.Form ->
                AuthenticatedScaffoldChrome(
                    title = "DEJAR RESEÑA",
                    onNavigateBack = ::resetReviewFlow,
                    showBottomBar = false,
                )
            ReservationReviewEntryMode.Success ->
                AuthenticatedScaffoldChrome(
                    title = "DEJAR RESEÑA",
                    onNavigateBack = ::resetReviewFlow,
                    showBottomBar = false,
                )
          },
  ) { contentPadding ->
    when (currentMode) {
      ReservationReviewEntryMode.Launcher ->
          ReservationsLauncherContent(
              state = reservationsState,
              contentPadding = contentPadding,
              onReviewRequested = { card ->
                if (card.primaryActionKey == "leave_review") {
                  reviewViewModel.startReview(card.toReviewableReservation())
                  reviewEntryMode = ReservationReviewEntryMode.Form.name
                }
              },
              onRetryLoad = reservationsViewModel::refresh,
          )
      ReservationReviewEntryMode.Form,
      ReservationReviewEntryMode.Success ->
          leaveReviewState?.let { currentReviewState ->
            LeaveReviewScreen(
                state = currentReviewState,
                contentPadding = contentPadding,
                actions =
                    LeaveReviewScreenActions(
                        onRatingSelected = reviewViewModel::updateRating,
                        onCommentChanged = reviewViewModel::updateComment,
                        onPickEvidenceImage = reviewEvidenceImagePicker.launch,
                        onClearEvidenceImage = reviewViewModel::clearSelectedEvidenceImage,
                        onSubmit = reviewViewModel::submit,
                        onReturnToReservations = ::resetReviewFlow,
                        onExploreCourts = {
                          resetReviewFlow()
                          shellActions.selectSearch()
                        },
                    ),
            )
          }
    }
  }
}

@Composable
private fun ReservationsLauncherContent(
    state: MyReservationsUiState,
    contentPadding: PaddingValues,
    onReviewRequested: (MyReservationCardUiModel) -> Unit,
    onRetryLoad: () -> Unit,
) {
  MyReservationsScreen(
      state = state,
      contentPadding = contentPadding,
      actions =
          MyReservationsScreenActions(
              onRetryLoad = onRetryLoad,
              onReviewRequested = onReviewRequested,
          ),
  )
}

private enum class ReservationReviewEntryMode {
  Launcher,
  Form,
  Success,
}

private fun ReviewUiState.toLeaveReviewUiState(
    currentMode: ReservationReviewEntryMode
): LeaveReviewUiState? {
  val reservation = reviewableReservation ?: return null

  return LeaveReviewUiState(
      reservationContext = reservation.toReservationContext(),
      selectedRating = selectedRating,
      comment = comment,
      selectedEvidenceImage = selectedEvidenceImage,
      isEvidenceImagePickerAvailable = isEvidenceImagePickerAvailable,
      isSubmitting = isSubmitting,
      submitErrorMessage = submitErrorMessage,
      mode =
          if (currentMode == ReservationReviewEntryMode.Success) {
            LeaveReviewUiMode.Success
          } else {
            LeaveReviewUiMode.Form
          },
  )
}

private fun io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
    .toReservationContext(): LeaveReviewReservationContext = toLeaveReviewReservationContext()

private fun MyReservationCardUiModel.toReviewableReservation():
    io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation =
    io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation(
        reservationId = id,
        complexName = complexName,
        courtName = courtName,
        startsAt = startsAt,
        endsAt = endsAt,
        imageUrl = imageUrl,
    )

@Composable
private fun NotificationsEntry(shellActions: AuthenticatedShellActions) {
  AuthenticatedScaffold(
      selectedRoute = AuthenticatedTopLevelRoute.Notifications,
      onSearchSelected = shellActions.selectSearch,
      onReservationsSelected = shellActions.selectReservations,
      onNotificationsSelected = shellActions.selectNotifications,
      onMyComplexSelected = shellActions.selectMyComplex,
      onSignOut = shellActions.signOut,
      isOwner = shellActions.isOwner,
      viewingAsPlayer = shellActions.viewingAsPlayer,
      onSwitchToPlayerView = shellActions.switchToPlayerView,
      onSwitchToOwnerView = shellActions.switchToOwnerView,
      chrome = AuthenticatedScaffoldChrome(title = "Notificaciones"),
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
    shellActions: AuthenticatedShellActions,
) {
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

  MyComplexRouteContent(
      state = state,
      shellActions = shellActions,
      onRetry = myComplexViewModel::refresh,
  )
}

@Composable
internal fun MyComplexRouteContent(
    state: MyComplexUiState,
    shellActions: AuthenticatedShellActions,
    onRetry: () -> Unit,
) {
  OwnerRouteGuard(canRender = shellActions.isOwner, onUnauthorized = shellActions.selectSearch) {
    AuthenticatedScaffold(
        selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
        onSearchSelected = shellActions.selectSearch,
        onReservationsSelected = shellActions.selectReservations,
        onNotificationsSelected = shellActions.selectNotifications,
        onMyComplexSelected = shellActions.selectMyComplex,
        onSignOut = shellActions.signOut,
        isOwner = shellActions.isOwner,
        viewingAsPlayer = shellActions.viewingAsPlayer,
        onSwitchToPlayerView = shellActions.switchToPlayerView,
        onSwitchToOwnerView = shellActions.switchToOwnerView,
        chrome = AuthenticatedScaffoldChrome(title = "Mi complejo"),
    ) { contentPadding ->
      MyComplexEntryContent(
          state = state,
          contentPadding = contentPadding,
          onCreateComplex = shellActions.openCreateComplex,
          onRetry = onRetry,
          onOpenComplexDetail = shellActions.openComplexDetail,
      )
    }
  }
}

@Composable
internal fun MyComplexEntryContent(
    state: MyComplexUiState,
    contentPadding: PaddingValues,
    onCreateComplex: () -> Unit,
    onRetry: () -> Unit,
    onOpenComplexDetail: (String) -> Unit,
) {
  MyComplexScreen(
      state = state,
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
  val pickerCoordinator =
      remember(route.complexId, myComplexViewModel) {
        ComplexDetailCourtImagePickerCoordinator(
            complexId = route.complexId,
            updateCourtImage = myComplexViewModel::updateCourtImage,
        )
      }
  val courtImagePicker = rememberCourtImagePicker(pickerCoordinator::onImagePicked)
  LaunchedEffect(courtImagePicker.isAvailable) {
    myComplexViewModel.updateCourtImagePickerAvailability(courtImagePicker.isAvailable)
  }
  MyComplexInitialRefreshEffect(state = state, onInitialLoadRequested = myComplexViewModel::refresh)
  MyComplexHubReloadEffect(
      reloadRequestKey = authenticatedNavigationState.myComplexHubReloadRequestKey,
      onReloadRequested = myComplexViewModel::refresh,
  )

  ComplexDetailRouteContent(
      route = route,
      state = state,
      shellActions = shellActions,
      onRetry = myComplexViewModel::refresh,
      onAcknowledgeCourtImageSuccess = myComplexViewModel::acknowledgeCourtImageSuccess,
      onPickCourtImage = { courtId ->
        pickerCoordinator.onPickCourtImage(courtId, courtImagePicker.launch)
      },
  )
}

@Composable
internal fun ComplexDetailRouteContent(
    route: ComplexDetailRoute,
    state: MyComplexUiState,
    shellActions: AuthenticatedShellActions,
    onRetry: () -> Unit,
    onAcknowledgeCourtImageSuccess: () -> Unit = {},
    onPickCourtImage: (String) -> Unit = {},
) {
  OwnerRouteGuard(canRender = shellActions.isOwner, onUnauthorized = shellActions.selectSearch) {
    val complex = state.complexes.firstOrNull { it.id == route.complexId }

    AuthenticatedScaffold(
        selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
        onSearchSelected = shellActions.selectSearch,
        onReservationsSelected = shellActions.selectReservations,
        onNotificationsSelected = shellActions.selectNotifications,
        onMyComplexSelected = shellActions.returnToMyComplexRoot,
        onSignOut = shellActions.signOut,
        isOwner = shellActions.isOwner,
        viewingAsPlayer = shellActions.viewingAsPlayer,
        onSwitchToPlayerView = shellActions.switchToPlayerView,
        onSwitchToOwnerView = shellActions.switchToOwnerView,
        chrome =
            AuthenticatedScaffoldChrome(
                title = "Mi complejo",
                onNavigateBack = shellActions.closeCurrentDetail,
            ),
        topBarActions = {
          if (
              complex != null &&
                  shellActions.isOwner &&
                  !shellActions.viewingAsPlayer &&
                  !state.isLoading &&
                  state.errorMessage == null
          ) {
            val selectedComplex = complex
            IconButton(
                onClick = { shellActions.openAddCourt(selectedComplex.id, selectedComplex.name) },
                modifier =
                    Modifier.testTag("complex_detail_add_court_button_${selectedComplex.id}"),
            ) {
              Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar cancha")
            }
          }
        },
    ) { contentPadding ->
      ComplexDetailEntryContent(
          complex = complex,
          isLoading = state.isLoading,
          errorMessage = state.errorMessage,
          courtImageErrorMessage = state.courtImageErrorMessage,
          isCourtImagePickerAvailable = state.isCourtImagePickerAvailable,
          isUpdatingCourtImage = state.isUpdatingCourtImage,
          contentPadding = contentPadding,
          onRetry = onRetry,
          onConfigureAvailability = shellActions.openCourtAvailability,
          onPickCourtImage = onPickCourtImage,
      )

      MejenguerosLoadingDialog(
          visible = state.isUpdatingCourtImage,
          title = "Actualizando imagen",
          message = "Estamos subiendo y asociando la imagen de la cancha.",
      )

      state.courtImageSuccessMessage?.let { message ->
        MejenguerosConfirmationDialog(
            title = "Imagen actualizada",
            message = message,
            confirmText = "Aceptar",
            onConfirm = onAcknowledgeCourtImageSuccess,
            onDismissRequest = {},
            modifier = Modifier.testTag("complex_detail_image_success_dialog"),
        )
      }
    }
  }
}

@Composable
internal fun ComplexDetailEntryContent(
    complex: io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex?,
    isLoading: Boolean,
    errorMessage: String?,
    courtImageErrorMessage: String? = null,
    isCourtImagePickerAvailable: Boolean = false,
    isUpdatingCourtImage: Boolean = false,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit = {},
) {
  ComplexDetailScreen(
      complex = complex,
      isLoading = isLoading,
      errorMessage = errorMessage,
      courtImageErrorMessage = courtImageErrorMessage,
      isCourtImagePickerAvailable = isCourtImagePickerAvailable,
      isUpdatingCourtImage = isUpdatingCourtImage,
      contentPadding = contentPadding,
      onRetry = onRetry,
      onConfigureAvailability = onConfigureAvailability,
      onPickCourtImage = onPickCourtImage,
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
    courtImagePickerController: CourtImagePickerController? = null,
) {
  val state by viewModel.uiState.collectAsState()
  val courtImagePicker =
      courtImagePickerController ?: rememberCourtImagePicker(viewModel::updateSelectedCourtImage)

  LaunchedEffect(courtImagePicker.isAvailable) {
    viewModel.updateCourtImagePickerAvailability(courtImagePicker.isAvailable)
  }

  state.createdCourt?.let { createdCourt ->
    LaunchedEffect(createdCourt.id) {
      viewModel.acknowledgeSuccess()
      shellActions.closeAddCourtAfterSuccess()
    }
  }

  OwnerRouteGuard(canRender = shellActions.isOwner, onUnauthorized = shellActions.selectSearch) {
    AuthenticatedScaffold(
        selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
        onSearchSelected = shellActions.selectSearch,
        onReservationsSelected = shellActions.selectReservations,
        onNotificationsSelected = shellActions.selectNotifications,
        onMyComplexSelected = shellActions.returnToMyComplexRoot,
        onSignOut = shellActions.signOut,
        isOwner = shellActions.isOwner,
        viewingAsPlayer = shellActions.viewingAsPlayer,
        onSwitchToPlayerView = shellActions.switchToPlayerView,
        onSwitchToOwnerView = shellActions.switchToOwnerView,
        chrome =
            AuthenticatedScaffoldChrome(
                title = "Agregar cancha",
                onNavigateBack = shellActions.closeCurrentDetail,
            ),
    ) { contentPadding ->
      AddCourtScreen(
          state = state,
          contentPadding = contentPadding,
          actions =
              AddCourtScreenActions(
                  onRetryServices = viewModel::refreshServices,
                  onCourtNameChange = viewModel::updateCourtName,
                  onToggleService = viewModel::toggleCourtService,
                  onPickCourtImage = courtImagePicker.launch,
                  onClearCourtImage = { viewModel.updateSelectedCourtImage(null) },
                  onSubmit = viewModel::submit,
              ),
      )
    }
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
internal fun CatalogReloadEffect(
    catalogReloadRequestKey: Int,
    onReloadRequested: () -> Unit,
) {
  LaunchedEffect(catalogReloadRequestKey) {
    if (catalogReloadRequestKey > 0) {
      onReloadRequested()
    }
  }
}

@Composable
private fun CreateComplexEntry(
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
) {
  val createComplexViewModel = koinViewModel<CreateComplexViewModel>()

  CreateComplexEntryContent(
      authenticatedNavigationState = authenticatedNavigationState,
      shellActions = shellActions,
      createComplexViewModel = createComplexViewModel,
  )
}

@Composable
internal fun CreateComplexEntryContent(
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
    createComplexViewModel: CreateComplexViewModel,
    courtImagePickerController: CourtImagePickerController? = null,
) {
  val state by createComplexViewModel.uiState.collectAsState()
  val createdComplex = state.createdComplex
  if (createdComplex != null) {
    LaunchedEffect(createdComplex.firstCourtId) {
      createComplexViewModel.acknowledgeSuccess()
      shellActions.refreshOwnerRole()
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
  val courtImagePicker =
      courtImagePickerController
          ?: rememberCourtImagePicker(createComplexViewModel::updateSelectedCourtImage)

  LaunchedEffect(courtImagePicker.isAvailable) {
    createComplexViewModel.updateCourtImagePickerAvailability(courtImagePicker.isAvailable)
  }

  CreateComplexRouteContent(
      authenticatedNavigationState = authenticatedNavigationState,
      shellActions = shellActions,
      state = state,
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
      onPickCourtImage = courtImagePicker.launch,
      onClearCourtImage = { createComplexViewModel.updateSelectedCourtImage(null) },
      onNext = createComplexViewModel::goToFirstCourtStep,
      onBack = createComplexViewModel::goToComplexStep,
      onSubmit = createComplexViewModel::submit,
      onSuccessAcknowledged = {
        createComplexViewModel.acknowledgeSuccess()
        shellActions.returnToMyComplexRoot()
      },
      overlayVisible = locationPickerCoordinator.isOpen,
      overlayContent = {
        LocationPickerOverlayHost(
            coordinator = locationPickerCoordinator,
            selectedLocation = selectedLocation,
        )
      },
  )
}

@Composable
internal fun CreateComplexRouteContent(
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
    state: CreateComplexUiState,
    onRetryCatalogs: () -> Unit,
    onRetryCantons: () -> Unit,
    onComplexNameChange: (String) -> Unit,
    onProvinceSelected: (String) -> Unit,
    onCantonSelected: (String) -> Unit,
    onComplexAddressChange: (String) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onClearLocation: () -> Unit,
    onToggleComplexService: (String) -> Unit,
    onFirstCourtNameChange: (String) -> Unit,
    onToggleCourtService: (String) -> Unit,
    onPickCourtImage: () -> Unit,
    onClearCourtImage: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onSuccessAcknowledged: () -> Unit,
    overlayVisible: Boolean,
    overlayContent: @Composable () -> Unit,
) {
  OwnerRouteGuard(
      canRender = canRenderCreateComplexRoute(authenticatedNavigationState, shellActions),
      onUnauthorized = shellActions.selectSearch,
  ) {
    AuthenticatedScaffold(
        selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
        onSearchSelected = shellActions.selectSearch,
        onReservationsSelected = shellActions.selectReservations,
        onNotificationsSelected = shellActions.selectNotifications,
        onMyComplexSelected = shellActions.returnToMyComplexRoot,
        onSignOut = shellActions.signOut,
        isOwner = shellActions.isOwner,
        viewingAsPlayer = shellActions.viewingAsPlayer,
        onSwitchToPlayerView = shellActions.switchToPlayerView,
        onSwitchToOwnerView = shellActions.switchToOwnerView,
        chrome =
            AuthenticatedScaffoldChrome(
                title = "Mi complejo",
                onNavigateBack = shellActions.closeCurrentDetail,
            ),
        overlayVisible = overlayVisible,
        overlayContent = overlayContent,
    ) { contentPadding ->
      CreateComplexScreen(
          state = state,
          contentPadding = contentPadding,
          actions =
              CreateComplexScreenActions(
                  onRetryCatalogs = onRetryCatalogs,
                  onRetryCantons = onRetryCantons,
                  onComplexNameChange = onComplexNameChange,
                  onProvinceSelected = onProvinceSelected,
                  onCantonSelected = onCantonSelected,
                  onComplexAddressChange = onComplexAddressChange,
                  onOpenLocationPicker = onOpenLocationPicker,
                  onClearLocation = onClearLocation,
                  onToggleComplexService = onToggleComplexService,
                  onFirstCourtNameChange = onFirstCourtNameChange,
                  onToggleCourtService = onToggleCourtService,
                  onPickCourtImage = onPickCourtImage,
                  onClearCourtImage = onClearCourtImage,
                  onNext = onNext,
                  onBack = onBack,
                  onSubmit = onSubmit,
                  onSuccessAcknowledged = onSuccessAcknowledged,
              ),
      )
    }
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

  CourtAvailabilityRouteContent(state = state, viewModel = viewModel, shellActions = shellActions)
}

@Composable
internal fun CourtAvailabilityRouteContent(
    state: CourtAvailabilityUiState,
    viewModel: CourtAvailabilityViewModel,
    shellActions: AuthenticatedShellActions,
) {
  OwnerRouteGuard(canRender = shellActions.isOwner, onUnauthorized = shellActions.selectSearch) {
    AuthenticatedScaffold(
        selectedRoute = AuthenticatedTopLevelRoute.MyComplex,
        onSearchSelected = shellActions.selectSearch,
        onReservationsSelected = shellActions.selectReservations,
        onNotificationsSelected = shellActions.selectNotifications,
        onMyComplexSelected = shellActions.returnToMyComplexRoot,
        onSignOut = shellActions.signOut,
        isOwner = shellActions.isOwner,
        viewingAsPlayer = shellActions.viewingAsPlayer,
        onSwitchToPlayerView = shellActions.switchToPlayerView,
        onSwitchToOwnerView = shellActions.switchToOwnerView,
        chrome =
            AuthenticatedScaffoldChrome(
                title = state.appBarTitle,
                onNavigateBack = shellActions.closeCurrentDetail,
            ),
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
}

private fun CatalogReservationRoute.toReservationContext(): ReservationContext =
    ReservationContext(
        courtId = courtId,
        complexId = complexId,
        complexName = complexName,
        courtName = courtName,
        provinceName = provinceName,
        cantonName = cantonName,
    )

@Composable
private fun OwnerRouteGuard(
    canRender: Boolean,
    onUnauthorized: () -> Unit,
    content: @Composable () -> Unit,
) {
  if (!canRender) {
    LaunchedEffect(onUnauthorized) { onUnauthorized() }
    return
  }

  content()
}

internal fun canRenderCreateComplexRoute(
    authenticatedNavigationState: AuthenticatedNavigationState,
    shellActions: AuthenticatedShellActions,
): Boolean =
    shellActions.isOwner ||
        authenticatedNavigationState.selectedRoute == AuthenticatedTopLevelRoute.Search

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
