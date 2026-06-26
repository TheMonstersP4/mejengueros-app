package io.github.themonstersp4.mejengueros.presentation.catalog

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

data class CourtCatalogUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedProvince: String? = null,
    val selectedCanton: String? = null,
    val availableProvinces: List<String> = emptyList(),
    val availableCantons: List<String> = emptyList(),
    val visibleCourts: List<CourtCatalogItem> = emptyList(),
    val allCourts: List<CourtCatalogItem> = emptyList(),
    val loadErrorMessage: String? = null,
    val isDemoMode: Boolean = true,
)
