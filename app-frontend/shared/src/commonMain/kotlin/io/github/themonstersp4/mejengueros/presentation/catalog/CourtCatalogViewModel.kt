package io.github.themonstersp4.mejengueros.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
  val uiState: StateFlow<CourtCatalogUiState> = _uiState.asStateFlow()

  init {
    loadCatalog()
  }

  fun retryLoad() {
    loadCatalog()
  }

  fun updateSearchQuery(value: String) {
    _uiState.value = _uiState.value.copy(searchQuery = value)
    loadCatalog()
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
    loadCatalog()
  }

  fun selectCanton(cantonId: String?) {
    val currentState = _uiState.value
    if (currentState.selectedCantonId == cantonId) {
      return
    }

    _uiState.value = currentState.copy(selectedCantonId = cantonId)
    loadCatalog()
  }

  private fun loadCatalog() {
    loadJob?.cancel()
    loadJob =
        scope.launch {
          val currentFilters = _uiState.value
          _uiState.value = currentFilters.copy(isLoading = true, loadErrorMessage = null)
          try {
            val allCourts =
                repository.getCatalogCourts(
                    searchQuery = currentFilters.searchQuery,
                    provinceId = currentFilters.selectedProvinceId,
                    cantonId = currentFilters.selectedCantonId,
                )
            _uiState.value =
                buildCourtCatalogState(
                    allCourts = allCourts,
                    searchQuery = currentFilters.searchQuery,
                    selectedProvinceId = currentFilters.selectedProvinceId,
                    selectedCantonId = currentFilters.selectedCantonId,
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
}

private fun buildCourtCatalogState(
    allCourts: List<CourtCatalogItem>,
    searchQuery: String = "",
    selectedProvinceId: String? = null,
    selectedCantonId: String? = null,
): CourtCatalogUiState {
  val availableProvinces = buildAvailableProvinces(allCourts)
  val normalizedProvinceId =
      selectedProvinceId?.takeIf { provinceId -> availableProvinces.any { it.id == provinceId } }
  val availableCantons = buildAvailableCantons(allCourts, normalizedProvinceId)
  val normalizedCantonId =
      selectedCantonId?.takeIf { cantonId -> availableCantons.any { it.id == cantonId } }

  return CourtCatalogUiState(
      isLoading = false,
      searchQuery = searchQuery,
      selectedProvinceId = normalizedProvinceId,
      selectedCantonId = normalizedCantonId,
      availableProvinces = availableProvinces,
      availableCantons = availableCantons,
      visibleCourts = allCourts,
      allCourts = allCourts,
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
