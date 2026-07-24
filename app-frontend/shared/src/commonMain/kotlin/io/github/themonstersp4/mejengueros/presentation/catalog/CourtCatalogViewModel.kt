package io.github.themonstersp4.mejengueros.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourtCatalogViewModel(
    private val repository: ICourtCatalogRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(CourtCatalogUiState())
  private var loadJob: Job? = null
  private var searchJob: Job? = null
  private var nextPageJob: Job? = null
  private var servicesJob: Job? = null
  val uiState: StateFlow<CourtCatalogUiState> = _uiState.asStateFlow()

  init {
    loadServiceCatalog()
    loadCatalog()
  }

  fun retryLoad() {
    searchJob?.cancel()
    loadCatalog()
  }

  /**
   * Loads the next catalog page and appends it to the already visible courts. No-ops when there is
   * no next page or another load is already running, so repeated scroll triggers cannot fire
   * overlapping requests.
   */
  fun loadNextPage() {
    val current = _uiState.value
    if (!current.canLoadNextPage) {
      return
    }

    val targetPage = current.currentPage + 1
    nextPageJob?.cancel()
    nextPageJob =
        scope.launch {
          _uiState.value =
              _uiState.value.copy(isLoadingNextPage = true, nextPageErrorMessage = null)
          try {
            val page =
                repository.getCatalogCourts(
                    searchQuery = current.searchQuery,
                    provinceId = current.selectedProvinceId,
                    cantonId = current.selectedCantonId,
                    serviceIds = current.selectedServiceIds.toList(),
                    minRating = current.selectedMinRating,
                    page = targetPage,
                    pageSize = PageSize,
                )
            val accumulated = (current.allCourts + page.items).distinctBy(CourtCatalogItem::id)
            _uiState.value =
                buildCourtCatalogState(
                    accumulatedCourts = accumulated,
                    page = page,
                    searchQuery = current.searchQuery,
                    selectedProvinceId = current.selectedProvinceId,
                    selectedCantonId = current.selectedCantonId,
                    selectedServiceIds = current.selectedServiceIds,
                    selectedMinRating = current.selectedMinRating,
                    availableServices = _uiState.value.availableServices,
                    fallbackProvinces = current.availableProvinces,
                    fallbackCantons = current.availableCantons,
                )
          } catch (error: Throwable) {
            if (error is CancellationException) {
              throw error
            }

            _uiState.value =
                _uiState.value.copy(
                    isLoadingNextPage = false,
                    nextPageErrorMessage = "No pudimos cargar más canchas. Intentá nuevamente.",
                )
          }
        }
  }

  /** Retries the failed next-page load without discarding the loaded courts. */
  fun retryNextPage() {
    if (_uiState.value.isLoadingNextPage) {
      return
    }

    _uiState.value = _uiState.value.copy(nextPageErrorMessage = null)
    loadNextPage()
  }

  fun updateSearchQuery(value: String) {
    if (value == _uiState.value.searchQuery) {
      return
    }

    _uiState.value = _uiState.value.copy(searchQuery = value)
    searchJob?.cancel()
    searchJob =
        scope.launch {
          delay(SearchDebounceMillis)
          loadCatalog()
        }
  }

  fun selectProvince(provinceId: String?) {
    val currentState = _uiState.value
    val nextState =
        currentState.copy(
            selectedProvinceId = provinceId,
            selectedCantonId = null,
        )
    if (nextState == currentState) {
      return
    }

    _uiState.value = nextState
    searchJob?.cancel()
    loadCatalog()
  }

  fun selectCanton(cantonId: String?) {
    val currentState = _uiState.value
    if (currentState.selectedCantonId == cantonId) {
      return
    }

    _uiState.value = currentState.copy(selectedCantonId = cantonId)
    searchJob?.cancel()
    loadCatalog()
  }

  /** Adds or removes a service from the active filter, then reloads the catalog. */
  fun toggleService(serviceId: String) {
    val currentState = _uiState.value
    val nextSelection =
        if (serviceId in currentState.selectedServiceIds) {
          currentState.selectedServiceIds - serviceId
        } else {
          currentState.selectedServiceIds + serviceId
        }

    _uiState.value = currentState.copy(selectedServiceIds = nextSelection)
    searchJob?.cancel()
    loadCatalog()
  }

  fun clearServices() {
    val currentState = _uiState.value
    if (currentState.selectedServiceIds.isEmpty()) {
      return
    }

    _uiState.value = currentState.copy(selectedServiceIds = emptySet())
    searchJob?.cancel()
    loadCatalog()
  }

  /** Sets the minimum average rating filter (null clears it), then reloads the catalog. */
  fun selectMinRating(minRating: Int?) {
    val currentState = _uiState.value
    if (currentState.selectedMinRating == minRating) {
      return
    }

    _uiState.value = currentState.copy(selectedMinRating = minRating)
    searchJob?.cancel()
    loadCatalog()
  }

  /** Clears every filter at once (search text is kept) with a single reload. */
  fun clearAllFilters() {
    val currentState = _uiState.value
    val alreadyEmpty =
        currentState.selectedProvinceId == null &&
            currentState.selectedCantonId == null &&
            currentState.selectedServiceIds.isEmpty() &&
            currentState.selectedMinRating == null
    if (alreadyEmpty) {
      return
    }

    _uiState.value =
        currentState.copy(
            selectedProvinceId = null,
            selectedCantonId = null,
            selectedServiceIds = emptySet(),
            selectedMinRating = null,
        )
    searchJob?.cancel()
    loadCatalog()
  }

  private fun loadCatalog() {
    loadJob?.cancel()
    // A fresh first-page load (search/filter change or retry) supersedes any
    // in-flight next-page request so pages from different filters never mix.
    nextPageJob?.cancel()
    loadJob =
        scope.launch {
          val currentFilters = _uiState.value
          _uiState.value =
              currentFilters.copy(
                  isLoading = true,
                  loadErrorMessage = null,
                  isLoadingNextPage = false,
                  nextPageErrorMessage = null,
              )
          try {
            val firstPage =
                repository.getCatalogCourts(
                    searchQuery = currentFilters.searchQuery,
                    provinceId = currentFilters.selectedProvinceId,
                    cantonId = currentFilters.selectedCantonId,
                    serviceIds = currentFilters.selectedServiceIds.toList(),
                    minRating = currentFilters.selectedMinRating,
                    page = 1,
                    pageSize = PageSize,
                )
            _uiState.value =
                buildCourtCatalogState(
                    accumulatedCourts = firstPage.items,
                    page = firstPage,
                    searchQuery = currentFilters.searchQuery,
                    selectedProvinceId = currentFilters.selectedProvinceId,
                    selectedCantonId = currentFilters.selectedCantonId,
                    selectedServiceIds = currentFilters.selectedServiceIds,
                    selectedMinRating = currentFilters.selectedMinRating,
                    availableServices = _uiState.value.availableServices,
                    fallbackProvinces = currentFilters.availableProvinces,
                    fallbackCantons = currentFilters.availableCantons,
                )
          } catch (error: Throwable) {
            if (error is CancellationException) {
              throw error
            }

            _uiState.value =
                currentFilters.copy(
                    isLoading = false,
                    loadErrorMessage =
                        "No pudimos cargar el catálogo en este momento. Intentá nuevamente.",
                )
          }
        }
  }

  /**
   * Loads the service filter options once. A failure leaves the options empty so the rest of the
   * catalog keeps working; the service chip simply offers nothing to pick.
   */
  private fun loadServiceCatalog() {
    servicesJob?.cancel()
    servicesJob =
        scope.launch {
          val services =
              try {
                repository.getServiceCatalog()
              } catch (error: Throwable) {
                if (error is CancellationException) {
                  throw error
                }
                emptyList()
              }

          if (services.isEmpty()) {
            return@launch
          }

          val options =
              services
                  .map { CatalogFilterOption(id = it.id, label = it.name) }
                  .distinctBy(CatalogFilterOption::id)
                  .sortedBy(CatalogFilterOption::label)
          _uiState.value = _uiState.value.copy(availableServices = options)
        }
  }

  companion object {
    private const val SearchDebounceMillis = 300L
    private const val PageSize = CourtCatalogPage.DEFAULT_PAGE_SIZE
  }
}

