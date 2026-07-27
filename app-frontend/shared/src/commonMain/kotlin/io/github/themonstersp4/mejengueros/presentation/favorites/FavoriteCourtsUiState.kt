package io.github.themonstersp4.mejengueros.presentation.favorites

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

data class FavoriteCourtsUiState(
    val isLoading: Boolean = true,
    val courts: List<CourtCatalogItem> = emptyList(),
    val errorMessage: String? = null,
    val removingCourtIds: Set<String> = emptySet(),
    val removalErrorMessage: String? = null,
)
