package io.github.themonstersp4.mejengueros.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
  val uiState: StateFlow<CourtCatalogUiState> = _uiState.asStateFlow()

  init {
    loadCatalog()
  }

  fun retryLoad() {
    loadCatalog()
  }

  fun updateSearchQuery(value: String) {
    _uiState.value =
        buildCourtCatalogState(
            allCourts = _uiState.value.allCourts,
            searchQuery = value,
            selectedProvince = _uiState.value.selectedProvince,
            selectedCanton = _uiState.value.selectedCanton,
        )
  }

  fun selectProvince(province: String?) {
    _uiState.value =
        buildCourtCatalogState(
            allCourts = _uiState.value.allCourts,
            searchQuery = _uiState.value.searchQuery,
            selectedProvince = province,
            selectedCanton = _uiState.value.selectedCanton,
        )
  }

  fun selectCanton(canton: String?) {
    _uiState.value =
        buildCourtCatalogState(
            allCourts = _uiState.value.allCourts,
            searchQuery = _uiState.value.searchQuery,
            selectedProvince = _uiState.value.selectedProvince,
            selectedCanton = canton,
        )
  }

  private fun loadCatalog() {
    scope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, loadErrorMessage = null)
      try {
        val allCourts = repository.getCatalogCourts()
        _uiState.value = buildCourtCatalogState(allCourts = allCourts)
      } catch (error: Throwable) {
        if (error is CancellationException) {
          throw error
        }

        _uiState.value =
            CourtCatalogUiState(
                isLoading = false,
                loadErrorMessage =
                    "No pudimos cargar esta vista previa del catálogo. Intentá nuevamente.",
            )
      }
    }
  }
}

private fun buildCourtCatalogState(
    allCourts: List<CourtCatalogItem>,
    searchQuery: String = "",
    selectedProvince: String? = null,
    selectedCanton: String? = null,
): CourtCatalogUiState {
  val availableCantons = buildAvailableCantons(allCourts, selectedProvince)
  val normalizedCanton = selectedCanton?.takeIf(availableCantons::contains)

  return CourtCatalogUiState(
      isLoading = false,
      searchQuery = searchQuery,
      selectedProvince = selectedProvince,
      selectedCanton = normalizedCanton,
      availableProvinces = buildAvailableProvinces(allCourts),
      availableCantons = availableCantons,
      visibleCourts = filterCourts(allCourts, searchQuery, selectedProvince, normalizedCanton),
      allCourts = allCourts,
  )
}

private fun filterCourts(
    allCourts: List<CourtCatalogItem>,
    searchQuery: String,
    selectedProvince: String?,
    selectedCanton: String?,
): List<CourtCatalogItem> {
  val normalizedQuery = searchQuery.trim().lowercase()

  return allCourts
      .asSequence()
      .filter { it.isPublished && it.isActive }
      .filter {
        normalizedQuery.isBlank() ||
            it.complexName.lowercase().contains(normalizedQuery) ||
            it.courtName.lowercase().contains(normalizedQuery)
      }
      .filter { selectedProvince == null || it.province == selectedProvince }
      .filter { selectedCanton == null || it.canton == selectedCanton }
      .toList()
}

private fun buildAvailableProvinces(allCourts: List<CourtCatalogItem>): List<String> =
    allCourts.filter { it.isPublished && it.isActive }.map { it.province }.distinct().sorted()

private fun buildAvailableCantons(
    allCourts: List<CourtCatalogItem>,
    selectedProvince: String?,
): List<String> =
    allCourts
        .filter { it.isPublished && it.isActive }
        .filter { selectedProvince == null || it.province == selectedProvince }
        .map { it.canton }
        .distinct()
        .sorted()
