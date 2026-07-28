package io.github.themonstersp4.mejengueros.presentation.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class CourtCatalogUiStateTest {
  @Test
  fun activeFilterCountIsZeroWithNoFilters() {
    assertEquals(0, CourtCatalogUiState().activeFilterCount)
  }

  @Test
  fun activeFilterCountCountsEachGroupOnceIncludingMultiSelectServices() {
    val state =
        CourtCatalogUiState(
            selectedProvinceId = "province-1",
            selectedCantonId = "canton-1",
            selectedServiceIds = setOf("service-1", "service-2"),
            selectedMinRating = 4,
        )

    // Province + canton + services (one group despite two ids) + rating = 4.
    assertEquals(4, state.activeFilterCount)
  }

  @Test
  fun activeFilterCountReflectsPartialSelection() {
    val state =
        CourtCatalogUiState(
            selectedServiceIds = setOf("service-1"),
            selectedMinRating = 5,
        )

    assertEquals(2, state.activeFilterCount)
  }
}
