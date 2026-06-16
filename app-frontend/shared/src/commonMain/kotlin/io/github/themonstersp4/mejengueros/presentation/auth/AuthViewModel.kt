package io.github.themonstersp4.mejengueros.presentation.auth

import androidx.lifecycle.ViewModel
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val authRepository: IAuthRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    restoreSession()
  }

  fun updateUsername(username: String) {
    _uiState.value = _uiState.value.copy(username = username, errorMessage = null)
  }

  fun signIn(): Boolean {
    val username = _uiState.value.username.trim()
    if (username.isEmpty()) {
      _uiState.value = _uiState.value.copy(errorMessage = "Enter a username to continue.")
      return false
    }

    val session = authRepository.signIn(username)
    _uiState.value =
        _uiState.value.copy(
            username = session.username,
            isAuthenticated = true,
            errorMessage = null,
        )
    return true
  }

  fun signOut() {
    authRepository.signOut()
    _uiState.value = AuthUiState()
  }

  private fun restoreSession() {
    val session = authRepository.getSession() ?: return
    _uiState.value =
        _uiState.value.copy(
            username = session.username,
            isAuthenticated = true,
            errorMessage = null,
        )
  }
}
