package io.github.themonstersp4.mejengueros.presentation.auth

import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTest {

  @Test
  fun initRestoresExistingSession() {
    val repository = FakeAuthRepository(existingSession = AuthSession(username = "stored-user"))

    val viewModel = AuthViewModel(repository)

    assertEquals("stored-user", viewModel.uiState.value.username)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun signInWithBlankUsernameShowsErrorAndDoesNotAuthenticate() {
    val repository = FakeAuthRepository()
    val viewModel = AuthViewModel(repository)

    val result = viewModel.signIn()

    assertFalse(result)
    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertEquals("Enter a username to continue.", viewModel.uiState.value.errorMessage)
    assertNull(repository.savedUsername)
  }

  @Test
  fun signInWithUsernameSavesSessionAndAuthenticates() {
    val repository = FakeAuthRepository()
    val viewModel = AuthViewModel(repository)

    viewModel.updateUsername("  player-one  ")
    val result = viewModel.signIn()

    assertTrue(result)
    assertEquals("player-one", viewModel.uiState.value.username)
    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals("player-one", repository.receivedUsername)
    assertEquals("player-one", repository.savedUsername)
  }

  @Test
  fun signOutClearsRepositoryAndState() {
    val repository = FakeAuthRepository(existingSession = AuthSession(username = "stored-user"))
    val viewModel = AuthViewModel(repository)

    viewModel.signOut()

    assertEquals(AuthUiState(), viewModel.uiState.value)
    assertEquals(1, repository.signOutCount)
  }

  private class FakeAuthRepository(private val existingSession: AuthSession? = null) :
      IAuthRepository {
    var receivedUsername: String? = null
    var savedUsername: String? = null
    var signOutCount = 0

    override fun getSession(): AuthSession? = existingSession

    override fun signIn(username: String): AuthSession {
      receivedUsername = username
      val session = AuthSession(username = username)
      savedUsername = session.username
      return session
    }

    override fun signOut() {
      signOutCount++
    }
  }
}
