package io.github.themonstersp4.mejengueros.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.auth.AuthCallbackBus
import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: IAuthRepository,
    private val oauthBrowser: IOAuthBrowser,
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

  fun signInWithEmail(email: String, password: String) {
    val errorMessage =
        if (email.isBlank() || password.isBlank()) "Ingresá tu correo y contraseña."
        else
            "El inicio de sesión con correo y contraseña aún no está conectado. Usá Google o Microsoft por ahora."

    _uiState.value =
        _uiState.value.copy(
            isLoading = false,
            pendingProvider = null,
            errorMessage = errorMessage,
        )
  }

  fun requestPasswordReset() {
    _uiState.value =
        _uiState.value.copy(
            isLoading = false,
            pendingProvider = null,
            errorMessage = "La recuperación de contraseña aún no está conectada.",
        )
  }

  fun openRegistration() {
    _uiState.value =
        _uiState.value.copy(
            isLoading = false,
            pendingProvider = null,
            errorMessage = "El registro manual aún no está conectado.",
        )
  }

  fun signOut() {
    coroutineScope.launch {
      val signOutRequest = authRepository.signOut()
      _uiState.value = AuthUiState()
      runCatching { oauthBrowser.open(signOutRequest.logoutUrl) }
    }
  }

  private fun restoreSession() {
    coroutineScope.launch { authRepository.getSession()?.let(::applyAuthenticatedSession) }
  }

  private fun startSignIn(provider: AuthProvider) {
    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isLoading = true,
              pendingProvider = provider,
              errorMessage = null,
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
                    errorMessage = error.message ?: "Unable to start sign in.",
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
                      errorMessage = error.message ?: "Unable to finish sign in.",
                  )
            }
            .also { markCallbackConsumed(callbackUrl) }
      }
    }
  }

  private fun applyAuthenticatedSession(session: AuthSession) {
    _uiState.value =
        _uiState.value.copy(
            email = session.email,
            displayName = session.displayName,
            provider = session.provider,
            isAuthenticated = true,
            isLoading = false,
            pendingProvider = null,
            errorMessage = null,
        )
  }
}
