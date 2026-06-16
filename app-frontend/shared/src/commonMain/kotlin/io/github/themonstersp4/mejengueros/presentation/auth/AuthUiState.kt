package io.github.themonstersp4.mejengueros.presentation.auth

data class AuthUiState(
    val username: String = "",
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
)
