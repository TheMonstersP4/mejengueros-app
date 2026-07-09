package io.github.themonstersp4.mejengueros.presentation.auth

import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
  fun initKeepsAuthenticatedStartupGateActiveUntilProfileSyncCompletes() = runTest {
    val restoreGate = CompletableDeferred<Unit>()
    val profileRefreshStarted = CompletableDeferred<Unit>()
    val repository =
        FakeAuthRepository(
                existingSession = sampleSession(),
                onGetSession = { restoreGate.await() },
            )
            .apply {
              onRefreshUserProfile = {
                profileRefreshStarted.complete(Unit)
                CompletableDeferred<Unit>().await()
              }
            }
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = FakeErrorReporter(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    assertTrue(viewModel.uiState.value.isRestoringSession)

    restoreGate.complete(Unit)
    advanceUntilIdle()

    assertTrue(profileRefreshStarted.isCompleted)
    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertEquals("Player", viewModel.uiState.value.displayName)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isRestoringSession)
    assertTrue(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isOwner)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(1, repository.refreshUserProfileCount)
    scope.cancel()
  }

  @Test
  fun initWithoutStoredSessionClearsRestoringAndStaysUnauthenticated() = runTest {
    val repository = FakeAuthRepository(existingSession = null)
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = FakeErrorReporter(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isRestoringSession)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertEquals("", viewModel.uiState.value.email)
    scope.cancel()
  }

  @Test
  fun restoreFailureClearsRestoringAndReportsRecoverableFailure() = runTest {
    val repository = FakeAuthRepository(getSessionError = IllegalStateException("boom"))
    val errorReporter = FakeErrorReporter()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = errorReporter,
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isRestoringSession)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(
        listOf(
            ReportedFailure(
                name = "auth_session_restore_failed",
                attributes =
                    mapOf(
                        "operation" to "restore_session",
                        "error_source" to "unexpected",
                    ),
            )
        ),
        errorReporter.events,
    )
    scope.cancel()
  }

  @Test
  fun restoreCancellationClearsRestoringWithoutReportingRecoverableFailure() = runTest {
    val repository = FakeAuthRepository(getSessionError = CancellationException("cancelled"))
    val errorReporter = FakeErrorReporter()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = errorReporter,
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isRestoringSession)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertTrue(errorReporter.events.isEmpty())
    scope.cancel()
  }

  @Test
  fun restoredSessionProfileSyncUpdatesOwnerRoleWhenBackgroundRefreshCompletes() = runTest {
    val refreshGate = CompletableDeferred<Unit>()
    val profileRefreshStarted = CompletableDeferred<Unit>()
    val repository =
        FakeAuthRepository(existingSession = sampleSession(), currentProfile = null).apply {
          onRefreshUserProfile = {
            profileRefreshStarted.complete(Unit)
            refreshGate.await()
            currentProfile = UserProfile(id = "user-id", roles = listOf(UserRoleKind.OWNER))
          }
        }
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = FakeErrorReporter(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertTrue(profileRefreshStarted.isCompleted)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertTrue(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isOwner)

    refreshGate.complete(Unit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isOwner)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertEquals(1, repository.refreshUserProfileCount)
    scope.cancel()
  }

  @Test
  fun restoredSessionProfileSyncFailureIsReportedWithoutDeauthenticatingUser() = runTest {
    val repository =
        FakeAuthRepository(
            existingSession = sampleSession(),
            refreshUserProfileError = IllegalStateException("profile sync failed"),
        )
    val errorReporter = FakeErrorReporter()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = errorReporter,
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isRestoringSession)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertFalse(viewModel.uiState.value.isOwner)
    assertEquals(
        listOf(
            ReportedFailure(
                name = "auth_restore_profile_sync_failed",
                attributes =
                    mapOf(
                        "operation" to "restore_profile_sync",
                        "error_source" to "unexpected",
                    ),
            )
        ),
        errorReporter.events,
    )
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
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithGoogle()
    advanceUntilIdle()

    assertEquals(AuthProvider.Google, repository.receivedProvider)
    assertEquals("https://cognito.example/authorize", browser.openedUrl)
    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(AuthProvider.Google, viewModel.uiState.value.pendingProvider)
    assertTrue(viewModel.uiState.value.isExternalAuthInProgress)
    scope.cancel()
  }

  @Test
  fun cancelExternalAuthClearsProgressAndReportsRecoverableFailure() = runTest {
    val browser = FakeOAuthBrowser()
    val errorReporter = FakeErrorReporter()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            FakeAuthRepository(),
            browser,
            errorReporter = errorReporter,
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithGoogle()
    advanceUntilIdle()
    viewModel.cancelExternalAuth()

    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)
    assertEquals(
        listOf(
            ReportedFailure(
                name = "auth_external_signin_cancelled",
                attributes =
                    mapOf(
                        "operation" to "cancel_external_signin",
                        "stage" to "pending_callback",
                        "outcome" to "user_cancelled",
                        "provider" to "google",
                    ),
            )
        ),
        errorReporter.events,
    )
    scope.cancel()
  }

  @Test
  fun cancelExternalAuthAllowsFreshGoogleRetryAndReopensBrowser() = runTest {
    val browser = FakeOAuthBrowser()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            FakeAuthRepository(),
            browser,
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithGoogle()
    advanceUntilIdle()
    viewModel.cancelExternalAuth()

    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)

    viewModel.signInWithGoogle()
    advanceUntilIdle()

    assertEquals(2, browser.openCount)
    assertEquals("https://cognito.example/authorize", browser.openedUrl)
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.isExternalAuthInProgress)
    assertEquals(AuthProvider.Google, viewModel.uiState.value.pendingProvider)
    scope.cancel()
  }

  @Test
  fun externalSignInStartFailureClearsProgressAndReportsRecoverableFailureWithoutSecrets() =
      runTest {
        val browser = FakeOAuthBrowser(openError = IllegalStateException("browser failed"))
        val errorReporter = FakeErrorReporter()
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val viewModel =
            AuthViewModel(
                FakeAuthRepository(),
                browser,
                errorReporter = errorReporter,
                callbackUrls = MutableSharedFlow(),
                coroutineScope = scope,
            )

        viewModel.signInWithGoogle()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
        assertNull(viewModel.uiState.value.pendingProvider)
        assertEquals(
            listOf(
                ReportedFailure(
                    name = "auth_external_signin_start_failed",
                    attributes =
                        mapOf(
                            "operation" to "start_external_signin",
                            "stage" to "start",
                            "outcome" to "failed",
                            "provider" to "google",
                            "error_source" to "unexpected",
                        ),
                )
            ),
            errorReporter.events,
        )
        scope.cancel()
      }

  @Test
  fun callbackKeepsExternalAuthProgressVisibleUntilGoogleSignInCompletes() = runTest {
    val callbacks = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callbackGate = CompletableDeferred<Unit>()
    val callbackStarted = CompletableDeferred<Unit>()
    val repository =
        FakeAuthRepository().apply {
          onHandleCallback = { callbackUrl ->
            receivedCallback = callbackUrl
            callbackCount++
            callbackStarted.complete(Unit)
            callbackGate.await()
            sampleSession()
          }
        }
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = callbacks,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.signInWithGoogle()
    advanceUntilIdle()
    callbacks.emit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    advanceUntilIdle()

    assertTrue(callbackStarted.isCompleted)
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.isExternalAuthInProgress)
    assertEquals(AuthProvider.Google, viewModel.uiState.value.pendingProvider)
    assertFalse(viewModel.uiState.value.isAuthenticated)

    callbackGate.complete(Unit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)
    scope.cancel()
  }

  @Test
  fun callbackFailureClearsExternalAuthProgressAfterReplayCallbackStarts() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val callbackGate = CompletableDeferred<Unit>()
    val callbackStarted = CompletableDeferred<Unit>()
    val errorReporter = FakeErrorReporter()
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository =
        FakeAuthRepository().apply {
          onHandleCallback = {
            callbackCount++
            callbackStarted.complete(Unit)
            callbackGate.await()
            throw IllegalStateException("No se pudo finalizar el inicio de sesión.")
          }
        }
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = errorReporter,
            callbackUrls = callbacks,
            markCallbackConsumed = { callbacks.resetReplayCache() },
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertTrue(callbackStarted.isCompleted)
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)

    callbackGate.complete(Unit)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)
    assertEquals(
        "No se pudo finalizar el inicio de sesión.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(
        listOf(
            ReportedFailure(
                name = "auth_external_signin_callback_failed",
                attributes =
                    mapOf(
                        "operation" to "handle_external_signin_callback",
                        "stage" to "callback",
                        "outcome" to "failed",
                        "error_source" to "unexpected",
                    ),
            )
        ),
        errorReporter.events,
    )
    scope.cancel()
  }

  @Test
  fun callbackCancellationClearsExternalAuthProgressAfterReplayCallbackStarts() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val callbackGate = CompletableDeferred<Unit>()
    val callbackStarted = CompletableDeferred<Unit>()
    val errorReporter = FakeErrorReporter()
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository =
        FakeAuthRepository().apply {
          onHandleCallback = {
            callbackCount++
            callbackStarted.complete(Unit)
            callbackGate.await()
            throw CancellationException("External auth cancelled")
          }
        }
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            errorReporter = errorReporter,
            callbackUrls = callbacks,
            markCallbackConsumed = { callbacks.resetReplayCache() },
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertTrue(callbackStarted.isCompleted)
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.isExternalAuthInProgress)

    callbackGate.complete(Unit)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.isExternalAuthInProgress)
    assertNull(viewModel.uiState.value.pendingProvider)
    assertEquals(
        listOf(
            ReportedFailure(
                name = "auth_external_signin_callback_cancelled",
                attributes =
                    mapOf(
                        "operation" to "handle_external_signin_callback",
                        "stage" to "callback",
                        "outcome" to "cancelled",
                        "error_source" to "cancellation",
                    ),
            )
        ),
        errorReporter.events,
    )
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
            callbackUrls = callbacks,
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
            callbackUrls = callbacks,
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
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = firstScope,
    )
    advanceUntilIdle()
    firstScope.cancel()

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = secondScope,
    )
    advanceUntilIdle()

    assertEquals(1, repository.callbackCount)
    secondScope.cancel()
  }

  @Test
  fun consumedFailureCallbackIsNotProcessedByNewSubscriber() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository =
        FakeAuthRepository().apply {
          onHandleCallback = {
            callbackCount++
            throw IllegalStateException("No se pudo finalizar el inicio de sesión.")
          }
        }
    val firstErrorReporter = FakeErrorReporter()
    val secondErrorReporter = FakeErrorReporter()
    val firstScope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val secondScope = TestScope(UnconfinedTestDispatcher(testScheduler))

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        errorReporter = firstErrorReporter,
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = firstScope,
    )
    advanceUntilIdle()
    firstScope.cancel()

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        errorReporter = secondErrorReporter,
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = secondScope,
    )
    advanceUntilIdle()

    assertEquals(1, repository.callbackCount)
    assertEquals(1, firstErrorReporter.events.size)
    assertTrue(secondErrorReporter.events.isEmpty())
    secondScope.cancel()
  }

  @Test
  fun consumedCancellationCallbackIsNotProcessedByNewSubscriber() = runTest {
    val callbacks = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    callbacks.tryEmit("com.themonsters.mejengueros://auth/callback?code=code&state=state")
    val repository =
        FakeAuthRepository().apply {
          onHandleCallback = {
            callbackCount++
            throw CancellationException("External auth cancelled")
          }
        }
    val firstErrorReporter = FakeErrorReporter()
    val secondErrorReporter = FakeErrorReporter()
    val firstScope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val secondScope = TestScope(UnconfinedTestDispatcher(testScheduler))

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        errorReporter = firstErrorReporter,
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = firstScope,
    )
    advanceUntilIdle()
    firstScope.cancel()

    AuthViewModel(
        repository,
        FakeOAuthBrowser(),
        errorReporter = secondErrorReporter,
        callbackUrls = callbacks,
        markCallbackConsumed = { callbacks.resetReplayCache() },
        coroutineScope = secondScope,
    )
    advanceUntilIdle()

    assertEquals(1, repository.callbackCount)
    assertEquals(1, firstErrorReporter.events.size)
    assertTrue(secondErrorReporter.events.isEmpty())
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
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.signOut()
    advanceUntilIdle()

    assertEquals(AuthUiState(isRestoringSession = false), viewModel.uiState.value)
    assertEquals(1, repository.signOutCount)
    assertEquals("https://cognito.example/logout", browser.openedUrl)
    scope.cancel()
  }

  @Test
  fun signInWithEmailRequiresCredentials() = runTest {
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            FakeAuthRepository(),
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithEmail(email = "", password = "")

    assertEquals(
        "Ingresa tu correo y contraseña para continuar.",
        viewModel.uiState.value.errorMessage,
    )
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.pendingProvider)
    scope.cancel()
  }

  @Test
  fun signInWithEmailAuthenticatesSession() = runTest {
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.signInWithEmail(email = "player@example.com", password = "secret123")
    advanceUntilIdle()

    assertEquals("player@example.com", repository.receivedEmailSignInEmail)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isResolvingAuthenticatedStartup)
    assertNull(viewModel.uiState.value.errorMessage)
    scope.cancel()
  }

  @Test
  fun registerWithEmailStoresEmailAndRunsNavigationCallback() = runTest {
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    var navigationCallbackCount = 0

    viewModel.registerWithEmail(
        fullName = "Player One",
        email = "player@example.com",
        password = "secret123",
        onCodeSent = { navigationCallbackCount++ },
    )
    advanceUntilIdle()

    assertEquals("Player One", repository.receivedRegisterFullName)
    assertEquals("player@example.com", repository.receivedRegisterEmail)
    assertEquals("player@example.com", viewModel.uiState.value.emailInput)
    assertEquals(1, navigationCallbackCount)
    scope.cancel()
  }

  @Test
  fun confirmRegistrationUsesStoredEmailAndRunsNavigationCallback() = runTest {
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    var navigationCallbackCount = 0

    viewModel.registerWithEmail(
        fullName = "Player One",
        email = "player@example.com",
        password = "secret123",
        onCodeSent = {},
    )
    advanceUntilIdle()
    viewModel.confirmRegistration(code = "123456", onConfirmed = { navigationCallbackCount++ })
    advanceUntilIdle()

    assertEquals("player@example.com", repository.receivedConfirmEmail)
    assertEquals("123456", repository.receivedConfirmCode)
    assertEquals(1, navigationCallbackCount)
    scope.cancel()
  }

  @Test
  fun passwordResetUsesStoredEmailAndRunsNavigationCallbacks() = runTest {
    val repository = FakeAuthRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    var resetOpenedCount = 0
    var loginOpenedCount = 0

    viewModel.requestPasswordReset(
        email = "player@example.com",
        onCodeSent = { resetOpenedCount++ },
    )
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isPasswordResetConfirmed)

    viewModel.confirmPasswordReset(
        code = "123456",
        newPassword = "new-password",
        onConfirmed = { loginOpenedCount++ },
    )
    advanceUntilIdle()

    assertEquals("player@example.com", repository.receivedForgotEmail)
    assertEquals("player@example.com", repository.receivedResetEmail)
    assertEquals("123456", repository.receivedResetCode)
    assertTrue(viewModel.uiState.value.isPasswordResetConfirmed)
    assertEquals(1, resetOpenedCount)
    assertEquals(1, loginOpenedCount)
    scope.cancel()
  }

  @Test
  fun requestPasswordResetMapsNetworkFailureToSpanishRetryMessage() = runTest {
    val repository =
        FakeAuthRepository(
            passwordResetError =
                IllegalStateException(
                    "Failed to connect to cognito-idp.us-east-2.amazonaws.com/[2600:1f16::]:443"
                )
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    viewModel.requestPasswordReset(email = "player@example.com", onCodeSent = {})
    advanceUntilIdle()

    assertEquals(
        "No pudimos conectar con el servicio. Revisá tu conexión e intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertFalse(viewModel.uiState.value.isLoading)
    scope.cancel()
  }

  @Test
  fun refreshProfileAfterOwnerTransitionUpdatesIsOwnerFlag() = runTest {
    val repository = FakeAuthRepository(currentProfile = null)
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AuthViewModel(
            repository,
            FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isOwner)

    // Simulate backend granting OWNER role after complex creation
    repository.currentProfile = UserProfile(id = "user-id", roles = listOf(UserRoleKind.OWNER))

    viewModel.refreshProfileAfterOwnerTransition()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isOwner)
    assertEquals(1, repository.refreshUserProfileCount)
    scope.cancel()
  }

  private class FakeAuthRepository(
      private val existingSession: AuthSession? = null,
      private val getSessionError: Throwable? = null,
      private val onGetSession: (suspend () -> Unit)? = null,
      private val passwordResetError: Throwable? = null,
      private val refreshUserProfileError: Throwable? = null,
      var currentProfile: UserProfile? = null,
  ) : IAuthRepository {
    var onRefreshUserProfile: (suspend () -> Unit)? = null
    var onHandleCallback: (suspend (String) -> AuthSession)? = null
    var receivedProvider: AuthProvider? = null
    var receivedCallback: String? = null
    var callbackCount = 0
    var signOutCount = 0
    var refreshUserProfileCount = 0
    var receivedRegisterFullName: String? = null
    var receivedRegisterEmail: String? = null
    var receivedConfirmEmail: String? = null
    var receivedConfirmCode: String? = null
    var receivedEmailSignInEmail: String? = null
    var receivedForgotEmail: String? = null
    var receivedResetEmail: String? = null
    var receivedResetCode: String? = null

    override suspend fun getSession(): AuthSession? {
      onGetSession?.invoke()
      getSessionError?.let { throw it }
      return existingSession
    }

    override fun getUserProfile(): UserProfile? = currentProfile

    override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest {
      receivedProvider = provider
      return AuthSignInRequest("https://cognito.example/authorize")
    }

    override suspend fun registerWithEmail(fullName: String, email: String, password: String) {
      receivedRegisterFullName = fullName
      receivedRegisterEmail = email
    }

    override suspend fun confirmRegistration(email: String, code: String) {
      receivedConfirmEmail = email
      receivedConfirmCode = code
    }

    override suspend fun resendRegistrationCode(email: String) = Unit

    override suspend fun signInWithEmail(email: String, password: String): AuthSession {
      receivedEmailSignInEmail = email
      return sampleSession()
    }

    override suspend fun requestPasswordReset(email: String) {
      passwordResetError?.let { throw it }
      receivedForgotEmail = email
    }

    override suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) {
      receivedResetEmail = email
      receivedResetCode = code
    }

    override suspend fun handleCallback(callbackUrl: String): AuthSession {
      onHandleCallback?.let {
        return it(callbackUrl)
      }
      receivedCallback = callbackUrl
      callbackCount++
      return sampleSession()
    }

    override suspend fun signOut(): AuthSignOutRequest {
      signOutCount++
      return AuthSignOutRequest("https://cognito.example/logout")
    }

    override suspend fun refreshUserProfile() {
      refreshUserProfileCount++
      onRefreshUserProfile?.invoke()
      refreshUserProfileError?.let { throw it }
    }
  }

  private class FakeErrorReporter : ErrorReporter {
    val events = mutableListOf<ReportedFailure>()

    override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
      events += ReportedFailure(name = name, attributes = attributes)
    }
  }

  private data class ReportedFailure(
      val name: String,
      val attributes: Map<String, String>,
  )

  private class FakeOAuthBrowser(
      private val openError: Throwable? = null,
  ) : IOAuthBrowser {
    var openedUrl: String? = null
    var openCount = 0

    override suspend fun open(url: String) {
      openError?.let { throw it }
      openCount++
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
