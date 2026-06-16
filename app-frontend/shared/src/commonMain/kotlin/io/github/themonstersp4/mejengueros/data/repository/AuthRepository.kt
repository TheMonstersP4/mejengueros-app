package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.auth.CognitoOAuthRequestFactory
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.JwtIdTokenDecoder
import io.github.themonstersp4.mejengueros.data.auth.OAuthCallbackParser
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository

class AuthRepository(
    private val localDataSource: IAuthLocalDataSource,
    private val remoteDataSource: IAuthRemoteDataSource,
    private val requestFactory: CognitoOAuthRequestFactory,
    private val pkceGenerator: PkceGenerator,
    private val randomStringGenerator: IRandomStringGenerator,
    private val callbackParser: OAuthCallbackParser,
    private val idTokenDecoder: JwtIdTokenDecoder,
) : IAuthRepository {
  override suspend fun getSession(): AuthSession? = localDataSource.getSession()

  override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest {
    val state = randomStringGenerator.generate(StateLength)
    val pkce = pkceGenerator.generate()
    localDataSource.saveOAuthState(
        PendingOAuthState(state = state, codeVerifier = pkce.codeVerifier)
    )
    return requestFactory.createSignInRequest(
        provider = provider,
        state = state,
        codeChallenge = pkce.codeChallenge,
    )
  }

  override suspend fun handleCallback(callbackUrl: String): AuthSession {
    val callback = callbackParser.parse(callbackUrl)
    val pendingState = localDataSource.getOAuthState() ?: error("No pending sign in request.")
    check(callback.state == pendingState.state) { "OAuth state does not match." }

    val tokens = remoteDataSource.exchangeCode(callback.code, pendingState.codeVerifier)
    val claims = idTokenDecoder.decode(tokens.idToken)
    val session =
        AuthSession(
            sub = claims.sub,
            email = claims.email,
            displayName = claims.displayName,
            provider = claims.provider,
            idToken = tokens.idToken,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresAtEpochSeconds = claims.expiresAtEpochSeconds,
        )
    localDataSource.saveSession(session)
    localDataSource.clearOAuthState()
    return session
  }

  override suspend fun signOut(): AuthSignOutRequest {
    localDataSource.clearSession()
    localDataSource.clearOAuthState()
    return requestFactory.createSignOutRequest()
  }

  private companion object {
    const val StateLength = 32
  }
}
