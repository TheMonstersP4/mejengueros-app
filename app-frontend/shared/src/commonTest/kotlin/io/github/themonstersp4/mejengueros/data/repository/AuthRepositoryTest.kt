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
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.data.remote.CognitoTokenResponseDto
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
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
    val repository =
        createRepository(secureStorage = secureStorage, remoteDataSource = remoteDataSource)

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
  fun getSessionReturnsStoredSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveSession(sampleSession())
    val repository = createRepository(secureStorage = secureStorage)

    val session = repository.getSession()

    assertNotNull(session)
    assertEquals("player@example.com", session.email)
  }

  @Test
  fun getSessionReturnsNullForExpiredSession() = runTest {
    val secureStorage = InMemoryAuthSecureStorage()
    secureStorage.saveSession(sampleSession(expiresAtEpochSeconds = 1))
    val repository = createRepository(secureStorage = secureStorage)

    assertNull(repository.getSession())
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

  private fun createRepository(
      secureStorage: IAuthSecureStorage = InMemoryAuthSecureStorage(),
      remoteDataSource: FakeAuthRemoteDataSource = FakeAuthRemoteDataSource(),
  ): AuthRepository =
      AuthRepository(
          secureStorage = secureStorage,
          remoteDataSource = remoteDataSource,
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

  private class FailingSaveAuthSecureStorage(
      initialSession: AuthSession? = null,
      initialOAuthState: PendingOAuthState? = null,
      private val sessionFailure: Throwable? = null,
      private val oauthStateFailure: Throwable? = null,
  ) : IAuthSecureStorage {
    private var session: AuthSession? = initialSession
    private var oauthState: PendingOAuthState? = initialOAuthState

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
  }

  private companion object {
    const val CodeVerifier = "verifier-value-verifier-value-verifier-value-verifier-value"

    val testConfig =
        CognitoAuthConfig(
            clientId = "client-id",
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
