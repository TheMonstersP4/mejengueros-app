package io.github.themonstersp4.mejengueros.presentation.ownerreviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.OwnerReceivedCourtFilter
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerReceivedReviewsViewModel(
    private val reviewRepository: IReviewRepository,
    private val complexRepository: IComplexRepository,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    private val coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(OwnerReceivedReviewsUiState(isLoading = true))
  private var loadPageJob: Job? = null
  private var loadMoreJob: Job? = null
  private var courtsJob: Job? = null
  private var currentLoadGeneration: Long = 0

  val uiState: StateFlow<OwnerReceivedReviewsUiState> = _uiState.asStateFlow()

  init {
    loadCourtsAndFirstPage()
  }

  fun selectCourt(courtId: String?) {
    val normalized = courtId?.takeIf { it.isNotBlank() }
    if (normalized == _uiState.value.selectedCourtId) return

    _uiState.value =
        _uiState.value.copy(
            selectedCourtId = normalized,
            items = emptyList(),
            page = 1,
            totalPages = 0,
            hasNextPage = false,
            isLoading = true,
            isLoadingMore = false,
            isRefreshing = false,
            loadErrorMessage = null,
            loadMoreErrorMessage = null,
        )
    loadFirstPage()
  }

  fun refresh() {
    val state = _uiState.value
    if (state.isLoading || state.isLoadingMore || state.isRefreshing) return

    _uiState.value =
        state.copy(
            isLoading = true,
            isLoadingMore = false,
            isRefreshing = true,
            loadErrorMessage = null,
            loadMoreErrorMessage = null,
        )
    loadFirstPage()
  }

  fun loadNextPage() {
    // Auto-scroll entry point: respect the canLoadMore guard so a visible
    // load-more error message does not spin the network in a tight loop.
    if (!_uiState.value.canLoadMore) return
    loadMoreInternal()
  }

  fun retryLoadMore() {
    // Explicit user retry from the visible "Reintentar" button. Same pre-conditions
    // as loadNextPage EXCEPT the loadMoreErrorMessage guard, which is the whole
    // reason the user is pressing retry.
    val state = _uiState.value
    if (!state.hasNextPage) return
    if (state.isLoading || state.isLoadingMore || state.isRefreshing) return
    loadMoreInternal()
  }

  private fun loadMoreInternal() {
    val state = _uiState.value
    val nextPage = state.page + 1
    val pageSize = state.effectivePageSize
    val generation = ++currentLoadGeneration
    loadMoreJob?.cancel()
    loadMoreJob =
        scope.launch {
          _uiState.value =
              _uiState.value.copy(
                  isLoadingMore = true,
                  loadMoreErrorMessage = null,
              )

          runCatching {
                reviewRepository.getOwnerReceivedReviews(
                    courtId = _uiState.value.selectedCourtId,
                    page = nextPage,
                    pageSize = pageSize,
                )
              }
              .onSuccess { page ->
                if (!isActiveGeneration(generation)) return@onSuccess
                val current = _uiState.value
                _uiState.value =
                    current.copy(
                        items = current.items + page.items,
                        page = page.page,
                        pageSize = page.pageSize,
                        totalPages = page.totalPages,
                        hasNextPage = page.hasNextPage,
                        summary = page.summary,
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        loadErrorMessage = null,
                        loadMoreErrorMessage = null,
                    )
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (!isActiveGeneration(generation)) return@onFailure

                reportLoadMoreFailure(error)

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        loadMoreErrorMessage = error.toLoadMoreUserMessage(),
                    )
              }
        }
  }

  fun acknowledgeError() {
    val state = _uiState.value
    if (state.loadErrorMessage == null && state.loadMoreErrorMessage == null) return
    _uiState.value = state.copy(loadErrorMessage = null, loadMoreErrorMessage = null)
  }

  private fun loadCourtsAndFirstPage() {
    courtsJob?.cancel()
    courtsJob =
        scope.launch {
          runCatching { complexRepository.getMyComplexHub() }
              .onSuccess { hub ->
                val courts = hub.ownedCourts()
                val previous = _uiState.value
                val preservedSelection =
                    courts.firstOrNull { it.courtId == previous.selectedCourtId }
                val nextSelectionId = preservedSelection?.courtId
                _uiState.value =
                    previous.copy(
                        availableCourts = courts,
                        selectedCourtId = nextSelectionId,
                    )
                if (previous.items.isEmpty() && previous.loadErrorMessage == null) {
                  loadFirstPage()
                }
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                // Courts are an enhancement only; fall back to whatever the API returns.
                val previous = _uiState.value
                if (previous.items.isEmpty() && previous.loadErrorMessage == null) {
                  loadFirstPage()
                }
              }
        }
  }

  private fun loadFirstPage() {
    val state = _uiState.value
    val pageSize = state.effectivePageSize
    val generation = ++currentLoadGeneration
    loadPageJob?.cancel()
    loadPageJob =
        scope.launch {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = !_uiState.value.isRefreshing,
                  loadErrorMessage = null,
              )
          runCatching {
                reviewRepository.getOwnerReceivedReviews(
                    courtId = _uiState.value.selectedCourtId,
                    page = 1,
                    pageSize = pageSize,
                )
              }
              .onSuccess { page ->
                if (!isActiveGeneration(generation)) return@onSuccess
                applyPage(page = page, replaceItems = true)
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (!isActiveGeneration(generation)) return@onFailure

                reportLoadFailure(error)

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        items =
                            if (_uiState.value.isRefreshing) _uiState.value.items else emptyList(),
                        loadErrorMessage = error.toLoadUserMessage(),
                    )
              }
        }
  }

  private fun applyPage(page: ReceivedReviewPage, replaceItems: Boolean) {
    val current = _uiState.value
    _uiState.value =
        current.copy(
            items = if (replaceItems) page.items else current.items + page.items,
            summary = page.summary,
            page = page.page,
            pageSize = page.pageSize,
            totalPages = page.totalPages,
            hasNextPage = page.hasNextPage,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false,
            loadErrorMessage = null,
            loadMoreErrorMessage = null,
        )
  }

  private fun isActiveGeneration(generation: Long): Boolean = currentLoadGeneration == generation

  private fun reportLoadFailure(error: Throwable) {
    errorReporter.reportRecoverableFailure(
        name = "owner_received_reviews_load_failed",
        attributes = error.toReportAttributes(),
    )
  }

  private fun reportLoadMoreFailure(error: Throwable) {
    errorReporter.reportRecoverableFailure(
        name = "owner_received_reviews_load_more_failed",
        attributes = error.toReportAttributes(),
    )
  }
}

private fun io.github.themonstersp4.mejengueros.domain.model.MyComplexHub.ownedCourts():
    List<OwnerReceivedCourtFilter> =
    complexes
        .flatMap { complex -> complex.courts }
        .map { court -> OwnerReceivedCourtFilter(courtId = court.id, name = court.name) }
        .distinctBy { it.courtId }
        .sortedBy { it.name.lowercase() }

private fun Throwable.toLoadUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para ver las reseñas recibidas."
            else -> "No pudimos cargar las reseñas recibidas. Intentá de nuevo."
          }
      else -> "No pudimos cargar las reseñas recibidas. Intentá de nuevo."
    }

private fun Throwable.toLoadMoreUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para ver más reseñas."
            else -> "No pudimos cargar más reseñas. Intentá de nuevo."
          }
      else -> "No pudimos cargar más reseñas. Intentá de nuevo."
    }

private fun Throwable.toReportAttributes(): Map<String, String> =
    when (this) {
      is AppApiException ->
          mapOf(
              "error_source" to "app_api",
              "status_code" to statusCode.toString(),
          )
      else -> mapOf("error_source" to "unexpected")
    }
