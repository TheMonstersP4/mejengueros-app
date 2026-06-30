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
) {
  val selectedProvince: CatalogFilterOption?
    get() = availableProvinces.firstOrNull { it.id == selectedProvinceId }

  val selectedCanton: CatalogFilterOption?
    get() = availableCantons.firstOrNull { it.id == selectedCantonId }
}
