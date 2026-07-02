package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.model.UserProfile

interface IAuthRepository {
  suspend fun getSession(): AuthSession?

  fun getUserProfile(): UserProfile?

  suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest

  suspend fun registerWithEmail(fullName: String, email: String, password: String)

  suspend fun confirmRegistration(email: String, code: String)

  suspend fun resendRegistrationCode(email: String)

  suspend fun signInWithEmail(email: String, password: String): AuthSession

  suspend fun requestPasswordReset(email: String)

  suspend fun confirmPasswordReset(email: String, code: String, newPassword: String)

  suspend fun handleCallback(callbackUrl: String): AuthSession

  suspend fun signOut(): AuthSignOutRequest
}
