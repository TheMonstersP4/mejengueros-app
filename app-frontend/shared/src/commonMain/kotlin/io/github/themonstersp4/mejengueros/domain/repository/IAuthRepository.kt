package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.AuthSession

interface IAuthRepository {
  fun getSession(): AuthSession?

  fun signIn(username: String): AuthSession

  fun signOut()
}
