package io.github.themonstersp4.mejengueros.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.auth.AuthCallbackBus
import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: IAuthRepository,
    private val oauthBrowser: IOAuthBrowser,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    private val callbackUrls: SharedFlow<String> = AuthCallbackBus.callbackUrls,
    private val markCallbackConsumed: (String) -> Unit = AuthCallbackBus::markConsumed,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    restoreSession()
    observeCallbacks()
  }

  fun signInWithGoogle() {
    startSignIn(AuthProvider.Google)
  }

  fun signInWithMicrosoft() {
    startSignIn(AuthProvider.Microsoft)
  }

  fun cancelExternalAuth() {
    val state = _uiState.value
    if (!state.isExternalAuthInProgress) {
      return
    }

    errorReporter.reportRecoverableFailure(
        name = "auth_external_signin_cancelled",
        attributes =
            externalAuthReportAttributes(
                operation = "cancel_external_signin",
                provider = state.pendingProvider,
                stage = "pending_callback",
                outcome = "user_cancelled",
            ),
    )

    clearExternalAuthProgress()
  }

  fun clearFeedback() {
    _uiState.value =
        _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            isPasswordResetConfirmed = false,
            pendingProvider = null,
            isExternalAuthInProgress = false,
        )
  }

  fun signInWithEmail(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa tu correo y contraseña para continuar.",
          )
      return
    }

    runAuthAction {
      val session = authRepository.signInWithEmail(email = email, password = password)
      applyAuthenticatedSession(session)
    }
  }

  fun registerWithEmail(
      fullName: String,
      email: String,
      password: String,
      onCodeSent: () -> Unit,
  ) {
    if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa nombre, correo y contraseña para crear la cuenta.",
          )
      return
    }

    runAuthAction {
      authRepository.registerWithEmail(fullName = fullName, email = email, password = password)
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              emailInput = email.trim(),
              successMessage = "Revisa tu correo para confirmar la cuenta.",
              errorMessage = null,
          )
      onCodeSent()
    }
  }

  fun confirmRegistration(
      code: String,
      onConfirmed: () -> Unit,
  ) {
    val email = _uiState.value.emailInput
    if (email.isBlank() || code.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa el código enviado a tu correo.",
          )
      return
    }

    runAuthAction {
      authRepository.confirmRegistration(email = email, code = code)
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              codeInput = "",
              successMessage = "Cuenta confirmada. Ya puedes iniciar sesión.",
              errorMessage = null,
          )
      onConfirmed()
    }
  }

  fun resendRegistrationCode() {
    val email = _uiState.value.emailInput
    if (email.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa tu correo para reenviar el código.",
          )
      return
    }

    runAuthAction {
      authRepository.resendRegistrationCode(email)
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              successMessage = "Enviamos un nuevo código de verificación.",
              errorMessage = null,
          )
    }
  }

  fun requestPasswordReset(
      email: String,
      onCodeSent: () -> Unit,
  ) {
    if (email.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa tu correo para recuperar el acceso.",
          )
      return
    }

    runAuthAction {
      authRepository.requestPasswordReset(email)
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              emailInput = email.trim(),
              successMessage = "Si la cuenta existe, enviamos un código de recuperación.",
              errorMessage = null,
              isPasswordResetConfirmed = false,
          )
      onCodeSent()
    }
  }

  fun confirmPasswordReset(
      code: String,
      newPassword: String,
      onConfirmed: () -> Unit,
  ) {
    val email = _uiState.value.emailInput
    if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = "Ingresa el código y la nueva contraseña.",
          )
      return
    }

    runAuthAction {
      authRepository.confirmPasswordReset(
          email = email,
          code = code,
          newPassword = newPassword,
      )
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              codeInput = "",
              newPasswordInput = "",
              successMessage = "Contraseña actualizada. Ya puedes iniciar sesión.",
              errorMessage = null,
              isPasswordResetConfirmed = true,
          )
      onConfirmed()
    }
  }

  fun signOut() {
    coroutineScope.launch {
      val signOutRequest = authRepository.signOut()
      _uiState.value = AuthUiState(isRestoringSession = false)
      runCatching { oauthBrowser.open(signOutRequest.logoutUrl) }
    }
  }

  fun refreshProfileAfterOwnerTransition() {
    launchProfileRefresh(
        reportName = "auth_profile_refresh_failed",
        operation = "owner_transition",
    )
  }

  private fun restoreSession() {
    coroutineScope.launch {
      try {
        val session = authRepository.getSession()
        if (session != null) {
          applyAuthenticatedSession(session, isRestoredSession = true)
          refreshRestoredProfile()
        } else {
          clearStartupGates()
        }
      } catch (error: CancellationException) {
        if (currentCoroutineContext().isActive) {
          clearStartupGates()
        } else {
          throw error
        }
        return@launch
      } catch (error: Throwable) {
        errorReporter.reportRecoverableFailure(
            name = "auth_session_restore_failed",
            attributes = error.toReportAttributes(operation = "restore_session"),
        )
        clearStartupGates()
      }
    }
  }

  private fun refreshRestoredProfile() {
    launchProfileRefresh(
        reportName = "auth_restore_profile_sync_failed",
        operation = "restore_profile_sync",
        clearAuthenticatedStartupGateOnCompletion = true,
    )
  }

  private fun launchProfileRefresh(
      reportName: String,
      operation: String,
      clearAuthenticatedStartupGateOnCompletion: Boolean = false,
  ) {
    coroutineScope.launch {
      try {
        refreshProfileAndApplyOwnerRole()
      } catch (error: CancellationException) {
        if (!currentCoroutineContext().isActive) {
          throw error
        }
      } catch (error: Throwable) {
        errorReporter.reportRecoverableFailure(
            name = reportName,
            attributes = error.toReportAttributes(operation = operation),
        )
      } finally {
        if (clearAuthenticatedStartupGateOnCompletion && currentCoroutineContext().isActive) {
          finishAuthenticatedStartupResolution()
        }
      }
    }
  }

  private fun startSignIn(provider: AuthProvider) {
    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoading = true,
              pendingProvider = provider,
              isExternalAuthInProgress = true,
              errorMessage = null,
              successMessage = null,
              isPasswordResetConfirmed = false,
          )
      runCatching {
            val request = authRepository.createSignInRequest(provider)
            oauthBrowser.open(request.authorizationUrl)
          }
          .onFailure { error ->
            reportExternalAuthFailure(
                error = error,
                provider = provider,
                stage = "start",
                failureName = "auth_external_signin_start_failed",
                cancellationName = "auth_external_signin_start_cancelled",
                operation = "start_external_signin",
            )
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    pendingProvider = null,
                    isExternalAuthInProgress = false,
                    errorMessage = resolveAuthErrorMessage(error, "No se pudo iniciar sesión."),
                )
          }
    }
  }

  private fun runAuthAction(action: suspend () -> Unit) {
    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoading = true,
              pendingProvider = null,
              isExternalAuthInProgress = false,
              errorMessage = null,
              successMessage = null,
              isPasswordResetConfirmed = false,
          )
      runCatching { action() }
          .onFailure { error ->
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    pendingProvider = null,
                    isExternalAuthInProgress = false,
                    errorMessage =
                        resolveAuthErrorMessage(error, "No se pudo completar la solicitud."),
                    isPasswordResetConfirmed = false,
                )
          }
    }
  }

  private fun observeCallbacks() {
    coroutineScope.launch {
      callbackUrls.collect { callbackUrl ->
        _uiState.value =
            _uiState.value.copy(
                isLoading = true,
                isExternalAuthInProgress = true,
                errorMessage = null,
                successMessage = null,
                isPasswordResetConfirmed = false,
            )
        runCatching { authRepository.handleCallback(callbackUrl) }
            .onSuccess(::applyAuthenticatedSession)
            .onFailure { error ->
              reportExternalAuthFailure(
                  error = error,
                  provider = _uiState.value.pendingProvider,
                  stage = "callback",
                  failureName = "auth_external_signin_callback_failed",
                  cancellationName = "auth_external_signin_callback_cancelled",
                  operation = "handle_external_signin_callback",
              )
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false,
                      pendingProvider = null,
                      isExternalAuthInProgress = false,
                      errorMessage =
                          resolveAuthErrorMessage(
                              error,
                              "No se pudo finalizar el inicio de sesión.",
                          ),
                  )
            }
            .also { markCallbackConsumed(callbackUrl) }
      }
    }
  }

  private fun applyAuthenticatedSession(
      session: AuthSession,
      isRestoredSession: Boolean = false,
  ) {
    val profile = authRepository.getUserProfile()
    val isOwner = profile?.roles?.contains(UserRoleKind.OWNER) == true
    _uiState.value =
        _uiState.value.copy(
            userId = session.sub,
            email = session.email,
            displayName = session.displayName,
            provider = session.provider,
            isRestoringSession = false,
            isResolvingAuthenticatedStartup = isRestoredSession,
            isAuthenticated = true,
            isOwner = isOwner,
            isLoading = false,
            pendingProvider = null,
            isExternalAuthInProgress = false,
            errorMessage = null,
            successMessage = null,
            isPasswordResetConfirmed = false,
        )
  }

  private fun clearStartupGates() {
    _uiState.value =
        _uiState.value.copy(
            isRestoringSession = false,
            isResolvingAuthenticatedStartup = false,
        )
  }

  private fun finishAuthenticatedStartupResolution() {
    _uiState.value = _uiState.value.copy(isResolvingAuthenticatedStartup = false)
  }

  private fun clearExternalAuthProgress() {
    _uiState.value =
        _uiState.value.copy(
            isLoading = false,
            pendingProvider = null,
            isExternalAuthInProgress = false,
            errorMessage = null,
            successMessage = null,
            isPasswordResetConfirmed = false,
        )
  }

  private suspend fun refreshProfileAndApplyOwnerRole() {
    authRepository.refreshUserProfile()
    val profile = authRepository.getUserProfile()
    val isOwner = profile?.roles?.contains(UserRoleKind.OWNER) == true
    _uiState.value = _uiState.value.copy(isOwner = isOwner)
  }

  private fun resolveAuthErrorMessage(error: Throwable, fallback: String): String {
    val message = error.message.orEmpty()

    return when {
      message.contains("Failed to connect", ignoreCase = true) ||
          message.contains("timeout", ignoreCase = true) ||
          message.contains("timed out", ignoreCase = true) ||
          message.contains("Unable to resolve host", ignoreCase = true) ->
          "No pudimos conectar con el servicio. Revisá tu conexión e intentá de nuevo."
      message.isBlank() -> fallback
      else -> message
    }
  }

  private fun reportExternalAuthFailure(
      error: Throwable,
      provider: AuthProvider?,
      stage: String,
      failureName: String,
      cancellationName: String,
      operation: String,
  ) {
    if (error is CancellationException && !coroutineScope.isActive) {
      throw error
    }

    val isCancellation = error is CancellationException
    errorReporter.reportRecoverableFailure(
        name = if (isCancellation) cancellationName else failureName,
        attributes =
            externalAuthReportAttributes(
                operation = operation,
                provider = provider,
                stage = stage,
                outcome = if (isCancellation) "cancelled" else "failed",
            ) +
                if (isCancellation) mapOf("error_source" to "cancellation")
                else error.toReportAttributes(operation),
    )
  }
}

private fun externalAuthReportAttributes(
    operation: String,
    provider: AuthProvider?,
    stage: String,
    outcome: String,
): Map<String, String> = buildMap {
  put("operation", operation)
  put("stage", stage)
  put("outcome", outcome)
  provider?.let { put("provider", it.name.lowercase()) }
}

private fun Throwable.toReportAttributes(operation: String): Map<String, String> {
  val baseAttributes = mapOf("operation" to operation)

  return when (this) {
    is AppApiException ->
        baseAttributes +
            mapOf(
                "error_source" to "app_api",
                "status_code" to statusCode.toString(),
            )
    else -> baseAttributes + mapOf("error_source" to "unexpected")
  }
}
