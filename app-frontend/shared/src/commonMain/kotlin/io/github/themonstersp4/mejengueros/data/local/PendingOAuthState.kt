package io.github.themonstersp4.mejengueros.data.local

import kotlinx.serialization.Serializable

@Serializable data class PendingOAuthState(val state: String, val codeVerifier: String)
