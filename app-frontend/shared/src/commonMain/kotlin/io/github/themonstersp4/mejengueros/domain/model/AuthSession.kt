package io.github.themonstersp4.mejengueros.domain.model

data class AuthSession(
    val sub: String,
    val email: String,
    val displayName: String?,
    val provider: String?,
    val idToken: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long,
)
