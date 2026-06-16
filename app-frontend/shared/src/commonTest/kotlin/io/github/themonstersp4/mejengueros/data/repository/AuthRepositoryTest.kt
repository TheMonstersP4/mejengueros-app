package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.auth.Base64Url
import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthConfig
import io.github.themonstersp4.mejengueros.data.auth.CognitoOAuthRequestFactory
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.JwtIdTokenDecoder
import io.github.themonstersp4.mejengueros.data.auth.OAuthCallbackParser
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.data.remote.CognitoTokenResponseDto
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class AuthRepositoryTest {
  @Test
  fun createSignInRequestStoresPkceStateAndBuildsCognitoUrl() = runTest {
    val localDataSource = FakeAuthLocalDataSource()
    val repository = createRepository(localDataSource = localDataSource)

    val request = repository.createSignInRequest(AuthProvider.Google)

    assertEquals("state-value", localDataSource.savedOAuthState?.state)
    assertEquals(CodeVerifier, localDataSource.savedOAuthState?.codeVerifier)
    assertTrue(request.authorizationUrl.contains("identity_provider=Google"))
    assertTrue(request.authorizationUrl.contains("code_challenge_method=S256"))
    assertTrue(request.authorizationUrl.contains("state=state-value"))
  }

  @Test
  fun handleCallbackExchangesCodeAndSavesAuthenticatedSession() = runTest {
    val localDataSource =
        FakeAuthLocalDataSource(
            PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
        )
    val remoteDataSource = FakeAuthRemoteDataSource()
    val repository =
        createRepository(localDataSource = localDataSource, remoteDataSource = remoteDataSource)

    val session =
        repository.handleCallback(
            "com.themonsters.mejengueros://auth/callback?code=auth-code&state=state-value"
        )

    assertEquals("auth-code", remoteDataSource.receivedCode)
    assertEquals(CodeVerifier, remoteDataSource.receivedCodeVerifier)
    assertEquals("user-sub", session.sub)
    assertEquals("player@example.com", session.email)
    assertEquals("Google", session.provider)
    assertEquals(session, localDataSource.savedSession)
    assertNull(localDataSource.savedOAuthState)
  }

  @Test
  fun signOutClearsSessionAndPendingOAuthState() = runTest {
    val localDataSource =
        FakeAuthLocalDataSource(
            PendingOAuthState(state = "state-value", codeVerifier = CodeVerifier)
        )
    localDataSource.savedSession = sampleSession()
    val repository = createRepository(localDataSource = localDataSource)

    val request = repository.signOut()

    assertNull(localDataSource.savedSession)
    assertNull(localDataSource.savedOAuthState)
    assertTrue(request.logoutUrl.contains("/logout"))
  }

  @Test
  fun getSessionReturnsStoredSession() = runTest {
    val localDataSource = FakeAuthLocalDataSource()
    localDataSource.savedSession = sampleSession()
    val repository = createRepository(localDataSource = localDataSource)

    val session = repository.getSession()

    assertNotNull(session)
    assertEquals("player@example.com", session.email)
  }

  private fun createRepository(
      localDataSource: FakeAuthLocalDataSource = FakeAuthLocalDataSource(),
      remoteDataSource: FakeAuthRemoteDataSource = FakeAuthRemoteDataSource(),
  ): AuthRepository =
      AuthRepository(
          localDataSource = localDataSource,
          remoteDataSource = remoteDataSource,
          requestFactory = CognitoOAuthRequestFactory(testConfig),
          pkceGenerator = PkceGenerator(FakeRandomStringGenerator()),
          randomStringGenerator = FakeRandomStringGenerator(),
          callbackParser = OAuthCallbackParser(),
          idTokenDecoder = JwtIdTokenDecoder(Json),
      )

  private class FakeAuthLocalDataSource(initialState: PendingOAuthState? = null) :
      IAuthLocalDataSource {
    var savedSession: AuthSession? = null
    var savedOAuthState: PendingOAuthState? = initialState

    override fun getSession(): AuthSession? = savedSession

    override fun saveSession(session: AuthSession) {
      savedSession = session
    }

    override fun clearSession() {
      savedSession = null
    }

    override fun getOAuthState(): PendingOAuthState? = savedOAuthState

    override fun saveOAuthState(state: PendingOAuthState) {
      savedOAuthState = state
    }

    override fun clearOAuthState() {
      savedOAuthState = null
    }
  }

  private class FakeAuthRemoteDataSource : IAuthRemoteDataSource {
    var receivedCode: String? = null
    var receivedCodeVerifier: String? = null

    override suspend fun exchangeCode(code: String, codeVerifier: String): CognitoTokenResponseDto {
      receivedCode = code
      receivedCodeVerifier = codeVerifier
      return CognitoTokenResponseDto(
          idToken = sampleIdToken(),
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

    fun sampleSession(): AuthSession =
        AuthSession(
            sub = "user-sub",
            email = "player@example.com",
            displayName = "Player",
            provider = "Google",
            idToken = sampleIdToken(),
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = 4102444800,
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
