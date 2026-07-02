package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.auth.CognitoOAuthRequestFactory
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.JwtIdTokenDecoder
import io.github.themonstersp4.mejengueros.data.auth.OAuthCallbackParser
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.data.remote.CognitoTokenResponseDto
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthenticatedUserRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICognitoNativeAuthDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

class AuthRepository(
    private val secureStorage: IAuthSecureStorage,
    private val remoteDataSource: IAuthRemoteDataSource,
    private val nativeAuthDataSource: ICognitoNativeAuthDataSource,
    private val authenticatedUserRemoteDataSource: IAuthenticatedUserRemoteDataSource,
    private val requestFactory: CognitoOAuthRequestFactory,
    private val pkceGenerator: PkceGenerator,
    private val randomStringGenerator: IRandomStringGenerator,
    private val callbackParser: OAuthCallbackParser,
    private val idTokenDecoder: JwtIdTokenDecoder,
) : IAuthRepository {
  private val _userProfile = MutableStateFlow<UserProfile?>(null)

  override fun getUserProfile(): UserProfile? = _userProfile.value

  override suspend fun getSession(): AuthSession? {
    val session =
        secureStorage.getSession()?.takeIf { it.expiresAtEpochSeconds > currentEpochSeconds() }
            ?: return null

    syncCurrentUser()
    return session
  }

  override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest {
    val state = randomStringGenerator.generate(StateLength)
    val pkce = pkceGenerator.generate()
    secureStorage.saveOAuthState(PendingOAuthState(state = state, codeVerifier = pkce.codeVerifier))
    return requestFactory.createSignInRequest(
        provider = provider,
        state = state,
        codeChallenge = pkce.codeChallenge,
    )
  }

  override suspend fun registerWithEmail(fullName: String, email: String, password: String) {
    nativeAuthDataSource.signUp(fullName.trim(), email.trim(), password)
  }

  override suspend fun confirmRegistration(email: String, code: String) {
    nativeAuthDataSource.confirmSignUp(email.trim(), code.trim())
  }

  override suspend fun resendRegistrationCode(email: String) {
    nativeAuthDataSource.resendConfirmationCode(email.trim())
  }

  override suspend fun signInWithEmail(email: String, password: String): AuthSession {
    val tokens = nativeAuthDataSource.signIn(email.trim(), password)
    return saveSession(tokens)
  }

  override suspend fun requestPasswordReset(email: String) {
    nativeAuthDataSource.forgotPassword(email.trim())
  }

  override suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) {
    nativeAuthDataSource.confirmForgotPassword(email.trim(), code.trim(), newPassword)
  }

  override suspend fun handleCallback(callbackUrl: String): AuthSession {
    val callback = callbackParser.parse(callbackUrl)
    val pendingState = secureStorage.getOAuthState() ?: error("No pending sign in request.")
    check(callback.state == pendingState.state) { "OAuth state does not match." }

    val tokens = remoteDataSource.exchangeCode(callback.code, pendingState.codeVerifier)
    val session = saveSession(tokens)
    secureStorage.clearOAuthState()
    return session
  }

  override suspend fun signOut(): AuthSignOutRequest {
    secureStorage.clearSession()
    secureStorage.clearOAuthState()
    return requestFactory.createSignOutRequest()
  }

  override suspend fun refreshUserProfile() {
    syncCurrentUser()
  }

  @OptIn(ExperimentalTime::class)
  private fun currentEpochSeconds(): Long = Clock.System.now().epochSeconds

  private suspend fun saveSession(tokens: CognitoTokenResponseDto): AuthSession {
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
    secureStorage.saveSession(session)
    syncCurrentUserOrClearSession()
    return session
  }

  private suspend fun syncCurrentUser() {
    try {
      _userProfile.value = authenticatedUserRemoteDataSource.syncCurrentUser()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Throwable) {
      // Restored sessions survive temporary API or network failures during app startup.
    }
  }

  private suspend fun syncCurrentUserOrClearSession() {
    try {
      _userProfile.value = authenticatedUserRemoteDataSource.syncCurrentUser()
    } catch (error: CancellationException) {
      throw error
    } catch (error: Throwable) {
      secureStorage.clearSession()
      throw error
    }
  }

  private companion object {
    const val StateLength = 32
  }
}
