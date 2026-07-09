package io.github.themonstersp4.mejengueros.presentation.catalog

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

data class CourtCatalogUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedProvinceId: String? = null,
    val selectedCantonId: String? = null,
    val availableProvinces: List<CatalogFilterOption> = emptyList(),
    val availableCantons: List<CatalogFilterOption> = emptyList(),
    val visibleCourts: List<CourtCatalogItem> = emptyList(),
    val allCourts: List<CourtCatalogItem> = emptyList(),
    val loadErrorMessage: String? = null,
    val currentPage: Int = 0,
    val hasNextPage: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val nextPageErrorMessage: String? = null,
    val totalCourts: Int = 0,
) {
  val selectedProvince: CatalogFilterOption?
    get() = availableProvinces.firstOrNull { it.id == selectedProvinceId }

  val selectedCanton: CatalogFilterOption?
    get() = availableCantons.firstOrNull { it.id == selectedCantonId }

  /**
   * True when the catalog can request the next page right now: there is another page, the first
   * page already loaded, and no other load is in flight.
   */
  val canLoadNextPage: Boolean
    get() =
        hasNextPage &&
            !isLoading &&
            !isLoadingNextPage &&
            loadErrorMessage == null &&
            nextPageErrorMessage == null
}
