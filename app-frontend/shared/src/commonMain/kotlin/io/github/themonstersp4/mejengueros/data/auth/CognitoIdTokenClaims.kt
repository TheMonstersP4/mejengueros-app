package io.github.themonstersp4.mejengueros.data.auth

data class CognitoIdTokenClaims(
    val sub: String,
    val email: String,
    val displayName: String?,
    val provider: String?,
    val expiresAtEpochSeconds: Long,
)
