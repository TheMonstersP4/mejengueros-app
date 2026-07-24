package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.model.AuthSignInRequest
import io.github.themonstersp4.mejengueros.domain.model.AuthSignOutRequest
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.domain.repository.INotificationRepository
import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModel
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.notifications.NotificationsViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthSessionRestorationScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun appNavHostKeepsLoginHiddenWhileSessionRestoreIsPending() = runTest {
    stopKoin()

    val restoreGate = CompletableDeferred<Unit>()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val authViewModel =
        AuthViewModel(
            authRepository = PendingRestoreAuthRepository(restoreGate = restoreGate),
            oauthBrowser = FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    startKoin {
      modules(
          module {
            viewModel { authViewModel }
            single<IAuthSecureStorage> { InMemoryHostAuthSecureStorage() }
          }
      )
    }

    try {
      composeRule.setContent { MejenguerosTheme { AppNavHost() } }

      composeRule.waitForIdle()
      composeRule.onNodeWithTag("auth_session_restore_root", useUnmergedTree = true).assertExists()
      composeRule
          .onNodeWithTag("auth_session_restore_loading", useUnmergedTree = true)
          .assertExists()
      composeRule.onNodeWithText("Restaurando tu sesión...").assertExists()
      composeRule.onNodeWithTag("login_root", useUnmergedTree = true).assertDoesNotExist()

      restoreGate.complete(Unit)
      advanceUntilIdle()
      composeRule.waitForIdle()

      composeRule
          .onNodeWithTag("auth_session_restore_root", useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule.onNodeWithTag("login_root", useUnmergedTree = true).assertExists()
    } finally {
      scope.cancel()
      stopKoin()
    }
  }

  @Test
  fun appNavHostFallsBackToLoginWhenRestoreIsCancelledInsideActiveScope() = runTest {
    stopKoin()

    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val authViewModel =
        AuthViewModel(
            authRepository = CancelledRestoreAuthRepository(),
            oauthBrowser = FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    startKoin {
      modules(
          module {
            viewModel { authViewModel }
            single<IAuthSecureStorage> { InMemoryHostAuthSecureStorage() }
          }
      )
    }

    try {
      composeRule.setContent { MejenguerosTheme { AppNavHost() } }

      advanceUntilIdle()
      composeRule.waitForIdle()

      composeRule
          .onNodeWithTag("auth_session_restore_root", useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule.onNodeWithTag("login_root", useUnmergedTree = true).assertExists()
    } finally {
      scope.cancel()
      stopKoin()
    }
  }

  @Test
  fun appNavHostKeepsStartupGateVisibleWhileRestoredProfileRefreshIsPending() = runTest {
    stopKoin()

    val refreshGate = CompletableDeferred<Unit>()
    val profileRefreshStarted = CompletableDeferred<Unit>()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val authViewModel =
        AuthViewModel(
            authRepository =
                RestoredSessionPendingProfileRefreshAuthRepository(
                    refreshGate = refreshGate,
                    profileRefreshStarted = profileRefreshStarted,
                ),
            oauthBrowser = FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    startKoin {
      modules(
          module {
            viewModel { authViewModel }
            single<IAuthSecureStorage> { InMemoryHostAuthSecureStorage() }
          }
      )
    }

    try {
      composeRule.setContent { MejenguerosTheme { AppNavHost() } }

      advanceUntilIdle()
      composeRule.waitForIdle()

      kotlin.test.assertTrue(profileRefreshStarted.isCompleted)
      kotlin.test.assertTrue(authViewModel.uiState.value.isAuthenticated)
      kotlin.test.assertTrue(authViewModel.uiState.value.isResolvingAuthenticatedStartup)
      composeRule.onNodeWithTag("auth_session_restore_root", useUnmergedTree = true).assertExists()
      composeRule.onNodeWithTag("login_root", useUnmergedTree = true).assertDoesNotExist()

      refreshGate.complete(Unit)
    } finally {
      scope.cancel()
      stopKoin()
    }
  }

  @Test
  fun appNavHostKeepsRestorationScreenUntilStartupOwnerPreferenceHydrationCompletes() = runTest {
    stopKoin()

    val refreshGate = CompletableDeferred<Unit>()
    val profileRefreshStarted = CompletableDeferred<Unit>()
    val ownerPreferenceReadGate = CompletableDeferred<Unit>()
    val ownerPreferenceReadStarted = CompletableDeferred<Unit>()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val authViewModel =
        AuthViewModel(
            authRepository =
                RestoredSessionPendingProfileRefreshAuthRepository(
                    refreshGate = refreshGate,
                    profileRefreshStarted = profileRefreshStarted,
                ),
            oauthBrowser = FakeOAuthBrowser(),
            callbackUrls = MutableSharedFlow(),
            coroutineScope = scope,
        )

    startKoin {
      modules(
          module {
            viewModel { authViewModel }
            viewModel { CourtCatalogViewModel(repository = get(), coroutineScope = scope) }
            viewModel { MyComplexViewModel(repository = get(), coroutineScope = scope) }
            viewModel {
              NotificationsViewModel(notificationRepository = get(), coroutineScope = scope)
            }
            single<IAuthSecureStorage> {
              DelayedOwnerPreferenceHostAuthSecureStorage(
                  ownerPreference = OwnerViewPreference.OWNER,
                  ownerPreferenceReadGate = ownerPreferenceReadGate,
                  ownerPreferenceReadStarted = ownerPreferenceReadStarted,
              )
            }
            single<ICourtCatalogRepository> { FakeCourtCatalogRepository() }
            single<IComplexRepository> { FakeComplexRepository() }
            single<INotificationRepository> { EmptyNotificationRepository() }
          }
      )
    }

    try {
      composeRule.setContent { MejenguerosTheme { AppNavHost() } }

      advanceUntilIdle()
      composeRule.waitForIdle()

      kotlin.test.assertTrue(profileRefreshStarted.isCompleted)
      composeRule.onNodeWithTag("auth_session_restore_root", useUnmergedTree = true).assertExists()
      composeRule.onNodeWithText("Buscar").assertDoesNotExist()
      composeRule.onNodeWithTag("my_complex_root", useUnmergedTree = true).assertDoesNotExist()

      refreshGate.complete(Unit)
      advanceUntilIdle()

      composeRule.waitUntil(timeoutMillis = 5_000) { ownerPreferenceReadStarted.isCompleted }
      kotlin.test.assertTrue(ownerPreferenceReadStarted.isCompleted)
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("auth_session_restore_root", useUnmergedTree = true).assertExists()
      composeRule.onNodeWithText("Buscar").assertDoesNotExist()
      composeRule.onNodeWithTag("my_complex_root", useUnmergedTree = true).assertDoesNotExist()

      ownerPreferenceReadGate.complete(Unit)
      advanceUntilIdle()
      composeRule.waitForIdle()

      composeRule
          .onNodeWithTag("auth_session_restore_root", useUnmergedTree = true)
          .assertDoesNotExist()
      composeRule.onNodeWithTag("my_complex_root", useUnmergedTree = true).assertExists()
      composeRule.onNodeWithText("Buscar").assertDoesNotExist()
    } finally {
      scope.cancel()
      stopKoin()
    }
  }
}

private class PendingRestoreAuthRepository(
    private val restoreGate: CompletableDeferred<Unit>,
) : IAuthRepository {
  override suspend fun getSession(): AuthSession? {
    restoreGate.await()
    return null
  }

  override fun getUserProfile(): UserProfile? = null

  override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest =
      error("Unused in this test")

  override suspend fun registerWithEmail(fullName: String, email: String, password: String) =
      error("Unused in this test")

  override suspend fun confirmRegistration(email: String, code: String) =
      error("Unused in this test")

  override suspend fun resendRegistrationCode(email: String) = error("Unused in this test")

  override suspend fun signInWithEmail(email: String, password: String): AuthSession =
      error("Unused in this test")

  override suspend fun requestPasswordReset(email: String) = error("Unused in this test")

  override suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) =
      error("Unused in this test")

  override suspend fun handleCallback(callbackUrl: String): AuthSession =
      error("Unused in this test")

  override suspend fun signOut(): AuthSignOutRequest = error("Unused in this test")

  override suspend fun refreshUserProfile() = error("Unused in this test")
}

private class CancelledRestoreAuthRepository : IAuthRepository {
  override suspend fun getSession(): AuthSession? = throw CancellationException("cancelled")

  override fun getUserProfile(): UserProfile? = null

  override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest =
      error("Unused in this test")

  override suspend fun registerWithEmail(fullName: String, email: String, password: String) =
      error("Unused in this test")

  override suspend fun confirmRegistration(email: String, code: String) =
      error("Unused in this test")

  override suspend fun resendRegistrationCode(email: String) = error("Unused in this test")

  override suspend fun signInWithEmail(email: String, password: String): AuthSession =
      error("Unused in this test")

  override suspend fun requestPasswordReset(email: String) = error("Unused in this test")

  override suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) =
      error("Unused in this test")

  override suspend fun handleCallback(callbackUrl: String): AuthSession =
      error("Unused in this test")

  override suspend fun signOut(): AuthSignOutRequest = error("Unused in this test")

  override suspend fun refreshUserProfile() = error("Unused in this test")
}

private class RestoredSessionPendingProfileRefreshAuthRepository(
    private val refreshGate: CompletableDeferred<Unit>,
    private val profileRefreshStarted: CompletableDeferred<Unit>,
) : IAuthRepository {
  private var currentProfile: UserProfile? = null

  override suspend fun getSession(): AuthSession? =
      AuthSession(
          sub = "owner-sub",
          email = "owner@example.com",
          displayName = "Owner",
          provider = "Google",
          idToken = "id-token",
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresAtEpochSeconds = 4102444800,
      )

  override fun getUserProfile(): UserProfile? = currentProfile

  override suspend fun createSignInRequest(provider: AuthProvider): AuthSignInRequest =
      error("Unused in this test")

  override suspend fun registerWithEmail(fullName: String, email: String, password: String) =
      error("Unused in this test")

  override suspend fun confirmRegistration(email: String, code: String) =
      error("Unused in this test")

  override suspend fun resendRegistrationCode(email: String) = error("Unused in this test")

  override suspend fun signInWithEmail(email: String, password: String): AuthSession =
      error("Unused in this test")

  override suspend fun requestPasswordReset(email: String) = error("Unused in this test")

  override suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) =
      error("Unused in this test")

  override suspend fun handleCallback(callbackUrl: String): AuthSession =
      error("Unused in this test")

  override suspend fun signOut(): AuthSignOutRequest = error("Unused in this test")

  override suspend fun refreshUserProfile() {
    profileRefreshStarted.complete(Unit)
    refreshGate.await()
    currentProfile = UserProfile(id = "owner-profile", roles = listOf(UserRoleKind.OWNER))
  }
}

private class InMemoryHostAuthSecureStorage : IAuthSecureStorage {
  private var session: AuthSession? = null
  private var oauthState: PendingOAuthState? = null
  private val ownerViewPreferences = mutableMapOf<String, OwnerViewPreference>()

  override suspend fun getSession(): AuthSession? = session

  override suspend fun saveSession(session: AuthSession) {
    this.session = session
  }

  override suspend fun clearSession() {
    session = null
  }

  override suspend fun getOAuthState(): PendingOAuthState? = oauthState

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    oauthState = state
  }

  override suspend fun clearOAuthState() {
    oauthState = null
  }

  override suspend fun getOwnerViewPreference(userId: String): OwnerViewPreference? =
      ownerViewPreferences[userId]

  override suspend fun saveOwnerViewPreference(userId: String, preference: OwnerViewPreference) {
    ownerViewPreferences[userId] = preference
  }

  override suspend fun clearOwnerViewPreference(userId: String) {
    ownerViewPreferences.remove(userId)
  }
}

