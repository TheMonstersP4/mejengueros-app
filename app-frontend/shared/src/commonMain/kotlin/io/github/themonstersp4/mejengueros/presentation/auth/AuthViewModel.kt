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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
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

  fun clearFeedback() {
    _uiState.value =
        _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            pendingProvider = null,
        )
  }

  fun signInWithEmail(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false,
              pendingProvider = null,
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
          applyAuthenticatedSession(session)
          refreshRestoredProfile()
        } else {
          finishSessionRestoration()
        }
      } catch (error: CancellationException) {
        if (currentCoroutineContext().isActive) {
          finishSessionRestoration()
        } else {
          throw error
        }
        return@launch
      } catch (error: Throwable) {
        errorReporter.reportRecoverableFailure(
            name = "auth_session_restore_failed",
            attributes = error.toReportAttributes(operation = "restore_session"),
        )
        finishSessionRestoration()
      }
    }
  }

  private fun refreshRestoredProfile() {
    launchProfileRefresh(
        reportName = "auth_restore_profile_sync_failed",
        operation = "restore_profile_sync",
    )
  }

  private fun launchProfileRefresh(reportName: String, operation: String) {
    coroutineScope.launch {
      runCatching { refreshProfileAndApplyOwnerRole() }
          .onFailure { error ->
            if (error is CancellationException) return@onFailure

            errorReporter.reportRecoverableFailure(
                name = reportName,
                attributes = error.toReportAttributes(operation = operation),
            )
          }
    }
  }

  private fun startSignIn(provider: AuthProvider) {
    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoading = true,
              pendingProvider = provider,
              errorMessage = null,
              successMessage = null,
          )
      runCatching {
            val request = authRepository.createSignInRequest(provider)
            oauthBrowser.open(request.authorizationUrl)
          }
          .onFailure { error ->
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    pendingProvider = null,
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
              errorMessage = null,
              successMessage = null,
          )
      runCatching { action() }
          .onFailure { error ->
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    pendingProvider = null,
                    errorMessage =
                        resolveAuthErrorMessage(error, "No se pudo completar la solicitud."),
                )
          }
    }
  }

  private fun observeCallbacks() {
    coroutineScope.launch {
      callbackUrls.collect { callbackUrl ->
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        runCatching { authRepository.handleCallback(callbackUrl) }
            .onSuccess(::applyAuthenticatedSession)
            .onFailure { error ->
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false,
                      pendingProvider = null,
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

  private fun applyAuthenticatedSession(session: AuthSession) {
    val profile = authRepository.getUserProfile()
    val isOwner = profile?.roles?.contains(UserRoleKind.OWNER) == true
    _uiState.value =
        _uiState.value.copy(
            userId = session.sub,
            email = session.email,
            displayName = session.displayName,
            provider = session.provider,
            isRestoringSession = false,
            isAuthenticated = true,
            isOwner = isOwner,
            isLoading = false,
            pendingProvider = null,
            errorMessage = null,
            successMessage = null,
        )
  }

  private fun finishSessionRestoration() {
    _uiState.value = _uiState.value.copy(isRestoringSession = false)
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
