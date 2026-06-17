package io.github.themonstersp4.mejengueros.presentation.auth

import io.github.themonstersp4.mejengueros.domain.model.AuthProvider

data class AuthUiState(
    val email: String = "",
    val displayName: String? = null,
    val provider: String? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val pendingProvider: AuthProvider? = null,
    val errorMessage: String? = null,
) {
  val title: String
    get() = displayName ?: email
}
