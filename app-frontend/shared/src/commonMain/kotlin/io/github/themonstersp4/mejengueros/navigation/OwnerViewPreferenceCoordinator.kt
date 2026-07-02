package io.github.themonstersp4.mejengueros.navigation

import io.github.themonstersp4.mejengueros.data.auth.AuthSecureStorageWriteException
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.OwnerViewPreference
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class OwnerViewPreferenceCoordinator(
    private val navigationState: AuthenticatedNavigationState,
    private val secureStorage: IAuthSecureStorage,
    private val coroutineScope: CoroutineScope,
) {
  suspend fun hydrate(authState: AuthUiState) {
    if (!authState.isAuthenticated) {
      navigationState.clearOwnerViewPreferenceHydration()
      return
    }

    if (!authState.isOwner) {
      navigationState.applyOwnerViewPreference(
          userId = authState.userId,
          isOwner = false,
          preference = null,
      )
      return
    }

    val userId = authState.userId?.takeIf { it.isNotBlank() } ?: return
    navigationState.applyOwnerViewPreference(
        userId = userId,
        isOwner = true,
        preference = secureStorage.getOwnerViewPreference(userId),
    )
  }

  fun switchToPlayerView(authState: AuthUiState) {
    navigationState.switchToPlayerView()
    persist(authState, OwnerViewPreference.PLAYER)
  }

  fun switchToOwnerView(authState: AuthUiState) {
    navigationState.switchToOwnerView()
    persist(authState, OwnerViewPreference.OWNER)
  }

  private fun persist(authState: AuthUiState, preference: OwnerViewPreference) {
    val userId = authState.userId?.takeIf { it.isNotBlank() }
    if (!authState.isOwner || userId == null) {
      return
    }

    coroutineScope.launch {
      try {
        secureStorage.saveOwnerViewPreference(userId, preference)
      } catch (_: AuthSecureStorageWriteException) {}
    }
  }
}
