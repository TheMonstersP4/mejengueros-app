package io.github.themonstersp4.mejengueros.presentation.auth

import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  @Test
  fun initRestoresExistingSession() = runTest {
    val repository = FakeAuthRepository(existingSession = sampleSession())
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertEquals("Player", viewModel.uiState.value.displayName)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertNull(viewModel.uiState.value.errorMessage)
    scope.cancel()
  }

  @Test
  fun signInWithGoogleOpensCognitoAuthorizationUrl() = runTest {
    val browser = FakeOAuthBrowser()
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            browser,
            MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithGoogle()
    advanceUntilIdle()

    assertEquals(AuthProvider.Google, repository.receivedProvider)
    assertEquals("https://cognito.example/authorize", browser.openedUrl)
    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(AuthProvider.Google, viewModel.uiState.value.pendingProvider)
    scope.cancel()
  }

  @Test
  fun callbackAuthenticatesSession() = runTest {
    val callbacks = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbacks,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    callbacks.emit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    advanceUntilIdle()

    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertEquals("Google", viewModel.uiState.value.provider)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "com.themonsters.mejengueros://auth/callback?code=code&state=state",
        repository.receivedCallback,
    )
    scope.cancel()
  }

  @Test
  fun callbackPublishedBeforeViewModelStillAuthenticatesSession() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbacks,
            markCallbackConsumed = { callbacks.resetReplayCache() },
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertEquals(
        "com.themonsters.mejengueros://auth/callback?code=code&state=state",
        repository.receivedCallback,
    )
    scope.cancel()
  }

  @Test
  fun consumedCallbackIsNotProcessedByNewSubscriber() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository = FakeAuthRepository()
    val firstScope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val secondScope = TestScope(UnconfinedTestDispatcher(testScheduler))

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = firstScope,
    )
    advanceUntilIdle()
    firstScope.cancel()

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = secondScope,
    )
    advanceUntilIdle()

    assertEquals(1, repository.callbackCount)
    secondScope.cancel()
  }

  @Test
  fun signOutClearsRepositoryStateAndOpensLogoutUrl() = runTest {
    val browser = FakeOAuthBrowser()
    val repository = FakeAuthRepository(existingSession = sampleSession())
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            browser,
            MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.signOut()
    advanceUntilIdle()

    assertEquals(AuthUiState(), viewModel.uiState.value)
    assertEquals(1, repository.signOutCount)
    assertEquals("https://cognito.example/logout", browser.openedUrl)
    scope.cancel()
  }

  private class FakeAuthRepository(private val existingSession: AuthSession? = null) :
      IAuthRepository {
    var receivedProvider: AuthProvider? = null
    var receivedCallback: String? = null
    var callbackCount = 0
    var signOutCount = 0

    override suspend fun getSession(): AuthSession? = existingSession

    override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest {
      receivedProvider = provider
      return AuthSignInRequest("https://cognito.example/authorize")
    }

    override suspend fun handleCallback(callbackUrl: String): AuthSession {
      receivedCallback = callbackUrl
      callbackCount++
      return sampleSession()
    }

    override suspend fun signOut(): AuthSignOutRequest {
      signOutCount++
      return AuthSignOutRequest("https://cognito.example/logout")
    }
  }

  private class FakeOAuthBrowser : IOAuthBrowser {
    var openedUrl: String? = null

    override suspend fun open(url: String) {
      openedUrl = url
    }
  }

  private companion object {
    fun sampleSession(): AuthSession =
        AuthSession(
            sub = "user-sub",
            email = "player@example.com",
            displayName = "Player",
            provider = "Google",
            idToken = "id-token",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = 4102444800,
        )
  }
}
