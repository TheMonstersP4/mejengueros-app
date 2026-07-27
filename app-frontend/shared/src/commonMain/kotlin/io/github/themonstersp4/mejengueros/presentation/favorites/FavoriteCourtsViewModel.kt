package io.github.themonstersp4.mejengueros.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtFavoriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoriteCourtsViewModel(
    private val userId: String,
    private val favoriteRepository: ICourtFavoriteRepository,
    private val catalogRepository: ICourtCatalogRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(FavoriteCourtsUiState())
  val uiState: StateFlow<FavoriteCourtsUiState> = _uiState.asStateFlow()
  private var currentIds: List<String> = emptyList()
  private var idsVersion = 0L
  private var observationJob: Job? = null
  private var observerFailed = false

  init {
    startObserving()
  }

  fun retry() {
    if (observerFailed) {
      startObserving()
      return
    }
    val retryIds = currentIds
    val retryVersion = idsVersion
    scope.launch { load(retryIds, retryVersion) }
  }

  private fun startObserving() {
    observationJob?.cancel()
    observerFailed = false
    observationJob =
        scope.launch {
          try {
            favoriteRepository.observeFavoriteCourtIds(userId).collectLatest { ids ->
              currentIds = ids.distinct()
              idsVersion += 1
              load(currentIds, idsVersion)
            }
          } catch (error: Throwable) {
            if (error is CancellationException) throw error
            observerFailed = true
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No pudimos cargar tus canchas favoritas. Intentá nuevamente.",
                )
          }
        }
  }

  fun remove(courtId: String) {
    if (courtId in _uiState.value.removingCourtIds) return
    _uiState.value =
        _uiState.value.copy(
            removingCourtIds = _uiState.value.removingCourtIds + courtId,
            removalErrorMessage = null,
        )
    scope.launch {
      try {
        favoriteRepository.setFavorite(userId, courtId, false)
        _uiState.value =
            _uiState.value.copy(removingCourtIds = _uiState.value.removingCourtIds - courtId)
      } catch (error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value =
            _uiState.value.copy(
                removingCourtIds = _uiState.value.removingCourtIds - courtId,
                removalErrorMessage = "No pudimos quitar la cancha. Intentá nuevamente.",
            )
      }
    }
  }

  private suspend fun load(ids: List<String>, expectedIdsVersion: Long) {
    if (ids.isEmpty()) {
      if (expectedIdsVersion == idsVersion) {
        _uiState.value =
            _uiState.value.copy(isLoading = false, courts = emptyList(), errorMessage = null)
      }
      return
    }
    if (expectedIdsVersion == idsVersion) {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }
    try {
      val courts = catalogRepository.getFavoriteCourts(ids)
      if (expectedIdsVersion == idsVersion) {
        _uiState.value = _uiState.value.copy(isLoading = false, courts = courts)
      }
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      if (expectedIdsVersion == idsVersion) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                errorMessage = "No pudimos cargar tus canchas favoritas. Intentá nuevamente.",
            )
      }
    }
  }
}