private fun buildCourtCatalogState(
    accumulatedCourts: List<CourtCatalogItem>,
    page: CourtCatalogPage,
    searchQuery: String = "",
    selectedProvinceId: String? = null,
    selectedCantonId: String? = null,
    selectedServiceIds: Set<String> = emptySet(),
    selectedMinRating: Int? = null,
    availableServices: List<CatalogFilterOption> = emptyList(),
    fallbackProvinces: List<CatalogFilterOption> = emptyList(),
    fallbackCantons: List<CatalogFilterOption> = emptyList(),
): CourtCatalogUiState {
  // Filter options are derived from every court loaded so far, so the province
  // and canton dropdowns keep growing as more pages stream in.
  val responseProvinces = buildAvailableProvinces(accumulatedCourts)
  val resolvedSelectedProvince =
      selectedProvinceId?.let { provinceId ->
        responseProvinces.firstOrNull { it.id == provinceId }
            ?: fallbackProvinces.firstOrNull { it.id == provinceId }
      }
  val availableProvinces = responseProvinces.preservingSelection(resolvedSelectedProvince)
  val normalizedProvinceId = resolvedSelectedProvince?.id
  val responseCantons = buildAvailableCantons(accumulatedCourts, normalizedProvinceId)
  val resolvedSelectedCanton =
      selectedCantonId?.let { cantonId ->
        responseCantons.firstOrNull { it.id == cantonId }
            ?: fallbackCantons.firstOrNull { it.id == cantonId }
      }
  val availableCantons = responseCantons.preservingSelection(resolvedSelectedCanton)
  val normalizedCantonId = resolvedSelectedCanton?.id

  return CourtCatalogUiState(
      isLoading = false,
      searchQuery = searchQuery,
      selectedProvinceId = normalizedProvinceId,
      selectedCantonId = normalizedCantonId,
      selectedServiceIds = selectedServiceIds,
      selectedMinRating = selectedMinRating,
      availableProvinces = availableProvinces,
      availableCantons = availableCantons,
      availableServices = availableServices,
      visibleCourts = accumulatedCourts,
      allCourts = accumulatedCourts,
      loadErrorMessage = null,
      currentPage = page.page,
      hasNextPage = page.hasNextPage,
      isLoadingNextPage = false,
      nextPageErrorMessage = null,
      totalCourts = page.totalItems,
  )
}

private fun buildAvailableProvinces(allCourts: List<CourtCatalogItem>): List<CatalogFilterOption> =
    allCourts
        .map { CatalogFilterOption(id = it.provinceId, label = it.provinceName) }
        .distinctBy(CatalogFilterOption::id)
        .sortedBy(CatalogFilterOption::label)

private fun buildAvailableCantons(
    allCourts: List<CourtCatalogItem>,
    selectedProvinceId: String?,
): List<CatalogFilterOption> =
    allCourts
        .filter { selectedProvinceId == null || it.provinceId == selectedProvinceId }
        .map { CatalogFilterOption(id = it.cantonId, label = it.cantonName) }
        .distinctBy(CatalogFilterOption::id)
        .sortedBy(CatalogFilterOption::label)

private fun List<CatalogFilterOption>.preservingSelection(
    selectedOption: CatalogFilterOption?
): List<CatalogFilterOption> {
  if (selectedOption == null || any { it.id == selectedOption.id }) {
    return this
  }

  return (this + selectedOption).sortedBy(CatalogFilterOption::label)
}
