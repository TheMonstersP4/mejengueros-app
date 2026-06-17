package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest

interface IAuthRepository {
  suspend fun getSession(): AuthSession?

  suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest

  suspend fun handleCallback(callbackUrl: String): AuthSession

  suspend fun signOut(): AuthSignOutRequest
}
