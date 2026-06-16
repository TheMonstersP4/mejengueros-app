package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository

class AuthRepository(private val localDataSource: IAuthLocalDataSource) : IAuthRepository {
  override fun getSession(): AuthSession? = localDataSource.getSession()

  override fun signIn(username: String): AuthSession {
    val session = AuthSession(username = username.trim())
    localDataSource.saveSession(session)
    return session
  }

  override fun signOut() {
    localDataSource.clearSession()
  }
}
