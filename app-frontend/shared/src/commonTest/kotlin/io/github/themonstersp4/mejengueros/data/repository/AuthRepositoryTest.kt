package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.auth.AuthSecureStorageWriteException
import io.github.themonstersp4.mejengueros.data.auth.Base64Url
import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthConfig
import io.github.themonstersp4.mejengueros.data.auth.CognitoOAuthRequestFactory
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.InMemoryAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.JwtIdTokenDecoder
import io.github.themonstersp4.mejengueros.data.auth.OAuthCallbackParser
import io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.data.remote.CognitoTokenResponseDto
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthenticatedUserRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICognitoNativeAuthDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class AuthRepositoryTest {
  @Test
  fun createSignInRequestStoresPkceStateAndBuildsCognitoUrl() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    val repository = createRepository(secureStorage = secureStorage)

    val request = repository.createSignInRequest(AuthProvider.Google)

    assertEquals("state-value", secureStorage.getOAuthState()?.state)
    assertEquals(CodeVerifier, secureStorage.getOAuthState()?.codeVerifier)
    assertTrue(request.authorizationUrl.contains("identity_provider=Google"))
    assertTrue(request.authorizationUrl.contains("code_challenge_method=S256"))
    assertTrue(request.authorizationUrl.contains("state=state-value"))
  }

  @Test
  fun createSignInRequestPropagatesOAuthStatePersistenceFailure() = runTest {
    val secureStorage =
        FailingSaveAuthSecureStorage(
            oauthStateFailure =
                AuthSecureStorageWriteException("Failed to securely persist OAuth state.")
        )
    val repository = createRepository(secureStorage = secureStorage)

    val error =
        assertFailsWith<AuthSecureStorageWriteException> {
          repository.createSignInRequest(AuthProvider.Google)
        }

    assertEquals("Failed to securely persist OAuth state.", error.message)
    assertNull(secureStorage.getOAuthState())
  }

  @Test
  fun handleCallbackExchangesCodeAndSavesAuthenticatedSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
    )
    val remoteDataSource = FakeAuthRemoteDataSource()
    val authenticatedUserRemoteDataSource = FakeAuthenticatedUserRemoteDataSource()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            remoteDataSource = remoteDataSource,
            authenticatedUserRemoteDataSource = authenticatedUserRemoteDataSource,
        )

    val session =
        repository.handleCallback(
            "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
        )

    assertEquals("auth-code", remoteDataSource.receivedCode)
    assertEquals(CodeVerifier, remoteDataSource.receivedCodeVerifier)
    assertEquals("user-sub", session.sub)
    assertEquals("player@example.com", session.email)
    assertEquals("Google", session.provider)
    assertEquals(session, secureStorage.getSession())
    assertEquals(1, authenticatedUserRemoteDataSource.syncCount)
    assertNull(secureStorage.getOAuthState())
  }

  @Test
  fun handleCallbackPropagatesSessionPersistenceFailureWithoutClearingPendingState() = runTest {
    val secureStorage =
        FailingSaveAuthSecureStorage(
            initialOAuthState =
                PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier),
            sessionFailure =
                AuthSecureStorageWriteException("Failed to securely persist auth session."),
        )
    val remoteDataSource = FakeAuthRemoteDataSource()
    val repository =
        createRepository(secureStorage = secureStorage, remoteDataSource = remoteDataSource)

    val error =
        assertFailsWith<AuthSecureStorageWriteException> {
          repository.handleCallback(
              "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
          )
        }

    assertEquals("Failed to securely persist auth session.", error.message)
    assertEquals(CodeVerifier, remoteDataSource.receivedCodeVerifier)
    assertEquals("state-value", secureStorage.getOAuthState()?.state)
    assertNull(secureStorage.getSession())
  }

  @Test
  fun signOutClearsSessionAndPendingOAuthState() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
    )
    secureStorage.saveSession(sampleSession())
    val repository = createRepository(secureStorage = secureStorage)

    val request = repository.signOut()

    assertNull(secureStorage.getSession())
    assertNull(secureStorage.getOAuthState())
    assertTrue(request.logoutUrl.contains("/logout"))
  }

  @Test
  fun getSessionReturnsStoredSessionWithoutWaitingForProfileSync() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveSession(sampleSession())
    val authenticatedUserRemoteDataSource = FakeAuthenticatedUserRemoteDataSource()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource = authenticatedUserRemoteDataSource,
        )

    val session = withTimeout(100) { repository.getSession() }

    assertNotNull(session)
    assertEquals("player@example.com", session.email)
    assertEquals(0, authenticatedUserRemoteDataSource.syncCount)
  }

  @Test
  fun getSessionReturnsNullForExpiredSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveSession(sampleSession(expiresAtEpochSeconds = 1))
    val authenticatedUserRemoteDataSource = FakeAuthenticatedUserRemoteDataSource()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource = authenticatedUserRemoteDataSource,
        )

    assertNull(repository.getSession())
    assertEquals(0, authenticatedUserRemoteDataSource.syncCount)
  }

  @Test
  fun refreshUserProfilePropagatesCancellationWithoutClearingSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    val session = sampleSession()
    secureStorage.saveSession(session)
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource =
                FakeAuthenticatedUserRemoteDataSource(syncFailure = CancellationException()),
        )

    assertFailsWith<CancellationException> { repository.refreshUserProfile() }

    assertEquals(session, secureStorage.getSession())
  }

  @Test
  fun handleCallbackRejectsInvalidState() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "expected-state", codeVerifier = CodeVerifier)
    )
    val repository = createRepository(secureStorage = secureStorage)

    assertFailsWith<IllegalStateException> {
      repository.handleCallback(
          "com.themonsters.mejengueros://auth/callback?code=auth-code&state=wrong-state"
      )
    }
  }

  @Test
  fun handleCallbackRejectsErrorCallback() = runTest {
    val repository = createRepository()

    assertFailsWith<IllegalStateException> {
      repository.handleCallback(
          "com.themonsters.mejengueros://auth/callback?error=access_denied&error_description=Denied"
      )
    }
  }

  @Test
  fun handleCallbackPropagatesTokenExchangeFailure() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
    )
    val repository =
        createRepository(
            secureStorage = secureStorage,
            remoteDataSource = FakeAuthRemoteDataSource(exchangeFailure = true),
        )

    assertFailsWith<IllegalStateException> {
      repository.handleCallback(
          "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
      )
    }
  }

  @Test
  fun handleCallbackRejectsInvalidJwt() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
    )
    val repository =
        createRepository(
            secureStorage = secureStorage,
            remoteDataSource = FakeAuthRemoteDataSource(idToken = "invalid-jwt"),
        )

    assertFailsWith<IllegalStateException> {
      repository.handleCallback(
          "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
      )
    }
  }

  @Test
  fun signInWithEmailStoresCognitoSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    val nativeAuthDataSource = FakeCognitoNativeAuthDataSource()
    val authenticatedUserRemoteDataSource = FakeAuthenticatedUserRemoteDataSource()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            nativeAuthDataSource = nativeAuthDataSource,
            authenticatedUserRemoteDataSource = authenticatedUserRemoteDataSource,
        )

    val session = repository.signInWithEmail(" player@example.com ", "password")

    assertEquals("player@example.com", nativeAuthDataSource.receivedSignInEmail)
    assertEquals("user-sub", session.sub)
    assertEquals(session, secureStorage.getSession())
    assertEquals(1, authenticatedUserRemoteDataSource.syncCount)
  }

  @Test
  fun signInWithEmailClearsSessionWhenUserSyncFails() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource =
                FakeAuthenticatedUserRemoteDataSource(
                    syncFailure = IllegalStateException("User sync failed.")
                ),
        )

    assertFailsWith<IllegalStateException> {
      repository.signInWithEmail(" player@example.com ", "password")
    }

    assertNull(secureStorage.getSession())
  }

  @Test
  fun signInWithEmailPropagatesCancellationWithoutClearingSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource =
                FakeAuthenticatedUserRemoteDataSource(syncFailure = CancellationException()),
        )

    assertFailsWith<CancellationException> {
      repository.signInWithEmail(" player@example.com ", "password")
    }

    assertNotNull(secureStorage.getSession())
  }

  @Test
  fun registerWithEmailUsesCognitoNativeAuth() = runTest {
    val nativeAuthDataSource = FakeCognitoNativeAuthDataSource()
    val repository = createRepository(nativeAuthDataSource = nativeAuthDataSource)

    repository.registerWithEmail(" Player One ", " player@example.com ", "password")
    repository.confirmRegistration(" player@example.com ", " 123456 ")
    repository.resendRegistrationCode(" player@example.com ")

    assertEquals("Player One", nativeAuthDataSource.receivedSignUpFullName)
    assertEquals("player@example.com", nativeAuthDataSource.receivedSignUpEmail)
    assertEquals("player@example.com", nativeAuthDataSource.receivedConfirmEmail)
    assertEquals("123456", nativeAuthDataSource.receivedConfirmCode)
    assertEquals("player@example.com", nativeAuthDataSource.receivedResendEmail)
  }

  @Test
  fun passwordResetUsesCognitoNativeAuth() = runTest {
    val nativeAuthDataSource = FakeCognitoNativeAuthDataSource()
    val repository = createRepository(nativeAuthDataSource = nativeAuthDataSource)

    repository.requestPasswordReset(" player@example.com ")
    repository.confirmPasswordReset(" player@example.com ", " 123456 ", "new-password")

    assertEquals("player@example.com", nativeAuthDataSource.receivedForgotEmail)
    assertEquals("player@example.com", nativeAuthDataSource.receivedResetEmail)
    assertEquals("123456", nativeAuthDataSource.receivedResetCode)
  }

  @Test
  fun getUserProfileReturnsCachedProfileAfterSuccessfulSync() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveOAuthState(
        PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
    )
    val ownerProfile = UserProfile(id = "user-id", roles = listOf(UserRoleKind.OWNER))
    val repository =
        createRepository(
            secureStorage = secureStorage,
            authenticatedUserRemoteDataSource =
                FakeAuthenticatedUserRemoteDataSource(returnedProfile = ownerProfile),
        )

    repository.handleCallback(
        "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
    )

    assertEquals(ownerProfile, repository.getUserProfile())
  }

  @Test
  fun getUserProfileReturnsNullBeforeFirstSync() = runTest {
    val repository = createRepository()

    assertNull(repository.getUserProfile())
  }

  private fun createRepository(
      secureStorage: IAuthSecureStorage = InMemoryAuthSecureStorage(),
      remoteDataSource: FakeAuthRemoteDataSource = FakeAuthRemoteDataSource(),
      nativeAuthDataSource: FakeCognitoNativeAuthDataSource = FakeCognitoNativeAuthDataSource(),
      authenticatedUserRemoteDataSource: FakeAuthenticatedUserRemoteDataSource =
          FakeAuthenticatedUserRemoteDataSource(),
  ): AuthRepository =
      AuthRepository(
          secureStorage = secureStorage,
          remoteDataSource = remoteDataSource,
          nativeAuthDataSource = nativeAuthDataSource,
          authenticatedUserRemoteDataSource = authenticatedUserRemoteDataSource,
          requestFactory = CognitoOAuthRequestFactory(testConfig),
          pkceGenerator = PkceGenerator(FakeRandomStringGenerator()),
          randomStringGenerator = FakeRandomStringGenerator(),
          callbackParser = OAuthCallbackParser(),
          idTokenDecoder = JwtIdTokenDecoder(Json),
      )

  private class FakeAuthRemoteDataSource(
      private val idToken: String = sampleIdToken(),
      private val exchangeFailure: Boolean = false,
  ) : IAuthRemoteDataSource {
    var receivedCode: String? = null
    var receivedCodeVerifier: String? = null

    override suspend fun exchangeCode(code: String, codeVerifier: String): CognitoTokenResponseDto {
      if (exchangeFailure) error("Token exchange failed.")
      receivedCode = code
      receivedCodeVerifier = codeVerifier
      return CognitoTokenResponseDto(
          idToken = idToken,
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresIn = 3600,
          tokenType = "Bearer",
      )
    }
  }

  private class FakeRandomStringGenerator : IRandomStringGenerator {
    override fun generate(length: Int): String = if (length == 32) "state-value" else CodeVerifier
  }

  private class FakeAuthenticatedUserRemoteDataSource(
      private val syncFailure: Throwable? = null,
      private val returnedProfile: UserProfile = UserProfile(id = "user-id", roles = emptyList()),
      private val onSync: (suspend () -> Unit)? = null,
  ) : IAuthenticatedUserRemoteDataSource {
    var syncCount = 0

    override suspend fun syncCurrentUser(): UserProfile {
      syncCount++
      onSync?.invoke()
      syncFailure?.let { throw it }
      return returnedProfile
    }
  }

  private class FailingSaveAuthSecureStorage(
      initialSession: AuthSession? = null,
      initialOAuthState: PendingOAuthState? = null,
      private val sessionFailure: Throwable? = null,
      private val oauthStateFailure: Throwable? = null,
  ) : IAuthSecureStorage {
    private var session: AuthSession? = initialSession
    private var oauthState: PendingOAuthState? = initialOAuthState
    private val ownerViewPreferences = mutableMapOf<String, OwnerViewPreference>()

    override suspend fun getSession(): AuthSession? = session

    override suspend fun saveSession(session: AuthSession) {
      sessionFailure?.let { throw it }
      this.session = session
    }

    override suspend fun clearSession() {
      session = null
    }

    override suspend fun getOAuthState(): PendingOAuthState? = oauthState

    override suspend fun saveOAuthState(state: PendingOAuthState) {
      oauthStateFailure?.let { throw it }
      oauthState = state
    }

    override suspend fun clearOAuthState() {
      oauthState = null
    }

    override suspend fun getOwnerViewPreference(userId: String): OwnerViewPreference? =
        ownerViewPreferences[userId]

    override suspend fun saveOwnerViewPreference(
        userId: String,
        preference: OwnerViewPreference,
    ) {
      ownerViewPreferences[userId] = preference
    }

    override suspend fun clearOwnerViewPreference(userId: String) {
      ownerViewPreferences.remove(userId)
    }
  }

  private class FakeCognitoNativeAuthDataSource : ICognitoNativeAuthDataSource {
    var receivedSignUpEmail: String? = null
    var receivedSignUpFullName: String? = null
    var receivedConfirmEmail: String? = null
    var receivedConfirmCode: String? = null
    var receivedResendEmail: String? = null
    var receivedSignInEmail: String? = null
    var receivedForgotEmail: String? = null
    var receivedResetEmail: String? = null
    var receivedResetCode: String? = null

    override suspend fun signUp(fullName: String, email: String, password: String) {
      receivedSignUpFullName = fullName
      receivedSignUpEmail = email
    }

    override suspend fun confirmSignUp(email: String, code: String) {
      receivedConfirmEmail = email
      receivedConfirmCode = code
    }

    override suspend fun resendConfirmationCode(email: String) {
      receivedResendEmail = email
    }

    override suspend fun signIn(email: String, password: String): CognitoTokenResponseDto {
      receivedSignInEmail = email
      return CognitoTokenResponseDto(
          idToken = sampleIdToken(),
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresIn = 3600,
          tokenType = "Bearer",
      )
    }

    override suspend fun forgotPassword(email: String) {
      receivedForgotEmail = email
    }

    override suspend fun confirmForgotPassword(email: String, code: String, newPassword: String) {
      receivedResetEmail = email
      receivedResetCode = code
    }
  }

  private companion object {
    const val CodeVerifier = "verifier-value-verifier-value-verifier-value-verifier-value"

    val testConfig =
        CognitoAuthConfig(
            clientId = "client-id",
            region = "us-east-2",
            domain = "https://example.auth.us-east-2.amazoncognito.com",
            redirectUri = "com.themonsters.mejengueros://auth/callback",
            logoutUri = "com.themonsters.mejengueros://auth/logout",
            scopes = listOf("openid", "email", "profile"),
        )

    fun sampleSession(expiresAtEpochSeconds: Long = 4102444800): AuthSession =
        AuthSession(
            sub = "user-sub",
            email = "player@example.com",
            displayName = "Player",
            provider = "Google",
            idToken = sampleIdToken(),
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = expiresAtEpochSeconds,
        )

    fun sampleIdToken(): String {
      val payload =
          """
          {
            "sub": "user-sub",
            "email": "player@example.com",
            "name": "Player",
            "exp": 4102444800,
            "identities": "[{\"providerName\":\"Google\"}]"
          }
          """
              .trimIndent()
      return "header.${Base64Url.encode(payload.encodeToByteArray())}.signature"
    }
  }
}