private class DelayedOwnerPreferenceHostAuthSecureStorage(
    private val ownerPreference: OwnerViewPreference,
    private val ownerPreferenceReadGate: CompletableDeferred<Unit>,
    private val ownerPreferenceReadStarted: CompletableDeferred<Unit>,
) : IAuthSecureStorage {
  private var session: AuthSession? = null
  private var oauthState: PendingOAuthState? = null
  private val ownerViewPreferences = mutableMapOf<String, OwnerViewPreference>()

  override suspend fun getSession(): AuthSession? = session

  override suspend fun saveSession(session: AuthSession) {
    this.session = session
  }

  override suspend fun clearSession() {
    session = null
  }

  override suspend fun getOAuthState(): PendingOAuthState? = oauthState

  override suspend fun saveOAuthState(state: PendingOAuthState) {
    oauthState = state
  }

  override suspend fun clearOAuthState() {
    oauthState = null
  }

  override suspend fun getOwnerViewPreference(userId: String): OwnerViewPreference {
    ownerPreferenceReadStarted.complete(Unit)
    ownerPreferenceReadGate.await()
    return ownerViewPreferences[userId] ?: ownerPreference
  }

  override suspend fun saveOwnerViewPreference(userId: String, preference: OwnerViewPreference) {
    ownerViewPreferences[userId] = preference
  }

  override suspend fun clearOwnerViewPreference(userId: String) {
    ownerViewPreferences.remove(userId)
  }
}

private class FakeCourtCatalogRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      minRating: Int?,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage = CourtCatalogPage.empty(pageSize)
}

private class FakeComplexRepository : IComplexRepository {
  override suspend fun getProvinces(): List<Province> = emptyList()

  override suspend fun getCantons(provinceId: String): List<Canton> = emptyList()

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> = emptyList()

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
      error("Unused in this test")

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt =
      error("Unused in this test")

  override suspend fun getMyComplexHub(): MyComplexHub = MyComplexHub(complexes = emptyList())
}

private class EmptyNotificationRepository : INotificationRepository {
  override suspend fun getNotifications(): List<UserNotification> = emptyList()

  override suspend fun markRead(notificationId: String): UserNotification =
      error("Unused in this test")

  override fun observeRealtimeNotifications() = emptyFlow<UserNotification>()
}

private class FakeOAuthBrowser : IOAuthBrowser {
  override suspend fun open(url: String) = Unit
}
